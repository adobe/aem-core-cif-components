/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.Test;

public class ProductTeaserComponentIT extends CommerceTestBase {

    // Differentiates between the HTML output of the component itself, and the tab displaying the HTML output
    private static final String PRODUCTTEASER_SELECTOR = CMP_EXAMPLES_DEMO_SELECTOR + " .productteaser ";

    @Test
    public void testProductTeaserWithSampleData() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/productteaser.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Verify product name
        Elements elements = doc.select(PRODUCTTEASER_SELECTOR + ".item__name > span");
        Assert.assertEquals("Summit Watch", elements.first().html());

        Elements teasers = doc.select(PRODUCTTEASER_SELECTOR + ".item__root");
        Assert.assertFalse("Expected rendered product teasers", teasers.isEmpty());
        for (Element teaser : teasers) {
            Elements images = teaser.select("a.item__images");
            Elements titles = teaser.select("a.item__name");
            Assert.assertEquals(1, images.size());
            Assert.assertEquals(1, titles.size());
            Element image = images.first();
            Element title = titles.first();
            Assert.assertFalse(image.attr("href").isEmpty());
            Assert.assertEquals(title.attr("href"), image.attr("href"));
            Assert.assertEquals("-1", image.attr("tabindex"));
            Assert.assertEquals("true", image.attr("aria-hidden"));
            Assert.assertFalse(title.hasAttr("tabindex"));
            Assert.assertFalse(title.hasAttr("aria-hidden"));
            Assert.assertFalse(image.hasAttr("target"));
            Assert.assertFalse(title.hasAttr("target"));
        }
    }
}
