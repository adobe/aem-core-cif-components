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
        String productPageUrl = COMMERCE_LIBRARY_PATH + "/product/sample-product.html/chaz-kangeroo-hoodie.html";
        int retries = 3;
        boolean isPageLoaded = false;

        // Retry logic to handle occasional 404 errors
        while (retries > 0 && !isPageLoaded) {
            // Fetch the page
            SlingHttpResponse response = adminAuthor.doGet(productPageUrl, 200);

            // Get the status code from the response
            int statusCode = response.getStatusLine().getStatusCode();

            // Check for 404 error and retry if necessary
            if (statusCode == 404) {
                retries--;
                try {
                    Thread.sleep(5000); // Wait for 5 seconds before retrying
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                // Page loaded successfully
                isPageLoaded = true;

                // Wait for the content fragment to be present (adjust time as needed)
                try {
                    Thread.sleep(5000); // Optional: Allow some time for the content to load
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Parse the response content
                Document doc = Jsoup.parse(response.getContent());

                // Check the number of content fragment elements in the content fragment component
                Elements elements = doc.select(CONTENT_FRAGMENT_SELECTOR
                        + ".cmp-contentfragment > .cmp-contentfragment__elements > .cmp-contentfragment__element");

                // Assert that exactly 1 content fragment element is present
                Assert.assertEquals(1, elements.size());
            }
        }

        // Fail the test if the page is not loaded after retries
        if (!isPageLoaded) {
            throw new ClientException("Page not found after multiple retries");
        }
    }

}
