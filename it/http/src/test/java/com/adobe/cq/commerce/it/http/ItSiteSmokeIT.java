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
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.junit.rules.CQAuthorClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Smoke tests for the CIF IT Site — verifies the site loads and the commerce navigation is configured.
 */
public class ItSiteSmokeIT {

    private static final String IT_SITE_HOME = "/content/cif-components-it-site/us/en";
    private static final String HEADER_XF_MODEL = "/content/experience-fragments/cif-components-it-site/us/en/site/header/master.model.json";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ClassRule
    public static final CQAuthorClassRule cqBaseClassRule = new CQAuthorClassRule();

    @Rule
    public CQRule cqBaseRule = new CQRule(cqBaseClassRule.authorRule);

    private static CQClient adminAuthor;

    @BeforeClass
    public static void init() throws ClientException {
        adminAuthor = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);
    }

    @Test
    public void testHomePageLoads() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(IT_SITE_HOME + ".html", 200);
        Document doc = Jsoup.parse(response.getContent());
        Assert.assertTrue("Page h1 should contain 'CIF IT Site'",
            doc.select("h1.cmp-title__text").first().text().contains("CIF IT Site"));
    }

    @Test
    public void testNavigationRendered() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(IT_SITE_HOME + ".html", 200);
        Document doc = Jsoup.parse(response.getContent());
        Elements navContainer = doc.select("div.navigation");
        Assert.assertTrue("Navigation container should be present in the page", navContainer.size() > 0);
    }

    @Test
    public void testCommerceNavigationConfigured() throws Exception {
        SlingHttpResponse response = adminAuthor.doGet(HEADER_XF_MODEL, 200);
        JsonNode json = MAPPER.readTree(response.getContent());
        JsonNode navigation = json.at("/:items/root/:items/navigation");

        Assert.assertFalse("Navigation component should exist at the expected model path",
            navigation.isMissingNode());
        Assert.assertEquals("Navigation should be the CIF commerce navigation component",
            "cif-components-it-site/components/commerce/navigation", navigation.get(":type").asText());
    }
}
