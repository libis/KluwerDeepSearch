package com.exlibris.primo.thirdnode.thirdnodeimpl;

import com.exlibris.jaguar.xsd.search.FACETLISTDocument;
import com.exlibris.primo.xsd.commonData.PrimoResult;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Created by mehmetc on 30/09/15.
 */
public class KluwerDeepSearchTest {
    private KluwerDeepSearch kluwerSearch;

    @Before
    public void setUp() throws Exception {
        kluwerSearch =  new KluwerDeepSearch();
        HashMap<String, String> config = new HashMap<>();
        config.put("log_to_file", "./out/test/test.log");
        config.put("clientId", "kbc.limo.client");
        config.put("clientSecret", System.getenv("KBC_LIMO_SECRET"));
        config.put("subscription", System.getenv("KBC_LIMO_SUBSCRIPTION"));
        kluwerSearch.init(config);
    }

    @Test
    public void testSearch() throws Exception {
        String vid = "KBC";
        String query = "auto";


        PrimoResult result = kluwerSearch.search(vid, query, 5, 10, new HashMap(), "stitle");

        assertEquals("5", result.getSEGMENTS().getJAGROOTArray(0).getRESULT().getDOCSET().getHITS());
    }


}