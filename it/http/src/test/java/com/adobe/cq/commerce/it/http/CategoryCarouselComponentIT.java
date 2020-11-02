/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.Test;

public class CategoryCarouselComponentIT extends CommerceTestBase {

    // Differentiates between the HTML output of the component itself, and the tab displaying the HTML output
    private static final String CATEGORYCAROUSEL_SELECTOR = CMP_EXAMPLES_DEMO_SELECTOR + " .categorycarousel ";

    @Test
    public void testCategoryCarouselWithSampleData() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/categorycarousel.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Check title
        Elements elements = doc.select(CATEGORYCAROUSEL_SELECTOR + ".carousel__title");
        Assert.assertEquals("Trending product categories", elements.first().html());

        // Check that the components displays 4 categories
        elements = doc.select(CATEGORYCAROUSEL_SELECTOR + ".carousel__cardscontainer > .categorycarousel__card");
        Assert.assertEquals(4, elements.size());
    }
}
