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

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import junit.category.IgnoreOn65;
import junit.category.IgnoreOnCloud;

import static org.junit.Assert.assertEquals;

public class ProductListComponentIT extends CommerceTestBase {

    // Differentiates between the HTML output of the component itself, and the tab displaying the HTML output
    private static final String PRODUCTLIST_SELECTOR = CMP_EXAMPLES_DEMO_SELECTOR + " .productlist ";

    @Test
    @Category({ IgnoreOnCloud.class })
    public void testProductListPageWithSampleData65() throws Exception {
        String pagePath = COMMERCE_LIBRARY_PATH + "/productlist/sample-productlist.html/outdoor.html";
        testProductListPageWithSampleData(pagePath, ImmutableMap.of(
            doc -> doc.select("title").first().html(), "Meta title for Outdoor Collection",
            doc -> doc.select("meta[name=keywords]").first().attr("content"), "Meta keywords for Outdoor Collection",
            doc -> doc.select("meta[name=description]").first().attr("content"), "Meta description for Outdoor Collection",
            // 6.5.8 uses the externalizer author link to create the canonical link
            doc -> doc.select("link[rel=canonical]").first().attr("href"), pagePath));
    }

    @Test
    @Category({ IgnoreOn65.class })
    public void testProductListPageWithSampleDataCloud() throws Exception {
        String pagePath = COMMERCE_LIBRARY_PATH + "/productlist/sample-productlist.html/outdoor.html";
        testProductListPageWithSampleData(pagePath, ImmutableMap.of(
            doc -> doc.select("title").first().html(), "Meta title for Outdoor Collection",
            doc -> doc.select("meta[name=keywords]").first().attr("content"), "Meta keywords for Outdoor Collection",
            doc -> doc.select("meta[name=description]").first().attr("content"), "Meta description for Outdoor Collection",
            // without mapping rules we expect the SitemapLinkExternalizer to return the path as is
            doc -> doc.select("link[rel=canonical]").first().attr("href"), pagePath));
    }

    private void testProductListPageWithSampleData(String pagePath, Map<Function<Document, String>, String> expectedMeta) throws Exception {
        SlingHttpResponse response = adminAuthor.doGet(pagePath, 200);
        Document doc = Jsoup.parse(response.getContent());

        // Verify category name
        Elements elements = doc.select(PRODUCTLIST_SELECTOR + ".category__title");
        assertEquals("Outdoor Collection", elements.first().html());

        // Check that search filters are displayed
        elements = doc.select(PRODUCTLIST_SELECTOR + ".productcollection__filters");
        assertEquals(1, elements.size());

        // Check that the 6 products are displayed on the first page
        elements = doc.select(PRODUCTLIST_SELECTOR + ".productcollection__items > .productcollection__item");
        assertEquals(6, elements.size());

        // Check the meta data
        for (Map.Entry<Function<Document, String>, String> testPair : expectedMeta.entrySet()) {
            assertEquals(testPair.getValue(), testPair.getKey().apply(doc));
        }

        // Verify datalayer attributes
        elements = doc.select(PRODUCTLIST_SELECTOR + ".productcollection__root");
        JsonNode result = OBJECT_MAPPER.readTree(elements.first().attr("data-cmp-data-layer"));
        JsonNode expected = OBJECT_MAPPER.readTree(getResource("datalayer/outdoor-productlist.json"));
        assertEquals(expected, result);

        // Verify product items datalayer attributes
        elements = doc.select(PRODUCTLIST_SELECTOR + ".productcollection__items > .productcollection__item");
        result = OBJECT_MAPPER.readTree(elements.stream()
            .map(e -> e.attr("data-cmp-data-layer"))
            .map(e -> e.replaceAll(",\\s*\"repo:modifyDate\":\\s*\"[\\d\\w:-]+\"", ""))
            .collect(Collectors.joining(",", "[", "]")));
        expected = OBJECT_MAPPER.readTree(getResource("datalayer/outdoor-productlist-items.json"));
        assertEquals(expected, result);
    }

    @Test
    public void testProductListPageWithManualSelection() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/productlist/manual-productlist.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Verify category title
        Elements elements = doc.select(PRODUCTLIST_SELECTOR + ".category__title");
        assertEquals("Outdoor Collection", elements.first().html());
    }

    @Test
    public void testProductListPageWithPlaceholderData() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/productlist/sample-productlist.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Verify category name
        Elements elements = doc.select(PRODUCTLIST_SELECTOR + ".category__title");
        assertEquals("Category name", elements.first().html());

        // Check that search filters are NOT displayed
        elements = doc.select(PRODUCTLIST_SELECTOR + ".productcollection__filters");
        Assert.assertTrue(elements.isEmpty());

        // Check that the 6 products are displayed on the first page
        elements = doc.select(PRODUCTLIST_SELECTOR + ".productcollection__items > .productcollection__item");
        assertEquals(6, elements.size());
    }

    @Test
    public void testProductListBreadcrumbWithSampleData() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/productlist/sample-productlist.html/outdoor.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Component Library > Commerce > Outdoor > Collection
        Elements elements = doc.select(BreadcrumbComponentIT.BREADCRUMB_ITEM_SELECTOR);
        assertEquals(5, elements.size());
    }

    @Test
    public void testProductListBreadcrumbWithPlaceholderData() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/productlist/sample-productlist.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Component Library > Commerce
        Elements elements = doc.select(BreadcrumbComponentIT.BREADCRUMB_ITEM_SELECTOR);
        assertEquals(3, elements.size());
    }

    @Test
    public void testCategoryNotFound() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/productlist/sample-productlist.html?wcmmode=disabled");
        assertEquals(404, response.getStatusLine().getStatusCode());
        response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/productlist/sample-productlist.html/unknown-category.html?wcmmode=disabled");
        assertEquals(404, response.getStatusLine().getStatusCode());
    }
}
