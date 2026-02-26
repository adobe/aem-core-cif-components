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

import org.apache.http.HttpEntity;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.util.FormEntityBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VersionHistoryPreviewIT extends CommerceTestBase {

    private static final String SOURCE_PRODUCT_TEASER_PAGE = COMMERCE_LIBRARY_PATH + "/productteaser";
    private static final String VERSION_HISTORY_ROOT_BASE = "/tmp/versionhistory/cif-it-hash";
    private static final String VERSION_HISTORY_PAGE_SUFFIX = "/content/core-components-examples/library/commerce/productteaser";
    private static final String PRODUCT_TEASER_SELECTOR = CMP_EXAMPLES_DEMO_SELECTOR + " .productteaser .item__name > span";
    private String versionHistoryRoot;
    private String versionHistoryPagePath;

    @Before
    public void setup() throws ClientException {
        assertTrue("Source page missing: " + SOURCE_PRODUCT_TEASER_PAGE, adminAuthor.exists(SOURCE_PRODUCT_TEASER_PAGE));
        versionHistoryRoot = VERSION_HISTORY_ROOT_BASE + "/cif-it-version-" + System.currentTimeMillis();
        String versionHistoryParent = versionHistoryRoot + "/content/core-components-examples/library/commerce";
        String versionHistoryPageNode = versionHistoryRoot + VERSION_HISTORY_PAGE_SUFFIX;
        versionHistoryPagePath = versionHistoryPageNode + ".html";

        adminAuthor.createNodeRecursive(versionHistoryParent, "sling:Folder");
        HttpEntity copyEntity = FormEntityBuilder.create()
            .addParameter(":operation", "copy")
            .addParameter(":dest", versionHistoryPageNode)
            .build();
        adminAuthor.doPost(SOURCE_PRODUCT_TEASER_PAGE, copyEntity, 200, 201);
        assertTrue("Version history preview page was not created", adminAuthor.exists(versionHistoryPageNode));
    }

    @After
    public void cleanup() throws ClientException {
        if (versionHistoryRoot != null && adminAuthor.exists(versionHistoryRoot)) {
            adminAuthor.deletePath(versionHistoryRoot);
        }
    }

    @Test
    public void testVersionHistoryPathRendersProductTeaser() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(versionHistoryPagePath, 200);
        Document doc = Jsoup.parse(response.getContent());
        Elements elements = doc.select(PRODUCT_TEASER_SELECTOR);
        assertEquals("Summit Watch", elements.first().html());
    }
}
