/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2025 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.cq.commerce.it.http;

import org.apache.sling.testing.clients.ClientException;
import org.jsoup.nodes.Document;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

public class ItSiteSmokeIT extends ItSiteTestBase {

    private static final String HEADER_XF_MODEL = IT_SITE_XF_ROOT + "/header/master.model.json";

    @Test
    public void testHomePageLoads() throws ClientException {
        Document doc = getPage(IT_SITE_ROOT);
        Assert.assertTrue("Page h1 should contain 'CIF IT Site'",
            doc.select("h1.cmp-title__text").first().text().contains("CIF IT Site"));
    }

    @Test
    public void testNavigationRendered() throws ClientException {
        Document doc = getPage(IT_SITE_ROOT);
        Assert.assertEquals("Navigation should have 6 first-level items",
            6, doc.select("li.cmp-navigation__item--level-0").size());
    }

    @Test
    public void testCommerceNavigationConfigured() throws Exception {
        JsonNode navigation = getJson(HEADER_XF_MODEL).at("/:items/root/:items/navigation");
        Assert.assertFalse("Navigation component should exist at the expected model path",
            navigation.isMissingNode());
        Assert.assertEquals("Navigation should be the CIF commerce navigation component",
            "cif-components-it-site/components/commerce/navigation", navigation.get(":type").asText());
    }

    @Test
    public void testCommerceGraphqlEndpointReachable() throws Exception {
        JsonNode json = executeGraphql("{storeConfig{store_code}}");
        Assert.assertEquals("GraphQL endpoint should return store_code 'default'",
            "default", json.at("/data/storeConfig/store_code").asText());
    }
}
