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

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.util.FormEntityBuilder;
import org.apache.sling.testing.clients.util.JsonUtils;
import org.codehaus.jackson.JsonNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VersionHistoryPreviewIT extends CommerceTestBase {

    private static final String VERSION_HISTORY_ROOT = "/tmp/versionhistory/";
    private static final String VERSION_HISTORY_PAGE_SUFFIX = "/content/core-components-examples/library/commerce/productteaser";
    private static final String SOURCE_PRODUCT_TEASER_PAGE = VERSION_HISTORY_PAGE_SUFFIX;
    private static final String VERSION_HISTORY_SERVLET = "/mnt/overlay/wcm/core/content/sites/versionhistory/_jcr_content.txt";
    private static final String PRODUCT_TEASER_SELECTOR = CMP_EXAMPLES_DEMO_SELECTOR + " .productteaser .item__name > span";
    private static final int VERSION_POLL_ATTEMPTS = 30;
    private static final long VERSION_POLL_DELAY_MS = 1000L;
    private String versionHistoryPagePath;
    private String versionHistoryVersionRoot;

    @Before
    public void setup() throws Exception {
        String label = "it-version-" + System.currentTimeMillis();
        adminAuthor.createVersion(SOURCE_PRODUCT_TEASER_PAGE, "IT version", label);
        String versionId = waitForVersionId(SOURCE_PRODUCT_TEASER_PAGE, label);
        versionHistoryPagePath = determinePreviewUrl(versionId);
        versionHistoryVersionRoot = getVersionHistoryVersionRoot(versionHistoryPagePath);
        assertTrue("Version preview URL should be a version history path", versionHistoryPagePath.contains(VERSION_HISTORY_ROOT));
        assertTrue("Version preview URL should end with .html", versionHistoryPagePath.endsWith(".html"));
    }

    @After
    public void cleanup() throws ClientException {
        if (versionHistoryVersionRoot != null && adminAuthor.exists(versionHistoryVersionRoot)) {
            adminAuthor.deletePath(versionHistoryVersionRoot);
        }
    }

    @Test
    public void testVersionHistoryPathRendersProductTeaser() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(versionHistoryPagePath, 200);
        Document doc = Jsoup.parse(response.getContent());
        Elements elements = doc.select(PRODUCT_TEASER_SELECTOR);
        assertEquals("Summit Watch", elements.first().html());
    }

    private String determinePreviewUrl(String versionId) throws ClientException {
        UrlEncodedFormEntity formEntity = FormEntityBuilder.create()
            .addParameter("wcmmode", "disabled")
            .addParameter("versionId", versionId)
            .build();
        SlingHttpResponse response = adminAuthor.doPost(VERSION_HISTORY_SERVLET, formEntity, 200);
        return response.getContent().trim() + ".html";
    }

    private String waitForVersionId(String pagePath, String label) throws Exception {
        String versionId = null;
        for (int attempt = 0; attempt < VERSION_POLL_ATTEMPTS; attempt++) {
            versionId = getVersionIdByLabel(pagePath, label);
            if (versionId != null) {
                return versionId;
            }
            Thread.sleep(VERSION_POLL_DELAY_MS);
        }
        throw new AssertionError("Version with label '" + label + "' was not created for " + pagePath);
    }

    private String getVersionIdByLabel(String pagePath, String label) throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet("/bin/wcm/versions.json?path=" + pagePath + "&showChildren=false", 200);
        JsonNode versions = JsonUtils.getJsonNodeFromString(response.getContent()).path("versions");
        if (versions == null || versions.size() == 0) {
            return null;
        }
        for (int i = 0; i < versions.size(); i++) {
            JsonNode version = versions.get(i);
            if (label.equals(version.path("label").getTextValue())) {
                return version.path("id").getTextValue();
            }
        }
        return null;
    }

    private String getVersionHistoryVersionRoot(String previewPagePath) {
        int start = previewPagePath.indexOf(VERSION_HISTORY_ROOT);
        if (start < 0) {
            return null;
        }

        int hashStart = start + VERSION_HISTORY_ROOT.length();
        int hashEnd = previewPagePath.indexOf('/', hashStart);
        if (hashEnd < 0) {
            return null;
        }

        int versionEnd = previewPagePath.indexOf('/', hashEnd + 1);
        if (versionEnd < 0) {
            return null;
        }

        return previewPagePath.substring(0, versionEnd);
    }
}
