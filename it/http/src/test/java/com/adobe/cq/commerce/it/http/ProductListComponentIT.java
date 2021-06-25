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

import java.util.stream.Collectors;

import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

public class ProductListComponentIT extends CommerceTestBase {

    // Differentiates between the HTML output of the component itself, and the tab displaying the HTML output
    private static final String PRODUCTLIST_SELECTOR = CMP_EXAMPLES_DEMO_SELECTOR + " .productlist ";

    @Test
    public void testProductListPageWithSampleData() throws Exception {
        String pagePath = COMMERCE_LIBRARY_PATH + "/productlist.outdoor.html";
        SlingHttpResponse response = adminAuthor.doGet(pagePath, 200);
        Document doc = Jsoup.parse(response.getContent());

        // Verify category name
        Elements elements = doc.select(PRODUCTLIST_SELECTOR + ".category__categoryTitle");
        Assert.assertEquals("Outdoor Collection", elements.first().html());

        // Check that search filters are displayed
        elements = doc.select(PRODUCTLIST_SELECTOR + ".search__filters");
        Assert.assertEquals(1, elements.size());

        // Check that the 6 products are displayed on the first page
        elements = doc.select(PRODUCTLIST_SELECTOR + ".gallery__items > .item__root");
        Assert.assertEquals(6, elements.size());

        // Check the meta data
        elements = doc.select("title");
        Assert.assertEquals("Meta title for Outdoor Collection", elements.first().html());

        elements = doc.select("meta[name=keywords]");
        Assert.assertEquals("Meta keywords for Outdoor Collection", elements.first().attr("content"));

        elements = doc.select("meta[name=description]");
        Assert.assertEquals("Meta description for Outdoor Collection", elements.first().attr("content"));

        elements = doc.select("link[rel=canonical]");
        Assert.assertEquals("http://localhost:4502" + pagePath, elements.first().attr("href"));

        // Verify datalayer attributes
        elements = doc.select(PRODUCTLIST_SELECTOR + ".gallery__root");
        JsonNode result = OBJECT_MAPPER.readTree(elements.first().attr("data-cmp-data-layer"));
        JsonNode expected = OBJECT_MAPPER.readTree(getResource("datalayer/outdoor-productlist.json"));
        Assert.assertEquals(expected, result);

        // Verify product items datalayer attributes
        elements = doc.select(PRODUCTLIST_SELECTOR + ".gallery__items > .item__root");
        result = OBJECT_MAPPER.readTree(elements.stream()
            .map(e -> e.attr("data-cmp-data-layer"))
            .map(e -> e.replaceAll(",\\s*\"repo:modifyDate\":\\s*\"[\\d\\w:-]+\"", ""))
            .collect(Collectors.joining(",", "[", "]")));
        expected = OBJECT_MAPPER.readTree(getResource("datalayer/outdoor-productlist-items.json"));
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testProductListPageWithPlaceholderData() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/productlist.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Verify category name
        Elements elements = doc.select(PRODUCTLIST_SELECTOR + ".category__categoryTitle");
        Assert.assertEquals("Category name", elements.first().html());

        // Check that search filters are NOT displayed
        elements = doc.select(PRODUCTLIST_SELECTOR + ".search__filters");
        Assert.assertTrue(elements.isEmpty());

        // Check that the 6 products are displayed on the first page
        elements = doc.select(PRODUCTLIST_SELECTOR + ".gallery__items > .item__root");
        Assert.assertEquals(6, elements.size());
    }

    @Test
    public void testProductListBreadcrumbWithSampleData() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/productlist.outdoor.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Component Library > Commerce > Outdoor > Collection
        Elements elements = doc.select(BreadcrumbComponentIT.BREADCRUMB_ITEM_SELECTOR);
        Assert.assertEquals(4, elements.size());
    }

    @Test
    public void testProductListBreadcrumbWithPlaceholderData() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/productlist.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Component Library > Commerce
        Elements elements = doc.select(BreadcrumbComponentIT.BREADCRUMB_ITEM_SELECTOR);
        Assert.assertEquals(2, elements.size());
    }
}
