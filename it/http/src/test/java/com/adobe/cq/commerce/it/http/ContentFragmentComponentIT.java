/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
 *    This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License. You may obtain a copy
 *    of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under
 *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *    OF ANY KIND, either express or implied. See the License for the specific language
 *    governing permissions and limitations under the License.
 *
 ******************************************************************************/

package com.adobe.cq.commerce.it.http;

import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.codehaus.jackson.JsonNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

public class ContentFragmentComponentIT extends CommerceTestBase {

    // Differentiates between the HTML output of the component itself, and the tab displaying the HTML output
    private static final String CONTENT_FRAGMENT_SELECTOR = CMP_EXAMPLES_DEMO_SELECTOR + " .contentfragment ";

    // Skip test for AEM 6.5
    @BeforeClass
    public static void beforeClass() throws Exception {
        JsonNode info = adminAuthor.doGetJson("/system/console/status-productinfo.json", 0);
        Assume.assumeFalse(info.get(6).getValueAsText().split("[\\(\\)]")[1].startsWith("6.5"));
    }

    @Test
    public void testContentFragmenWithSampleData() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/product.chaz-kangeroo-hoodie.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Check the number of content fragment elements in the content fragment component
        Elements elements = doc.select(CONTENT_FRAGMENT_SELECTOR
            + ".cmp-contentfragment > .cmp-contentfragment__elements > .cmp-contentfragment__element");
        Assert.assertEquals(1, elements.size());
    }
}
