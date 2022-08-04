/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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

public class CommerceListComponentIT extends CommerceTestBase {

    // Differentiates between the HTML output of the component itself, and the tab displaying the HTML output
    private static final String LIST_SELECTOR = CMP_EXAMPLES_DEMO_SELECTOR + " .list ";

    @Test
    public void testCommerceListWithSampleData() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/list.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Check for 4 lists on the page
        Elements elements = doc.select(LIST_SELECTOR);
        Assert.assertEquals(4, elements.size());

        // Check titles
        Elements titleElements = elements.get(0).select(".cmp-list__item .cmp-list__item-title");
        Assert.assertEquals(2, titleElements.size());
        Assert.assertTrue(checkMatch("Product", "Commerce List", titleElements.first(), titleElements.last()));

        titleElements = elements.get(1).select(".cmp-list__item .cmp-list__item-title");
        Assert.assertEquals(2, titleElements.size());
        Assert.assertTrue(checkMatch("Product List", "Commerce List", titleElements.first(), titleElements.last()));

        // items as links
        titleElements = elements.get(2).select(".cmp-list__item-link .cmp-list__item-title");
        Assert.assertEquals(2, titleElements.size());
        Assert.assertTrue(checkMatch("Product List", "Commerce List", titleElements.first(), titleElements.last()));

        // items as teasers
        titleElements = elements.get(3).select(".cmp-list__item .cmp-teaser__title");
        Assert.assertEquals(2, titleElements.size());
        Assert.assertTrue(checkMatch("Product List", "Commerce List", titleElements.first(), titleElements.last()));
    }

    private static boolean checkMatch(String s1, String s2, Element e1, Element e2) {
        return s1.equals(e1.html()) && s2.equals(e2.html()) || s1.equals(e2.html()) && s2.equals(e1.html());
    }
}
