package com.exlibris.primo.thirdnode.thirdnodeimpl;

import com.exlibris.jaguar.xsd.search.*;
import com.exlibris.primo.interfaces.AbstractDeepSearch;
import com.exlibris.primo.soap.messages.*;
import com.exlibris.primo.utils.ThirdNodeConst;
import com.exlibris.primo.xsd.commonData.PrimoResult;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import sun.misc.BASE64Encoder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * KluwerDeepSearch
 * ThirdNode adaptor for Primo to search Kluwer
 *
 * (c) 2016 KULeuven/LIBIS, Mehmet Celik
 */
//-Djavax.net.debug=all
public class KluwerDeepSearch extends AbstractDeepSearch {

    HashMap<String,String> records = new HashMap<String,String>();


    private boolean mock = true;
    private static String logToFile = "";
    private String mockFile = "data/kluwer.json";
    private String subscription = "";
    private String clientId = "";
    private String clientSecret = "";

    /**
     * Initialize
     * @param map
     */
    @Override
    public void init(Map map) {
        this.mock            = Boolean.valueOf((String) map.get("mock"));
        this.logToFile       = (String) map.get("log_to_file");
        this.mockFile        = (String) map.get("mock_file");

        //this.authorization   = (String) map.get("authorization");
        this.subscription    = (String) map.get("subscription");
        this.clientId        = (String) map.get("clientId");
        this.clientSecret    = (String) map.get("clientSecret");

        libisLogger("init:" + JSONValue.toJSONString(map));
    }

    /**
     * Perform search
     * @param vid
     * @param query
     * @param from
     * @param to
     * @param authorization
     * @param sort
     * @return
     * @throws Exception
     */
    @Override
    public PrimoResult search(String vid, String query, int from, int to, Map authorization, String sort) throws Exception {

        //Object signedIn = authorization.get(ThirdNodeConst.IS_LOGGED_IN);
        //Object onCampus = authorization.get(ThirdNodeConst.ON_OFF_CAMPUS);

        //boolean signedInUser = ((signedIn != null) && (((Boolean)signedIn).booleanValue())) || ((onCampus != null) && (((Boolean)onCampus).booleanValue()));
        //String institution = (String)authorization.get(ThirdNodeConst.INSTITUTION);
        //String sessionId = (String)authorization.get(ThirdNodeConst.SESSION_ID);

        libisLogger("search: VID=" + vid + "; sort=" + sort + "; query=" + query +
                "; from=" + from + "; to=" + to + "; authorization="+ JSONValue.toJSONString(authorization));

        //Patching a wrong result;
        //searchResult.getSEGMENTS().getJAGROOTArray(0).getRESULT().getDOCSET().setFIRSTHIT(String.valueOf(from));

        return json2pnx(kluwerSearch(query, from, to));
    }

    /**
     * No idea what this function does. It looks like it is never called
     * @param query
     * @param sort
     * @param facets
     * @param authorization
     * @param searchToken
     * @return
     */
    @Override
    public FACETLISTDocument.FACETLIST countFacets(String query, String sort, Map<String, String[]> facets, Map authorization, String searchToken){
        libisLogger("countFacets: query=" + query + "; sort=" + sort + "; searchToken=" + searchToken + "; facets=" + JSONValue.toJSONString(facets) + "; authorizaton=" + JSONValue.toJSONString(authorization));

        FACETLISTDocument.FACETLIST facetList = FACETLISTDocument.FACETLIST.Factory.newInstance();
        if ((facets != null) && (facets.size() > 0)) {
            FACETDocument.FACET facet = facetList.addNewFACET();
            facet.setNAME("genre");
            facet.setCOUNT(facets.size());

            for(Object entry: facets.entrySet()) {
                String facetKey   = String.valueOf(((JSONObject) entry).get("name"));
                String facetValue = String.valueOf(((JSONObject) entry).get("total"));

                if (facetKey.length() > 0 && facetValue.length() > 0) {
                    System.out.println(facetKey + " = " + facetValue);

                    FACETVALUESDocument.FACETVALUES value = facet.addNewFACETVALUES();
                    value.setKEY(facetKey);
                    value.setVALUE(facetValue);
                }
            }
        }

        return facetList;
    }

