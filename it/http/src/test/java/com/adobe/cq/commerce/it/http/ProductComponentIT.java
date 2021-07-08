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

import com.fasterxml.jackson.databind.JsonNode;

public class ProductComponentIT extends CommerceTestBase {

    // Differentiates between the HTML output of the component itself, and the tab displaying the HTML output
    private static final String PRODUCT_SELECTOR = CMP_EXAMPLES_DEMO_SELECTOR + " .product ";

    @Test
    public void testProductPageWithSampleData() throws Exception {
        String pagePath = COMMERCE_LIBRARY_PATH + "/product.html/chaz-kangeroo-hoodie.html";
        SlingHttpResponse response = adminAuthor.doGet(pagePath, 200);
        Document doc = Jsoup.parse(response.getContent());

        // Verify product name
        Elements elements = doc.select(PRODUCT_SELECTOR + ".productFullDetail__productName > span");
        Assert.assertEquals("Chaz Kangeroo Hoodie", elements.first().html());

        // Verify that the section for GroupedProduct is NOT displayed
        Assert.assertEquals(0, doc.select(".productFullDetail__groupedProducts").size());

        // Check the meta data
        elements = doc.select("title");
        Assert.assertEquals("Meta title for Chaz Kangeroo Hoodie", elements.first().html());

        elements = doc.select("meta[name=keywords]");
        Assert.assertEquals("Meta keywords for Chaz Kangeroo Hoodie", elements.first().attr("content"));

        elements = doc.select("meta[name=description]");
        Assert.assertEquals("Meta description for Chaz Kangeroo Hoodie", elements.first().attr("content"));

        elements = doc.select("link[rel=canonical]");
        Assert.assertEquals("http://localhost:4502" + pagePath, elements.first().attr("href"));

        // Verify datalayer attributes
        elements = doc.select(PRODUCT_SELECTOR + "> .productFullDetail__root");
        JsonNode result = OBJECT_MAPPER.readTree(elements.first().attr("data-cmp-data-layer"));
        JsonNode expected = OBJECT_MAPPER.readTree(getResource("datalayer/chaz-kangeroo-hoodie-product.json"));
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testProductWithManualSelection() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/product/manual-product.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Verify product name
        Elements elements = doc.select(PRODUCT_SELECTOR + ".productFullDetail__productName > span");
        Assert.assertEquals("Chaz Kangeroo Hoodie", elements.first().html());

        // Verify that the section for GroupedProduct is NOT displayed
        Assert.assertEquals(0, doc.select(".productFullDetail__groupedProducts").size());
    }

    @Test
    public void testProductPageWithSampleDataForGroupedProduct() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/product.html/set-of-sprite-yoga-straps.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Verify product name
        Elements elements = doc.select(PRODUCT_SELECTOR + ".productFullDetail__productName > span");
        Assert.assertEquals("Set of Sprite Yoga Straps", elements.first().html());

        // Verify that the section for GroupedProduct is displayed
        Assert.assertEquals(1, doc.select(PRODUCT_SELECTOR + ".productFullDetail__groupedProducts").size());
    }

    @Test
    public void testProductPageWithPlaceholderData() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/product.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Verify product name
        Elements elements = doc.select(PRODUCT_SELECTOR + ".productFullDetail__productName > span");
        Assert.assertEquals("Product name", elements.first().html());
    }

    @Test
    public void testProductBreadcrumbWithSampleData() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/product.html/chaz-kangeroo-hoodie.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Component Library > Commerce > Outdoor > Collection > Chaz Kangeroo Hoodie
        Elements elements = doc.select(BreadcrumbComponentIT.BREADCRUMB_ITEM_SELECTOR);
        Assert.assertEquals(5, elements.size());
    }

    @Test
    public void testProductBreadcrumbWithPlaceholderData() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/product.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Component Library > Commerce
        Elements elements = doc.select(BreadcrumbComponentIT.BREADCRUMB_ITEM_SELECTOR);
        Assert.assertEquals(2, elements.size());
    }
}
