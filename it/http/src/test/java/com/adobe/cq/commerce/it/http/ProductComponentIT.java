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

import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import junit.category.IgnoreOn65;
import junit.category.IgnoreOnCloud;

import static org.junit.Assert.assertEquals;

public class ProductComponentIT extends CommerceTestBase {

    // Differentiates between the HTML output of the component itself, and the tab displaying the HTML output
    private static final String PRODUCT_SELECTOR = CMP_EXAMPLES_DEMO_SELECTOR + " .product ";

    @Test
    @Category({ IgnoreOnCloud.class })
    public void testProductPageWithSampleData65() throws Exception {
        String pagePath = COMMERCE_LIBRARY_PATH + "/product.html/chaz-kangeroo-hoodie.html";
        testProductPageWithSampleData(pagePath, ImmutableMap.of(
            doc -> doc.select("title").first().html(), "Meta title for Chaz Kangeroo Hoodie",
            doc -> doc.select("meta[name=keywords]").first().attr("content"), "Meta keywords for Chaz Kangeroo Hoodie",
            doc -> doc.select("meta[name=description]").first().attr("content"), "Meta description for Chaz Kangeroo Hoodie",
            // 6.5.8 uses the externalizer author link to create the canonical link
            doc -> doc.select("link[rel=canonical]").first().attr("href"), "http://localhost:4502" + pagePath));
    }

    @Test
    @Category({ IgnoreOn65.class })
    public void testProductPageWithSampleDataCloud() throws Exception {
        String pagePath = COMMERCE_LIBRARY_PATH + "/product.html/chaz-kangeroo-hoodie.html";
        testProductPageWithSampleData(pagePath, ImmutableMap.of(
            doc -> doc.select("title").first().html(), "Meta title for Chaz Kangeroo Hoodie",
            doc -> doc.select("meta[name=keywords]").first().attr("content"), "Meta keywords for Chaz Kangeroo Hoodie",
            doc -> doc.select("meta[name=description]").first().attr("content"), "Meta description for Chaz Kangeroo Hoodie",
            // without mapping rules we expect the SitemapLinkExternalizer to return the path as is
            doc -> doc.select("link[rel=canonical]").first().attr("href"), pagePath));
    }

    private void testProductPageWithSampleData(String pagePath, Map<Function<Document, String>, String> expectedMetadata) throws Exception {
        SlingHttpResponse response = adminAuthor.doGet(pagePath, 200);
        Document doc = Jsoup.parse(response.getContent());

        // Verify product name
        Elements elements = doc.select(PRODUCT_SELECTOR + ".productFullDetail__productName > span");
        assertEquals("Chaz Kangeroo Hoodie", elements.first().html());

        // Verify that the section for GroupedProduct is NOT displayed
        assertEquals(0, doc.select(".productFullDetail__groupedProducts").size());

        // Check the meta data
        for (Map.Entry<Function<Document, String>, String> expectedMetadataPair : expectedMetadata.entrySet()) {
            assertEquals(expectedMetadataPair.getValue(), expectedMetadataPair.getKey().apply(doc));
        }

        // Verify datalayer attributes
        elements = doc.select(PRODUCT_SELECTOR + "> .productFullDetail__root");
        JsonNode result = OBJECT_MAPPER.readTree(elements.first().attr("data-cmp-data-layer"));
        JsonNode expected = OBJECT_MAPPER.readTree(getResource("datalayer/chaz-kangeroo-hoodie-product.json"));
        assertEquals(expected, result);
    }

    @Test
    public void testProductWithManualSelection() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/product/manual-product.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Verify product name
        Elements elements = doc.select(PRODUCT_SELECTOR + ".productFullDetail__productName > span");
        assertEquals("Chaz Kangeroo Hoodie", elements.first().html());

        // Verify that the section for GroupedProduct is NOT displayed
        assertEquals(0, doc.select(".productFullDetail__groupedProducts").size());
    }

    @Test
    public void testProductPageWithSampleDataForGroupedProduct() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/product.html/set-of-sprite-yoga-straps.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Verify product name
        Elements elements = doc.select(PRODUCT_SELECTOR + ".productFullDetail__productName > span");
        assertEquals("Set of Sprite Yoga Straps", elements.first().html());

        // Verify that the section for GroupedProduct is displayed
        assertEquals(1, doc.select(PRODUCT_SELECTOR + ".productFullDetail__groupedProducts").size());
    }

    @Test
    public void testProductPageWithPlaceholderData() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/product.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Verify product name
        Elements elements = doc.select(PRODUCT_SELECTOR + ".productFullDetail__productName > span");
        assertEquals("Product name", elements.first().html());
    }

    @Test
    public void testProductBreadcrumbWithSampleData() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/product.html/chaz-kangeroo-hoodie.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Component Library > Commerce > Outdoor > Collection > Chaz Kangeroo Hoodie
        Elements elements = doc.select(BreadcrumbComponentIT.BREADCRUMB_ITEM_SELECTOR);
        assertEquals(5, elements.size());
    }

    @Test
    public void testProductBreadcrumbWithPlaceholderData() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/product.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Component Library > Commerce
        Elements elements = doc.select(BreadcrumbComponentIT.BREADCRUMB_ITEM_SELECTOR);
        assertEquals(2, elements.size());
    }

    @Test
    public void testProductNotFound() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/product.html?wcmmode=disabled");
        assertEquals(404, response.getStatusLine().getStatusCode());
        response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/product.html/unknown-product.html?wcmmode=disabled");
        assertEquals(404, response.getStatusLine().getStatusCode());
    }
}
