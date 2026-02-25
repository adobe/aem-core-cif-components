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

import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VersionHistoryPreviewIT extends CommerceTestBase {

    private static final String SOURCE_PRODUCT_TEASER_PAGE = COMMERCE_LIBRARY_PATH + "/productteaser";
    private static final String VERSION_HISTORY_ROOT = "/tmp/versionhistory/cif-it-hash/cif-it-version";
    private static final String VERSION_HISTORY_PARENT = VERSION_HISTORY_ROOT + "/content/core-components-examples/library/commerce";
    private static final String VERSION_HISTORY_PAGE_NODE = VERSION_HISTORY_PARENT + "/productteaser";
    private static final String VERSION_HISTORY_PAGE_PATH = VERSION_HISTORY_PARENT + "/productteaser.html";
    private static final String PRODUCT_TEASER_SELECTOR = CMP_EXAMPLES_DEMO_SELECTOR + " .productteaser .item__name > span";

    @After
    public void cleanup() throws ClientException {
        if (adminAuthor.exists(VERSION_HISTORY_ROOT)) {
            adminAuthor.deletePath(VERSION_HISTORY_ROOT);
        }
    }

    @Test
    public void testVersionHistoryPathRendersProductTeaser() throws ClientException {
        assertTrue("Source page missing: " + SOURCE_PRODUCT_TEASER_PAGE, adminAuthor.exists(SOURCE_PRODUCT_TEASER_PAGE));
        adminAuthor.createNodeRecursive(VERSION_HISTORY_PARENT, "sling:Folder");
        adminAuthor.copyPage(new String[] { SOURCE_PRODUCT_TEASER_PAGE }, "productteaser", null, VERSION_HISTORY_PARENT, null, false);
        assertTrue("Version history preview page was not created", adminAuthor.exists(VERSION_HISTORY_PAGE_NODE));

        SlingHttpResponse response = adminAuthor.doGet(VERSION_HISTORY_PAGE_PATH, 200);
        Document doc = Jsoup.parse(response.getContent());
        Elements elements = doc.select(PRODUCT_TEASER_SELECTOR);
        assertEquals("Summit Watch", elements.first().html());
    }
}
