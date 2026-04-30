/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;

import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.junit.rules.CQAuthorClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base class for integration tests targeting the CIF IT Site
 * at {@code /content/cif-components-it-site}.
 *
 * Provides shared client setup and request utilities (page HTML, JSON model,
 * GraphQL) so individual test classes stay focused on assertions.
 */
public class ItSiteTestBase {

    protected static final String IT_SITE_ROOT = "/content/cif-components-it-site/us/en";
    protected static final String IT_SITE_XF_ROOT = "/content/experience-fragments/cif-components-it-site/us/en/site";
    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @ClassRule
    public static final CQAuthorClassRule cqBaseClassRule = new CQAuthorClassRule();

    @Rule
    public CQRule cqBaseRule = new CQRule(cqBaseClassRule.authorRule);

    protected static CQClient adminAuthor;

    @BeforeClass
    public static void initBase() throws ClientException {
        adminAuthor = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);
    }

    /**
     * Fetches a page and returns its HTML parsed as a Jsoup Document.
     *
     * @param pagePath JCR path without extension, e.g. {@code /content/cif-components-it-site/us/en}
     */
    protected Document getPage(String pagePath) throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(pagePath + ".html", 200);
        return Jsoup.parse(response.getContent());
    }

    /**
     * Fetches a Sling model JSON export and returns the parsed JsonNode.
     *
     * @param modelPath full path including extension, e.g. {@code /content/.../master.model.json}
     */
    protected JsonNode getJson(String modelPath) throws Exception {
        SlingHttpResponse response = adminAuthor.doGet(modelPath, 200);
        return OBJECT_MAPPER.readTree(response.getContent());
    }

    /**
     * Executes a GraphQL query against the IT site endpoint and returns the parsed response.
     *
     * @param query raw GraphQL query string, e.g. {@code {storeConfig{store_code}}}
     */
    protected JsonNode executeGraphql(String query) throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
        SlingHttpResponse response = adminAuthor.doGet("/api/graphql?query=" + encoded, 200);
        return OBJECT_MAPPER.readTree(response.getContent());
    }

    /**
     * Posts a JSON body to the given path and returns the parsed response.
     *
     * @param path JCR/servlet path, e.g. {@code /bin/cif/invalidate-cache}
     * @param jsonBody raw JSON string to send as request body
     * @param expectedStatus HTTP status codes to accept (vararg — pass none to skip status check)
     */
    protected SlingHttpResponse postJson(String path, String jsonBody, int... expectedStatus) throws ClientException {
        StringEntity entity = new StringEntity(jsonBody, ContentType.APPLICATION_JSON);
        return adminAuthor.doPost(path, entity, null, expectedStatus);
    }
}
