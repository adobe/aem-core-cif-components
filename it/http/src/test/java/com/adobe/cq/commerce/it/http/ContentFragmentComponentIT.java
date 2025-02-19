/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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
    public void testContentFragmenWithSampleData() throws ClientException, InterruptedException {
        final String url = COMMERCE_LIBRARY_PATH + "/product/sample-product.html/chaz-kangeroo-hoodie.html";

        for (int i = 0; i < 3; i++) {
            Document doc = Jsoup.parse(adminAuthor.doGet(url, 200).getContent());

            Elements elements = doc.select(CONTENT_FRAGMENT_SELECTOR
                + " .cmp-contentfragment > .cmp-contentfragment__elements > .cmp-contentfragment__element");

            if (elements.size() == 1)
                return; // Exit test if condition is met

            Thread.sleep(2000); // Wait before retrying
        }

        Assert.fail("Expected exactly 1 content fragment but found a different count after retries");
    }

}
