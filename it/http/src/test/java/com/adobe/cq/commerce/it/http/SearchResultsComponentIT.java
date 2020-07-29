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

public class SearchResultsComponentIT extends CommerceTestBase {

    // Differentiates between the HTML output of the component itself, and the tab displaying the HTML output
    private static final String SEARCHRESULTS_SELECTOR = CMP_EXAMPLES_DEMO_SELECTOR + " .searchresults ";

    @Test
    public void testSearchResultsWithSampleData() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/search.html?search_query=test", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Check that search filters are displayed
        Elements elements = doc.select(SEARCHRESULTS_SELECTOR + ".search__filters");
        Assert.assertEquals(1, elements.size());

        // Check that the 6 products are displayed on the first page
        elements = doc.select(SEARCHRESULTS_SELECTOR + ".gallery__items > .item__root");
        Assert.assertEquals(6, elements.size());
    }

    @Test
    public void testSearchResultsWithoutSearchQuery() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/search.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Check that the search doesn't display any product
        Elements elements = doc.select(SEARCHRESULTS_SELECTOR + ".category__root p");
        Assert.assertEquals("No products to display.", elements.first().html());
    }
}