    /**
     *
     * @param authorization
     * @return
     */
    private boolean isSearchAllowed(Map<String, String> authorization) {
        Object pdsHandle = authorization.get(ThirdNodeConst.PDS_HANDLE);
        boolean signedInUser = (pdsHandle != null && !"".equals(pdsHandle));

        Boolean isOnCampus;
        isOnCampus = Boolean.getBoolean(authorization.get(ThirdNodeConst.ON_OFF_CAMPUS));

        return signedInUser || isOnCampus;
    }

    /**
     * Sometimes you do not want to scroll through the Primo logs
     * @param message
     */
    private static synchronized void libisLogger(String message) {
        try {
            if (logToFile.length() > 0) {
                BufferedWriter rlog = new BufferedWriter(new FileWriter(logToFile, true));
                rlog.write(message + "\n");
                rlog.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(KluwerDeepSearch.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Setup and send search to Kluwer web service
     * @param query
     * @param from
     * @param to
     * @return
     */
    private JSONObject kluwerSearch(String query, int from, int to) {
        JSONObject data = new JSONObject();

        try {
            org.json.simple.parser.JSONParser jsonParser = new org.json.simple.parser.JSONParser();

            String searchUrl = makeKluwerQuery(query, from, to);

            HashMap<String, String> options = new HashMap<String, String>();
            options.put("Ocp-Apim-Subscription-Key", subscription);
            //options.put("Authorization", authorization);

            String oAuthToken = getOAuthToken();

            options.put("Authorization", "Bearer " + oAuthToken);

            options.put("Accept-language", "nl");
            options.put("Accept", "application/json");

            //Perform search and parse result
            data = (JSONObject) jsonParser.parse(httpGet(searchUrl, options));

        } catch (Exception ex) {
            Logger.getLogger(KluwerDeepSearch.class.getName()).log(Level.SEVERE, null, ex);
        }
        return data;
    }

    /**
     * convert returned JSON into PNX
     * @param data
     * @return
     */
    private PrimoResult json2pnx(JSONObject data){
        PrimoResult result = PrimoResult.Factory.newInstance();
        SEGMENTSDocument.SEGMENTS returnSegment = SEGMENTSDocument.Factory.newInstance().addNewSEGMENTS();
        RESULTDocument.RESULT results = returnSegment.addNewJAGROOT().addNewRESULT();
        DOCSETDocument.DOCSET docSet = results.addNewDOCSET();

        JSONArray entries;
        JSONArray facets;
        try {
            //Map
            String resourceType = "text_resource";
            long totalCount = (long) data.get("total-items");
            long offset = (long) data.get("offset");
            long step = (long) data.get("limit");

            entries = (JSONArray) ((JSONObject) data.get("data")).get("entries");
            facets = (JSONArray) ((JSONObject) ((JSONObject) data.get("data")).get("facets")).get("infokind");
            long count = entries.size();

            FACETLISTDocument.FACETLIST facetList = results.addNewFACETLIST();
            if ((facets != null) && (facets.size() > 0)) {
                FACETDocument.FACET facet = facetList.addNewFACET();
                facet.setNAME("genre");
                facet.setCOUNT(facets.size());

                for (Object entry : facets) {
                    String facetKey = "";
                    String facetValue = "";

                    facetValue = String.valueOf(((JSONObject) entry).get("total"));
                    facetKey = String.valueOf(((JSONObject) entry).get("name"));

                    if (facetKey.length() > 0 && facetValue.length() > 0) {
                        System.out.println(facetKey + " = " + facetValue);

                        FACETVALUESDocument.FACETVALUES value = facet.addNewFACETVALUES();
                        value.setKEY(facetKey);
                        value.setVALUE(facetValue);
                    }
                }
            }

            libisLogger("total=" + totalCount + ", offset=" + offset + ", step=" + step + ",");

            docSet.setTOTALHITS((int) totalCount);
            docSet.setTOTALTIME((long) ((JSONObject) data.get("generation")).get("duration"));
            docSet.setFIRSTHIT(String.valueOf(offset));
            docSet.setLASTHIT(String.valueOf(offset + count));
            docSet.setHITS(String.valueOf(count));
            docSet.setISLOCAL(false);

            for (Object entry : entries) {
                int index = 0;
                PrimoNMBibDocument pnx = PrimoNMBibDocument.Factory.newInstance();
                RecordType record = pnx.addNewPrimoNMBib().addNewRecord();
                String recordId = "";
                String title = "";
                String shortText = "";
                String publicationDate = "";
                String infoKind = "";
                String self = "";
                String score = "";

                for (Object key : ((JSONObject) entry).keySet()) {
                    switch ((String) key) {
                        case "id":
                            recordId = (String) ((JSONObject) entry).get("id");
                            break;
                        case "title":
                            title = (String) ((JSONObject) entry).get("title");
                            break;
                        case "shortText":
                            shortText = (String) ((JSONObject) entry).get("shortText");
                            break;
                        case "publicationDate":
                            publicationDate = (String) ((JSONObject) entry).get("publicationDate");
                            publicationDate = publicationDate.split("T")[0];
                            break;
                        case "infoKind":
                            infoKind = (String) ((JSONObject) entry).get("infoKind");
                            break;
                        case "self":
                            self = (String) ((JSONObject) entry).get("self");
                            break;
                        case "index":
                            index = (int) (long) ((JSONObject) entry).get("index");
                            break;
                        case "score":
                            score = (String) ((JSONObject) entry).get("score");
                    }
                }

                ControlType ct = record.addNewControl();
                ct.addSourceid("Kluwer");
                ct.addRecordid("Kluwer" + recordId);
                ct.addSourcerecordid(recordId);
                ct.addSourcesystem("Kluwer");
                ct.addIlsapiid("Kluwer" + recordId);
                ct.addSourceformat("JSON");

                DisplayType dt = record.addNewDisplay();
                dt.addType(resourceType);
                dt.addTitle(title);
                dt.addPublisher("Kluwer");
                dt.addCreationdate(publicationDate);
                dt.addSnippet(shortText);
                dt.addSource("Assuropolis");

                DeliveryType lt = record.addNewDelivery();
                lt.addDelcategory("Online Resource");
                lt.addInstitution("KBC");
                lt.addResdelscope("RESKBC");

                LinksType it = record.addNewLinks();
                it.addLinktorsrc("$$U" + self + "$$D" + "Kluwer");

                FacetsType ft = record.addNewFacets();
                ft.addRsrctype("text_resources");
                ft.addPrefilter("text_resources");
                ft.addCreationdate(publicationDate);
                ft.addGenre(infoKind);
                ft.addCollection("Assuropolis");
                ft.addToplevel("online_resources");
                ft.addLanguage("dut");
                ft.addAtoz(title.substring(0, 1));

                DOCDocument.DOC doc = docSet.addNewDOC();
                doc.setPrimoNMBib(pnx.getPrimoNMBib());
                doc.setNO(String.valueOf(index + 1));
                doc.setID(index);
                doc.setSEARCHENGINE("Kluwer");
                doc.setRANK(Float.valueOf(score));
            }
        } catch(Exception ex){
            Logger.getLogger(KluwerDeepSearch.class.getName()).log(Level.SEVERE, null, ex);
        }
        result.setSEGMENTS(returnSegment);
        return result;
    }

    /**
     * convert Lucene query into a Kluwer query
     * @param luceneQuery
     * @param from
     * @param to
     * @return
     */
    private String makeKluwerQuery(String luceneQuery, int from, int to) {
        String query = "";
        StringBuilder textQuery = new StringBuilder();
        StringBuilder facetQuery = new StringBuilder();
        StringTokenizer st = new StringTokenizer(luceneQuery);

        while (st.hasMoreTokens()) {
            String token = st.nextToken();

            if (!token.equals("AND") && !token.equals("OR") && !token.equals("NOT")) {
                if (token.contains(":")) {
                    if (token.startsWith("facet_genre")) {
                        if (facetQuery.length() > 0) {
                            facetQuery.append(" ");
                        }

                        String genre = token.substring("facet_genre".length() + 1).trim();
                        genre = genre.replace("\"", "").replace("(", "").replace(")", "").trim();
                        facetQuery.append(genre);

                    }
                } else {
                    if (textQuery.length() > 0) {
                        textQuery.append(" ");
                    }

                    textQuery.append(token.replace("\"", "").replace("(", "").replace(")", "").trim());
                }
            }
        }

        query = "https://api.wolterskluwer.be/search/assuropolis/search?q=" + textQuery.toString() + "&limit=" + (to - from) + "&offset=" + from + "&facets=infoKind";
        if (facetQuery.length() > 0) {
            query += "&infokind=" + facetQuery.toString();
        }

        return query;
    }

    /**
     * Do HTTP get
     * @param query
     * @param headers
     * @return
     */
    private String httpGet(String query, Map<String, String> headers) {
        KluwerDeepSearch.libisLogger(query);

        HttpURLConnection http = null;
        StringBuilder result = new StringBuilder();
        if (mock) {
            libisLogger("MOCK=true : " + mockFile);
            try {
                Path kluwerFile = Paths.get(mockFile);
                return new String(Files.readAllBytes(kluwerFile));
            } catch (Exception ex) {
                Logger.getLogger(KluwerDeepSearch.class.getName()).log(Level.SEVERE, null, ex);
                return "";
            }

        } else {
            try {
                URL parsedUrl = new URL(query);
                URI parsedUri = new URI(parsedUrl.getProtocol(), parsedUrl.getHost() + ":443", parsedUrl.getPath(), parsedUrl.getQuery(), null);
                if (parsedUrl.getPort() > 0) {
                    parsedUri = new URI(parsedUrl.getProtocol(), parsedUrl.getHost() + ":" + parsedUrl.getPort(), parsedUrl.getPath(), parsedUrl.getQuery(), null);
                }

                http = (HttpURLConnection) parsedUri.toURL().openConnection();

                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    http.setRequestProperty(entry.getKey(), entry.getValue());
                }

                http.setInstanceFollowRedirects(true);
                http.setConnectTimeout(10000);
                // http.setReadTimeout(10000);
                http.connect();

                if (http.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    libisLogger(http.getResponseCode() + ":" +http.getResponseMessage());
                }

                BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream(), "UTF-8"));
                String inputline = "";
                while ((inputline = in.readLine()) != null) {
                    result.append(inputline);
                }
                in.close();

                return result.toString();
            } catch (Exception ex) {
                Logger.getLogger(KluwerDeepSearch.class.getName()).log(Level.SEVERE, null, ex);
                return "";
            }
        }
    }

    private String getOAuthToken() {
        String tokenEndPoint = "https://auth.kluwer.be/search/oauth/token";
        String scope         = "search";
        String grantType     = "client_credentials";

        if (clientId == "" || clientSecret == "") {
            throw new RuntimeException("clientId or clientSecret parameters can not be empty");
        }

        HttpURLConnection http = null;
        BASE64Encoder authorizationEncoder = new BASE64Encoder();
        URI parsedUri = null;
        String urlParameters = "grant_type=" + grantType +"&scope=" + scope;
        String authorization = authorizationEncoder.encode((clientId + ":" + clientSecret).getBytes()).replaceAll("\n","");

        try {
            parsedUri = new URI(tokenEndPoint);

            http = (HttpURLConnection) parsedUri.toURL().openConnection();
            http.setRequestMethod("POST");
            http.setRequestProperty("Authorization", "Basic " + authorization);
            http.setRequestProperty("Content-Type","application/x-www-form-urlencoded");

            http.setUseCaches (false);
            http.setDoInput(true);
            http.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream(http.getOutputStream ());
            wr.writeBytes (urlParameters);
            wr.flush ();
            wr.close ();

            //Get Response
            InputStream is = http.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            JSONObject data = (JSONObject) JSONValue.parse(response.toString());

            return (String) data.get("access_token");

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        finally {
            if (http != null) {
                http.disconnect();
            }
        }
    }


}
