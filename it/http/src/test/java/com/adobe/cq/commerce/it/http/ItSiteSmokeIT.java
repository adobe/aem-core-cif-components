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

    @Test
    public void testCommerceGraphqlEndpointReachable() throws Exception {
        printGraphqlDiagnostics();

        SlingHttpResponse response = adminAuthor.doGet(
            "/api/graphql?query=%7BstoreConfig%7Bstore_code%7D%7D", 200, 404, 500, 403, 401);
        int status = response.getStatusLine().getStatusCode();
        System.out.println("[DEBUG] /api/graphql response status: " + status);
        if (status != 200) {
            System.err.println("[DEBUG] /api/graphql returned " + status + ". Response body:\n" + response.getContent());
        }

        Assert.assertEquals("Expected HTTP Status: 200 for /api/graphql", 200, status);

        JsonNode json = MAPPER.readTree(response.getContent());
        Assert.assertEquals("GraphQL endpoint should return store_code 'default'",
            "default", json.at("/data/storeConfig/store_code").asText());
    }

    private static void printGraphqlDiagnostics() {
        // AEM product info — confirms which AEM version is under test
        try {
            SlingHttpResponse prodInfo = adminAuthor.doGet("/system/console/status-productinfo.json", 200, 404);
            if (prodInfo.getStatusLine().getStatusCode() == 200) {
                JsonNode info = MAPPER.readTree(prodInfo.getContent());
                System.out.println("[DEBUG] AEM product info: " + info.path("data").toString());
            }
        } catch (Exception e) {
            System.out.println("[DEBUG] Could not read product info: " + e.getMessage());
        }

        // All bundles whose symbolic name contains "cif", "commerce", or "graphql"
        try {
            SlingHttpResponse bundlesResp = adminAuthor.doGet("/system/console/bundles.json", 200);
            JsonNode bundleData = MAPPER.readTree(bundlesResp.getContent()).path("data");
            System.out.println("[DEBUG] ========== CIF / Commerce bundle inventory ==========");
            if (bundleData.isArray()) {
                for (JsonNode bundle : bundleData) {
                    String sym = bundle.path("symbolicName").asText("");
                    if (sym.contains("cif") || sym.contains("commerce") || sym.contains("graphql")) {
                        String ver = bundle.path("version").asText("?");
                        String state = bundle.path("state").asText("?");
                        int id = bundle.path("id").asInt(-1);
                        System.out.println("[DEBUG]  " + sym + " v" + ver + " -> " + state);

                        // For bundles not Active/Fragment, fetch the detail page to show why
                        if (!"Active".equals(state) && !"Fragment".equals(state) && id >= 0) {
                            printBundleDetails(id, sym);
                        }
                    }
                }
            }
            System.out.println("[DEBUG] =====================================================");
        } catch (Exception e) {
            System.err.println("[DEBUG] Bundle inventory failed: " + e.getMessage());
        }
    }

    private static void printBundleDetails(int bundleId, String sym) {
        try {
            SlingHttpResponse detail = adminAuthor.doGet("/system/console/bundles/" + bundleId + ".json", 200);
            JsonNode props = MAPPER.readTree(detail.getContent()).path("data").path(0).path("props");
            if (props.isArray()) {
                for (JsonNode prop : props) {
                    String key = prop.path("key").asText("");
                    // Print anything that explains why the bundle is not Active
                    if (key.contains("Unsatisfied") || key.contains("Import-Package")
                        || key.contains("Require-Bundle") || key.contains("Export-Package")
                        || key.equals("Status")) {
                        System.err.println("[DEBUG]    !! [" + sym + "] " + key + ": " + prop.path("value"));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DEBUG]    Could not fetch details for bundle " + sym + " (id=" + bundleId + "): " + e.getMessage());
        }
    }
}
