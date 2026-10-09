/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.osgi.OsgiConsoleClient;
import org.apache.sling.testing.clients.util.poller.Polling;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.apache.http.HttpStatus.SC_MOVED_TEMPORARILY;

/**
 * Integration tests for SITES-40219: the {@code CatalogPageNotFoundFilter} must return a 404 for a non-existing product
 * or category also when the {@code SpecificPageFilterFactory} forwards the request to a specific product or category
 * page (a child page of the catalog page with a matching {@code selectorFilter}).
 *
 * The IT site contains two specific pages whose {@code selectorFilter} matches an identifier that does not exist in the
 * commerce backend:
 * <ul>
 * <li>{@code products/product-page/specific-product-page} for the url key {@value #MISSING_PRODUCT}</li>
 * <li>{@code products/category-page/specific-category-page} for the url path {@value #MISSING_CATEGORY}</li>
 * </ul>
 * Each specific page renders a unique title, which proves that the request was forwarded to it. In author/preview mode
 * placeholder data is rendered, so the page returns 200. With {@code wcmmode=disabled} the filter must return 404.
 */
public class SpecificCatalogPageNotFoundIT extends ItSiteTestBase {

    private static final String SPECIFIC_PAGE_STRATEGY_PID = "com.adobe.cq.commerce.core.components.internal.services.SpecificPageStrategy";

    private static final String MISSING_PRODUCT = "cif-it-specific-page-missing-product";
    private static final String MISSING_CATEGORY = "cif-it-specific-page-missing-category";

    private static final String PRODUCT_PAGE_URL = IT_SITE_ROOT + "/products/product-page.html/" + MISSING_PRODUCT
        + ".html";
    private static final String CATEGORY_PAGE_URL = IT_SITE_ROOT + "/products/category-page.html/" + MISSING_CATEGORY
        + ".html";

    private static final String SPECIFIC_PRODUCT_PAGE_TITLE = "CIF IT specific product page";
    private static final String SPECIFIC_CATEGORY_PAGE_TITLE = "CIF IT specific category page";

    private static final String TITLE_SELECTOR = ".cmp-title__text";

    private static String originalGenerateSpecificPageUrls;

    @BeforeClass
    public static void enableSpecificPageForwarding() throws ClientException, InterruptedException, TimeoutException {
        OsgiConsoleClient osgiClient = adminAuthor.adaptTo(OsgiConsoleClient.class);
        Map<String, Object> configuration = osgiClient.getConfiguration(SPECIFIC_PAGE_STRATEGY_PID);
        Object originalValue = configuration.get("generateSpecificPageUrls");
        originalGenerateSpecificPageUrls = originalValue == null ? null : String.valueOf(originalValue);
        updateSpecificPageStrategy("false");
        waitForSpecificPageRendering(true);
    }

    @AfterClass
    public static void restoreSpecificPageStrategy() throws ClientException, InterruptedException, TimeoutException {
        if (originalGenerateSpecificPageUrls != null) {
            updateSpecificPageStrategy(originalGenerateSpecificPageUrls);
            waitForSpecificPageRendering(!Boolean.parseBoolean(originalGenerateSpecificPageUrls));
        }
    }

    @Test
    public void testSpecificProductPageReturns404ForMissingProduct() throws ClientException {
        assertSpecificPageRendered(PRODUCT_PAGE_URL, SPECIFIC_PRODUCT_PAGE_TITLE);
        adminAuthor.doGet(PRODUCT_PAGE_URL + "?wcmmode=disabled", 404);
    }

    @Test
    public void testSpecificCategoryPageReturns404ForMissingCategory() throws ClientException {
        assertSpecificPageRendered(CATEGORY_PAGE_URL, SPECIFIC_CATEGORY_PAGE_TITLE);
        adminAuthor.doGet(CATEGORY_PAGE_URL + "?wcmmode=disabled", 404);
    }

    private void assertSpecificPageRendered(String url, String expectedTitle) throws ClientException {
        String previewUrl = url + "?wcmmode=preview";
        SlingHttpResponse response = adminAuthor.doGet(previewUrl, 200);
        String responseContent = response.getContent();
        Document doc = Jsoup.parse(responseContent);
        List<String> titleTexts = doc.select(TITLE_SELECTOR).stream().map(Element::text).collect(Collectors.toList());
        int markerCount = doc.select(TITLE_SELECTOR + ":containsOwn(" + expectedTitle + ")").size();

        Assert.assertEquals("Request should be forwarded to the specific page " + url + "; expectedTitle="
            + expectedTitle + "; documentTitle=" + doc.title() + "; observedTitleTexts=" + titleTexts, 1,
            markerCount);
    }

    private static void updateSpecificPageStrategy(String desiredValue)
        throws ClientException, InterruptedException, TimeoutException {
        OsgiConsoleClient osgiClient = adminAuthor.adaptTo(OsgiConsoleClient.class);
        osgiClient.waitEditConfiguration(30, SPECIFIC_PAGE_STRATEGY_PID, null,
            Collections.<String, Object>singletonMap("generateSpecificPageUrls", desiredValue),
            SC_MOVED_TEMPORARILY);
        Map<String, Object> configuration = osgiClient.getConfiguration(SPECIFIC_PAGE_STRATEGY_PID);
        Object actualValue = configuration.get("generateSpecificPageUrls");
        Assert.assertEquals("OSGi configuration was not applied for pid " + SPECIFIC_PAGE_STRATEGY_PID, desiredValue,
            String.valueOf(actualValue));
    }

    private static void waitForSpecificPageRendering(boolean expected)
        throws InterruptedException, TimeoutException {
        Polling polling = new Polling(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                try {
                    SlingHttpResponse response = adminAuthor.doGet(PRODUCT_PAGE_URL + "?wcmmode=preview", 200);
                    Document doc = Jsoup.parse(response.getContent());
                    boolean rendered = doc.select(TITLE_SELECTOR + ":containsOwn(" + SPECIFIC_PRODUCT_PAGE_TITLE + ")")
                        .size() == 1;
                    return rendered == expected;
                } catch (ClientException e) {
                    return false;
                }
            }
        });
        polling.poll(30000, 1000);
    }
}
