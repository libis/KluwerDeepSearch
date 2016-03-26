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
    }

    @Test
    public void testSearch() throws Exception {
        String vid = "KBC";
        String query = "((auto)) AND facet_rsss:(\"books\") AND facet_genre:(\"memento\") AND scope:(cb94ede6-313e-482c-9c35-8e539ba8d919)";


        PrimoResult result = kluwerSearch.search(vid, query, 5, 10, new HashMap(), "stitle");
        //FACETLISTDocument.FACETLIST facets = kluwerSearch.countFacets(query,"stitle", kluwerSearch., new HashMap(), "")
        System.out.println(result.toString());
    //    FACETLISTDocument.FACETLIST facetlist =  kluwerSearch.countFacets(query,"stitle", kluwerSearch.facets)
    //    System.out.println(facetlist.toString());

        assertTrue(true);
    }


}