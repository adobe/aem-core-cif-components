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

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Integration tests for the CIF cache invalidation servlet at {@code /bin/cif/invalidate-cache}.
 *
 * <p>
 * Tests fall into two tiers:
 * <ol>
 * <li><b>Servlet availability</b> — verifies each payload type is accepted (no token needed).</li>
 * <li><b>Full cache workflow</b> — updates Magento data via REST, confirms AEM serves stale cached
 * data, posts an invalidation request, then confirms AEM serves fresh data. Requires both
 * {@code COMMERCE_ENDPOINT} (Magento base URL, e.g. {@code https://mcprod.example.com}) and
 * {@code COMMERCE_INTEGRATION_TOKEN} to be set as system properties.</li>
 * </ol>
 *
 * <p>
 * Prerequisites:
 * <ul>
 * <li>CIF Core Components bundle active ({@code core-cif-components-core})</li>
 * <li>CIF addon installed — provides the {@code /bin/cif/invalidate-cache} servlet</li>
 * <li>{@code InvalidateCacheNotificationImpl} factory config deployed via {@code it/site/ui.config}</li>
 * <li>{@code InvalidateCacheSupport} OSGi config deployed via {@code it/site/ui.config}</li>
 * <li>{@code GraphqlClientImpl~default} config with cache entry for productlist component</li>
 * </ul>
 */
public class CacheInvalidationIT extends ItSiteTestBase {

    private static final String CACHE_INVALIDATION_ENDPOINT = "/bin/cif/invalidate-cache";

    // Test product: VA01 (Dulcea Infinity Scarf) is in the Scarves category
    private static final String TEST_PRODUCT_SKU = "VA01";

    // Test category: Scarves — VA01 belongs to this category; url_path used for page URL
    private static final String TEST_CATEGORY_UID = "MTQ="; // base64("14") — Scarves
    private static final String TEST_CATEGORY_URL_KEY = "venia-scarves";
    private static final String TEST_CATEGORY_URL_PATH = "venia-accessories/venia-scarves";
    private static final int TEST_CATEGORY_ID = 14;

    // Category page that lists VA01 — used by both product and category workflow tests
    private static final String TEST_CATEGORY_PAGE_URL = IT_SITE_ROOT + "/products/category-page.html/" + TEST_CATEGORY_URL_PATH + ".html";

    // COMMERCE_ENDPOINT is the Magento base URL (without /graphql), e.g. https://mcprod.example.com
    // REST writes go to COMMERCE_ENDPOINT/rest/V1 — must point to a writable Magento instance
    private static final String COMMERCE_ENDPOINT = System.getProperty("COMMERCE_ENDPOINT");
    private static final String INTEGRATION_TOKEN = resolveIntegrationToken();

    private static String resolveIntegrationToken() {
        String prop = System.getProperty("COMMERCE_INTEGRATION_TOKEN");
        if (prop != null && !prop.isEmpty()) {
            return prop;
        }
        return System.getenv("COMMERCE_INTEGRATION_TOKEN");
    }

    private static String commerceRestBase() {
        if (COMMERCE_ENDPOINT == null)
            return null;
        String base = COMMERCE_ENDPOINT;
        if (base.endsWith("/graphql")) {
            base = base.substring(0, base.length() - "/graphql".length());
        }
        return base.replaceAll("/+$", "") + "/rest/V1";
    }

    // ---- payload helpers ------------------------------------------------

    private String productSkusPayload(String... skus) {
        StringBuilder sb = new StringBuilder("{\"productSkus\":[");
        for (int i = 0; i < skus.length; i++) {
            if (i > 0)
                sb.append(",");
            sb.append("\"").append(skus[i]).append("\"");
        }
        sb.append("],\"storePath\":\"").append(IT_SITE_ROOT).append("\"}");
        return sb.toString();
    }

    private String categoryUidsPayload(String... uids) {
        StringBuilder sb = new StringBuilder("{\"categoryUids\":[");
        for (int i = 0; i < uids.length; i++) {
            if (i > 0)
                sb.append(",");
            sb.append("\"").append(uids[i]).append("\"");
        }
        sb.append("],\"storePath\":\"").append(IT_SITE_ROOT).append("\"}");
        return sb.toString();
    }

    private String cacheNamesPayload(String... names) {
        StringBuilder sb = new StringBuilder("{\"cacheNames\":[");
        for (int i = 0; i < names.length; i++) {
            if (i > 0)
                sb.append(",");
            sb.append("\"").append(names[i]).append("\"");
        }
        sb.append("],\"storePath\":\"").append(IT_SITE_ROOT).append("\"}");
        return sb.toString();
    }

    private String regexPatternsPayload(String... patterns) {
        StringBuilder sb = new StringBuilder("{\"regexPatterns\":[");
        for (int i = 0; i < patterns.length; i++) {
            if (i > 0)
                sb.append(",");
            sb.append("\"").append(patterns[i]).append("\"");
        }
        sb.append("],\"storePath\":\"").append(IT_SITE_ROOT).append("\"}");
        return sb.toString();
    }

    private String invalidateAllPayload() {
        return "{\"invalidateAll\":true,\"storePath\":\"" + IT_SITE_ROOT + "\"}";
    }

    // ---- page / REST helpers -------------------------------------------

    /**
     * Reads the product name for {@code TEST_PRODUCT_SKU} from the product collection on the
     * category page — matching the pattern used in the Venia reference implementation.
     * The product card is located via the {@code data-product-sku} attribute; the name is read
     * from the title span or, as a fallback, from the data-layer JSON.
     */
    private String getProductNameFromPage() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(TEST_CATEGORY_PAGE_URL, 200);
        Document doc = Jsoup.parse(response.getContent());
        Elements items = doc.select(".productcollection__item[data-product-sku=" + TEST_PRODUCT_SKU + "]");
        if (items.isEmpty()) {
            return null;
        }
        Element item = items.first();
        Elements titleEl = item.select(".productcollection__item-title span");
        if (!titleEl.isEmpty()) {
            return titleEl.first().text().trim();
        }
        String titleAttr = item.attr("title");
        if (titleAttr != null && !titleAttr.isEmpty()) {
            return titleAttr.trim();
        }
        String dataLayer = item.attr("data-cmp-data-layer");
        if (dataLayer != null && !dataLayer.isEmpty()) {
            try {
                JsonNode json = OBJECT_MAPPER.readTree(dataLayer.replace("&quot;", "\""));
                JsonNode firstValue = json.fields().next().getValue();
                if (firstValue.has("dc:title")) {
                    return firstValue.get("dc:title").asText();
                }
            } catch (Exception ignored) {
                // fall through
            }
        }
        return null;
    }

    private String getCategoryNameFromPage() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(TEST_CATEGORY_PAGE_URL, 200);
        Document doc = Jsoup.parse(response.getContent());
        Elements elements = doc.select(".category__title");
        return elements.isEmpty() ? null : elements.first().text();
    }

    private void updateProductName(String sku, String name) throws IOException {
        String url = commerceRestBase() + "/products/" + sku;
        String body = "{\"product\":{\"name\":\"" + name + "\"}}";
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPut request = new HttpPut(url);
            request.setHeader("Authorization", "Bearer " + INTEGRATION_TOKEN);
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
            HttpResponse response = client.execute(request);
            EntityUtils.consume(response.getEntity());
            Assert.assertEquals("Magento product update (PUT /products/" + sku + ") should return 200",
                200, response.getStatusLine().getStatusCode());
        }
    }

    private void updateCategoryName(int categoryId, String name) throws IOException {
        String url = commerceRestBase() + "/categories/" + categoryId;
        String body = "{\"category\":{\"name\":\"" + name + "\"}}";
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPut request = new HttpPut(url);
            request.setHeader("Authorization", "Bearer " + INTEGRATION_TOKEN);
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
            HttpResponse response = client.execute(request);
            EntityUtils.consume(response.getEntity());
            Assert.assertEquals("Magento category update (PUT /categories/" + categoryId + ") should return 200",
                200, response.getStatusLine().getStatusCode());
        }
    }

    // ---- servlet availability tests ------------------------------------

    /**
     * Verifies the cache invalidation servlet is deployed and reachable. Any response other than
     * 404 (200, 400, or 500) confirms the servlet is registered in OSGi and active.
     */
    @Test
    public void testServletReachable() throws Exception {
        SlingHttpResponse response = postJson(CACHE_INVALIDATION_ENDPOINT, invalidateAllPayload(), 200, 400, 500);
        Assert.assertNotEquals("Cache invalidation servlet should be reachable (not 404)",
            404, response.getStatusLine().getStatusCode());
    }

    /**
     * Verifies the servlet accepts a {@code productSkus} payload and returns 200.
     * Does not assert cache behaviour — only confirms the payload type is recognised.
     */
    @Test
    public void testInvalidateByProductSkus() throws Exception {
        SlingHttpResponse response = postJson(CACHE_INVALIDATION_ENDPOINT,
            productSkusPayload(TEST_PRODUCT_SKU), 200);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    }

    /**
     * Verifies the servlet accepts a {@code categoryUids} payload (base64-encoded category IDs)
     * and returns 200. Does not assert cache behaviour.
     */
    @Test
    public void testInvalidateByCategoryUids() throws Exception {
        SlingHttpResponse response = postJson(CACHE_INVALIDATION_ENDPOINT,
            categoryUidsPayload(TEST_CATEGORY_UID), 200);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    }

    /**
     * Verifies the servlet accepts a {@code cacheNames} payload — a list of OSGi component
     * resource-type cache bucket names — and returns 200. Does not assert cache behaviour.
     */
    @Test
    public void testInvalidateByCacheNames() throws Exception {
        SlingHttpResponse response = postJson(CACHE_INVALIDATION_ENDPOINT,
            cacheNamesPayload(
                "cif-components-it-site/components/commerce/productlist",
                "cif-components-it-site/components/commerce/navigation"),
            200);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    }

    /**
     * Verifies the servlet accepts a {@code regexPatterns} payload — regular expressions matched
     * against cached GraphQL response JSON — and returns 200. Does not assert cache behaviour.
     */
    @Test
    public void testInvalidateByRegexPatterns() throws Exception {
        SlingHttpResponse response = postJson(CACHE_INVALIDATION_ENDPOINT,
            regexPatternsPayload("\\\"sku\\\":\\\\s*\\\"" + TEST_PRODUCT_SKU + "\\\""),
            200);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    }

    /**
     * Verifies the servlet accepts an {@code invalidateAll} payload — clears every cache bucket
     * for the store — and returns 200. Does not assert cache behaviour.
     */
    @Test
    public void testInvalidateAll() throws Exception {
        SlingHttpResponse response = postJson(CACHE_INVALIDATION_ENDPOINT,
            invalidateAllPayload(), 200);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    }

    // ---- workflow tests ------------------------------------------------

    /**
     * Full end-to-end cache invalidation workflow triggered by product SKU.
     * <ol>
     *   <li>Fetches the Scarves category page and reads VA01's current name — warms the cache.</li>
     *   <li>Updates the product name in Magento via REST to a unique test value.</li>
     *   <li>Fetches the page again — asserts AEM still serves the <em>old</em> name (cache is holding).</li>
     *   <li>POSTs {@code productSkus:["VA01"]} to the invalidation servlet.</li>
     *   <li>Fetches the page again — asserts AEM now serves the <em>new</em> name (cache was cleared).</li>
     *   <li>Restores the original name in Magento and clears the cache in the finally block.</li>
     * </ol>
     */
    @Test
    public void testProductCacheInvalidationWorkflow() throws Exception {
        String originalName = getProductNameFromPage();
        Assert.assertNotNull("Category page should render product VA01 with a name", originalName);

        String testName = "CIF-IT-" + System.currentTimeMillis();
        updateProductName(TEST_PRODUCT_SKU, testName);
        try {
            String cachedName = getProductNameFromPage();
            Assert.assertEquals("AEM should serve stale cached name before invalidation",
                originalName, cachedName);

            postJson(CACHE_INVALIDATION_ENDPOINT, productSkusPayload(TEST_PRODUCT_SKU), 200);

            String freshName = getProductNameFromPage();
            Assert.assertEquals("AEM should serve updated name after cache invalidation",
                testName, freshName);

        } finally {
            updateProductName(TEST_PRODUCT_SKU, originalName);
            postJson(CACHE_INVALIDATION_ENDPOINT, productSkusPayload(TEST_PRODUCT_SKU), 200);
        }
    }

    /**
     * Full end-to-end cache invalidation workflow triggered by category UID.
     * <ol>
     *   <li>Fetches the Scarves category page and reads the category title — warms the cache.</li>
     *   <li>Updates the category name in Magento via REST to a unique test value.</li>
     *   <li>Fetches the page again — asserts AEM still serves the <em>old</em> name (cache is holding).</li>
     *   <li>POSTs {@code categoryUids:["MTQ="]} to the invalidation servlet.</li>
     *   <li>Fetches the page again — asserts AEM now serves the <em>new</em> name (cache was cleared).</li>
     *   <li>Restores the original name in Magento and clears the cache in the finally block.</li>
     * </ol>
     */
    @Test
    public void testCategoryUidCacheInvalidationWorkflow() throws Exception {
        String originalName = getCategoryNameFromPage();
        Assert.assertNotNull("Category page should render a category name", originalName);

        String testName = "CIF-IT-Cat-" + System.currentTimeMillis();
        updateCategoryName(TEST_CATEGORY_ID, testName);
        try {
            String cachedName = getCategoryNameFromPage();
            Assert.assertEquals("AEM should serve stale cached category name before invalidation",
                originalName, cachedName);

            postJson(CACHE_INVALIDATION_ENDPOINT, categoryUidsPayload(TEST_CATEGORY_UID), 200);

            String freshName = getCategoryNameFromPage();
            Assert.assertEquals("AEM should serve updated category name after invalidation",
                testName, freshName);

        } finally {
            updateCategoryName(TEST_CATEGORY_ID, originalName);
            postJson(CACHE_INVALIDATION_ENDPOINT, categoryUidsPayload(TEST_CATEGORY_UID), 200);
        }
    }

    /**
     * Full end-to-end cache invalidation workflow triggered by cache name.
     * <ol>
     *   <li>Fetches the Scarves category page and reads VA01's current name — warms the cache.</li>
     *   <li>Updates the product name in Magento via REST to a unique test value.</li>
     *   <li>Fetches the page again — asserts AEM still serves the <em>old</em> name (cache is holding).</li>
     *   <li>POSTs {@code cacheNames:["cif-components-it-site/components/commerce/productlist"]} to the invalidation servlet.</li>
     *   <li>Fetches the page again — asserts AEM now serves the <em>new</em> name (cache was cleared).</li>
     *   <li>Restores the original name in Magento and clears the cache in the finally block.</li>
     * </ol>
     */
    @Test
    public void testCacheNameInvalidationClearsProductCache() throws Exception {
        String originalName = getProductNameFromPage();
        Assert.assertNotNull("Category page should render product VA01 with a name", originalName);

        String testName = "CIF-IT-CN-" + System.currentTimeMillis();
        updateProductName(TEST_PRODUCT_SKU, testName);
        try {
            String cachedName = getProductNameFromPage();
            Assert.assertEquals("AEM should serve stale cached name before cache-name invalidation",
                originalName, cachedName);

            postJson(CACHE_INVALIDATION_ENDPOINT,
                cacheNamesPayload("cif-components-it-site/components/commerce/productlist"), 200);

            String freshName = getProductNameFromPage();
            Assert.assertEquals("AEM should serve updated name after cache-name invalidation",
                testName, freshName);

        } finally {
            updateProductName(TEST_PRODUCT_SKU, originalName);
            postJson(CACHE_INVALIDATION_ENDPOINT, productSkusPayload(TEST_PRODUCT_SKU), 200);
        }
    }

    /**
     * Full end-to-end cache invalidation workflow using {@code invalidateAll}.
     * <ol>
     *   <li>Fetches the Scarves category page and reads VA01's current name — warms the cache.</li>
     *   <li>Updates the product name in Magento via REST to a unique test value.</li>
     *   <li>Fetches the page again — asserts AEM still serves the <em>old</em> name (cache is holding).</li>
     *   <li>POSTs {@code invalidateAll:true} to clear every cache bucket for the store.</li>
     *   <li>Fetches the page again — asserts AEM now serves the <em>new</em> name (cache was cleared).</li>
     *   <li>Restores the original name in Magento and clears the cache in the finally block.</li>
     * </ol>
     */
    @Test
    public void testInvalidateAllClearsProductCache() throws Exception {
        String originalName = getProductNameFromPage();
        Assert.assertNotNull("Category page should render product VA01 with a name", originalName);

        String testName = "CIF-IT-All-" + System.currentTimeMillis();
        updateProductName(TEST_PRODUCT_SKU, testName);
        try {
            String cachedName = getProductNameFromPage();
            Assert.assertEquals("AEM should serve stale cached name before invalidateAll",
                originalName, cachedName);

            postJson(CACHE_INVALIDATION_ENDPOINT, invalidateAllPayload(), 200);

            String freshName = getProductNameFromPage();
            Assert.assertEquals("AEM should serve updated name after invalidateAll",
                testName, freshName);

        } finally {
            updateProductName(TEST_PRODUCT_SKU, originalName);
            postJson(CACHE_INVALIDATION_ENDPOINT, invalidateAllPayload(), 200);
        }
    }
}
