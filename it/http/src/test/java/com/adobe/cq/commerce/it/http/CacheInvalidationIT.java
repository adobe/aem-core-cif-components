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
import org.junit.experimental.categories.Category;

import com.fasterxml.jackson.databind.JsonNode;
import junit.category.IgnoreOn65;
import junit.category.IgnoreOnCloud;
import junit.category.IgnoreOnLts;

/**
 * Integration tests for the CIF cache invalidation servlet at {@code /bin/cif/invalidate-cache}.
 *
 * <p>
 * Tests fall into two tiers:
 * <ol>
 * <li><b>Servlet availability</b> — verifies each payload type is accepted (no Magento writes).</li>
 * <li><b>Full cache workflow</b> — updates Magento data via REST, confirms AEM serves stale cached
 * data, posts an invalidation request, then confirms AEM serves fresh data on both the category
 * listing and the product detail page. Requires {@code COMMERCE_ENDPOINT} (Magento base URL) and
 * {@code COMMERCE_INTEGRATION_TOKEN} to be set as system properties.</li>
 * </ol>
 *
 * <p>
 * Workflow tests run against three independent product/category pairs — one per AEM target
 * (Classic/6.5, LTS, Cloud) — selected by JUnit category. This mirrors the Venia reference
 * implementation and prevents cross-environment Magento state conflicts when tests run
 * back-to-back against a shared Magento backend.
 *
 * <p>
 * Prerequisites:
 * <ul>
 * <li>CIF Core Components bundle active ({@code core-cif-components-core})</li>
 * <li>CIF addon installed — provides the {@code /bin/cif/invalidate-cache} servlet</li>
 * <li>{@code InvalidateCacheNotificationImpl} factory config deployed via {@code it/site/ui.config}</li>
 * <li>{@code InvalidateCacheSupport} OSGi config deployed via {@code it/site/ui.config}</li>
 * <li>{@code GraphqlClientImpl~default} config with cache entries for productlist and product components</li>
 * </ul>
 */
public class CacheInvalidationIT extends ItSiteTestBase {

    private static final String CACHE_INVALIDATION_ENDPOINT = "/bin/cif/invalidate-cache";

    // ---- per-environment test data --------------------------------------

    /**
     * Immutable holder for a single environment's product + category test fixture.
     * Each fixture targets a different Magento product/category so concurrent or back-to-back
     * test runs against a shared Magento backend do not corrupt each other's state.
     */
    private static final class TestData {
        final String productSku;
        final String originalProductName;
        final String categoryUid;
        final int categoryId;
        final String categoryUrlPath;
        final String originalCategoryName;
        final String categoryPageUrl;

        TestData(String productSku, String originalProductName,
                 String categoryUid, int categoryId, String categoryUrlPath, String originalCategoryName) {
            this.productSku = productSku;
            this.originalProductName = originalProductName;
            this.categoryUid = categoryUid;
            this.categoryId = categoryId;
            this.categoryUrlPath = categoryUrlPath;
            this.originalCategoryName = originalCategoryName;
            this.categoryPageUrl = IT_SITE_ROOT + "/products/category-page.html/" + categoryUrlPath + ".html";
        }
    }

    // Each fixture uses a 2-level category url_path so the leaf category name appears in the
    // PDP breadcrumb (the IT site's breadcrumb component uses structureDepth=2).
    // The PDP URL is discovered at runtime from the product card href on the category page
    // — see discoverPdpUrl() — so the test works regardless of how each AEM instance is
    // configured to build product URLs.

    // Classic / AEM 6.5 — Blouses & Shirts
    private static final TestData CLASSIC = new TestData(
        "VT01", "Penelope Peasant Blouse",
        "MjM=", 23, "venia-tops/venia-blouses", "Blouses & Shirts");

    // LTS — Pants & Shorts
    private static final TestData LTS = new TestData(
        "VP01", "Selena Pants",
        "MzI=", 32, "venia-bottoms/venia-pants", "Pants & Shorts");

    // Cloud — Scarves
    private static final TestData CLOUD = new TestData(
        "VA01", "Dulcea Infinity Scarf",
        "MTQ=", 14, "venia-accessories/venia-scarves", "Scarves");

    // ---- Magento REST connection ----------------------------------------

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
        if (COMMERCE_ENDPOINT == null) {
            return null;
        }
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

    // ---- page / REST helpers --------------------------------------------

    /**
     * Reads the product name for {@code data.productSku} from the product collection on the
     * category page. Tries the title span first, then the {@code title} attribute, then the
     * data-layer JSON as a final fallback.
     */
    private String getProductNameFromCategoryPage(TestData data) throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(data.categoryPageUrl, 200);
        Document doc = Jsoup.parse(response.getContent());
        Elements items = doc.select(".productcollection__item[data-product-sku=" + data.productSku + "]");
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

    private String getCategoryNameFromPage(TestData data) throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(data.categoryPageUrl, 200);
        Document doc = Jsoup.parse(response.getContent());
        Elements elements = doc.select(".category__title");
        return elements.isEmpty() ? null : elements.first().text();
    }

    /**
     * Reads the product name from the PDP using the same selector as {@link ProductComponentIT},
     * {@code .productFullDetail__productName > span}.
     */
    /**
     * Discovers the actual PDP URL this AEM instance expects for the given SKU by reading
     * the product card's {@code href} on the category page. This sidesteps URL-format
     * differences between AEM instances (e.g. context-aware vs leaf-only product URLs)
     * because the category page itself emits whichever URL its URL provider is configured
     * to produce. Returns the URL with {@code ?wcmmode=disabled} appended so the PDP
     * renders in publish mode (avoids the "Product name" i18n placeholder in author mode).
     *
     * <p>
     * TODO: Investigate why the pipeline AEM emits the short-form URL
     * ({@code /product-page.html/<url_key>.html}) for product cards instead of the full
     * context-aware form ({@code /product-page.html/<category_url_path>/<url_key>.html})
     * that the local AEM produces. With {@code enableContextAwareProductUrls=true} in
     * {@code UrlProviderImpl.cfg.json}, the full form is expected. Either the OSGi config
     * isn't being applied on the pipeline, or the URL provider on that AEM version uses
     * different formatting logic. Once that is resolved, the discovery step here can be
     * dropped and the PDP URL hardcoded again.
     */
    private String discoverPdpUrl(TestData data) throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(data.categoryPageUrl, 200);
        Document doc = Jsoup.parse(response.getContent());
        Elements items = doc.select(".productcollection__item[data-product-sku=" + data.productSku + "]");
        if (items.isEmpty()) {
            throw new AssertionError("Cannot derive PDP URL: product card for SKU "
                + data.productSku + " not found on " + data.categoryPageUrl);
        }
        String href = items.first().attr("href");
        if (href == null || href.isEmpty()) {
            throw new AssertionError("Cannot derive PDP URL: product card for SKU "
                + data.productSku + " has no href attribute");
        }
        return href + (href.contains("?") ? "&" : "?") + "wcmmode=disabled";
    }

    private String getProductNameFromPdp(TestData data) throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(discoverPdpUrl(data), 200);
        Document doc = Jsoup.parse(response.getContent());
        Elements nameEl = doc.select(".productFullDetail__productName > span");
        return nameEl.isEmpty() ? null : nameEl.first().text().trim();
    }

    /**
     * Verifies the PDP actually resolves to a real product (not the {@code "Product name"}
     * i18n placeholder that AEM renders when the product context isn't loaded). Fails the
     * test fast with a setup-vs-cache-invalidation disambiguating message so future
     * failures aren't misdiagnosed as cache-invalidation problems.
     */
    private void assertPdpResolves(TestData data) throws ClientException {
        String name = getProductNameFromPdp(data);
        Assert.assertNotEquals(
            "PDP for SKU " + data.productSku + " did not resolve the product — got the "
                + "'Product name' i18n placeholder. This is an AEM URL-routing / WCM-mode "
                + "issue, not a cache invalidation failure.",
            "Product name", name);
    }

    /**
     * Concatenates every breadcrumb item on the PDP. Used to verify the leaf category name
     * appears in the product page breadcrumb after a category cache invalidation. Works only
     * when {@code data.categoryUrlPath} is 2 segments deep (the IT site breadcrumb is
     * configured with {@code structureDepth=2} and skips deeper leaves).
     */
    private String getPdpBreadcrumbText(TestData data) throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(discoverPdpUrl(data), 200);
        Document doc = Jsoup.parse(response.getContent());
        Elements items = doc.select(".cmp-breadcrumb__item");
        StringBuilder sb = new StringBuilder();
        for (Element item : items) {
            if (sb.length() > 0)
                sb.append(" | ");
            sb.append(item.text().trim());
        }
        return sb.toString();
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

    // ============================================================================================
    // SERVLET AVAILABILITY TESTS — single set, no Magento writes
    // ============================================================================================

    /** Servlet is reachable — any non-404 response confirms it is registered. */
    @Test
    public void testServletReachable() throws Exception {
        SlingHttpResponse response = postJson(CACHE_INVALIDATION_ENDPOINT, invalidateAllPayload(), 200, 400, 500);
        Assert.assertNotEquals("Cache invalidation servlet should be reachable (not 404)",
            404, response.getStatusLine().getStatusCode());
    }

    /** Servlet accepts the {@code productSkus} payload. */
    @Test
    public void testInvalidateByProductSkus() throws Exception {
        SlingHttpResponse response = postJson(CACHE_INVALIDATION_ENDPOINT,
            productSkusPayload(CLASSIC.productSku), 200);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    }

    /** Servlet accepts the {@code categoryUids} payload. */
    @Test
    public void testInvalidateByCategoryUids() throws Exception {
        SlingHttpResponse response = postJson(CACHE_INVALIDATION_ENDPOINT,
            categoryUidsPayload(CLASSIC.categoryUid), 200);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    }

    /** Servlet accepts the {@code cacheNames} payload. */
    @Test
    public void testInvalidateByCacheNames() throws Exception {
        SlingHttpResponse response = postJson(CACHE_INVALIDATION_ENDPOINT,
            cacheNamesPayload(
                "cif-components-it-site/components/commerce/productlist",
                "cif-components-it-site/components/commerce/navigation"),
            200);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    }

    /** Servlet accepts the {@code regexPatterns} payload. */
    @Test
    public void testInvalidateByRegexPatterns() throws Exception {
        SlingHttpResponse response = postJson(CACHE_INVALIDATION_ENDPOINT,
            regexPatternsPayload("\\\"sku\\\":\\\\s*\\\"" + CLASSIC.productSku + "\\\""), 200);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    }

    /** Servlet accepts the {@code invalidateAll} payload. */
    @Test
    public void testInvalidateAll() throws Exception {
        SlingHttpResponse response = postJson(CACHE_INVALIDATION_ENDPOINT, invalidateAllPayload(), 200);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    }

    // ============================================================================================
    // WORKFLOW HELPERS — five workflows, each parameterised by a TestData fixture
    // ============================================================================================

    private void runProductSkusWorkflow(TestData data) throws Exception {
        assertPdpResolves(data);
        String originalNameOnCategory = getProductNameFromCategoryPage(data);
        Assert.assertNotNull("Category page should render product " + data.productSku, originalNameOnCategory);
        String originalNameOnPdp = getProductNameFromPdp(data);
        Assert.assertNotNull("PDP should render " + data.productSku + " with a name", originalNameOnPdp);

        String testName = "CIF-IT-" + data.productSku + "-" + System.currentTimeMillis();
        updateProductName(data.productSku, testName);
        try {
            Assert.assertEquals("Category listing should serve stale cached name before invalidation",
                originalNameOnCategory, getProductNameFromCategoryPage(data));
            Assert.assertEquals("PDP should serve stale cached name before invalidation",
                originalNameOnPdp, getProductNameFromPdp(data));

            postJson(CACHE_INVALIDATION_ENDPOINT, productSkusPayload(data.productSku), 200);

            Assert.assertEquals("Category listing should serve updated name after productSkus invalidation",
                testName, getProductNameFromCategoryPage(data));
            Assert.assertEquals("PDP should serve updated name after productSkus invalidation",
                testName, getProductNameFromPdp(data));

        } finally {
            updateProductName(data.productSku, data.originalProductName);
            postJson(CACHE_INVALIDATION_ENDPOINT, productSkusPayload(data.productSku), 200);
        }
    }

    private void runCategoryUidsWorkflow(TestData data) throws Exception {
        assertPdpResolves(data);
        String originalCategoryName = getCategoryNameFromPage(data);
        Assert.assertNotNull("Category page should render a category name", originalCategoryName);
        Assert.assertTrue("PDP breadcrumb should initially contain category '" + originalCategoryName + "'",
            getPdpBreadcrumbText(data).contains(originalCategoryName));

        String testName = "CIF-IT-Cat-" + data.categoryId + "-" + System.currentTimeMillis();
        updateCategoryName(data.categoryId, testName);
        try {
            Assert.assertEquals("Category title should serve stale cached name before invalidation",
                originalCategoryName, getCategoryNameFromPage(data));
            Assert.assertTrue("PDP breadcrumb should still contain stale category name before invalidation",
                getPdpBreadcrumbText(data).contains(originalCategoryName));

            postJson(CACHE_INVALIDATION_ENDPOINT, categoryUidsPayload(data.categoryUid), 200);

            Assert.assertEquals("Category title should be updated after categoryUids invalidation",
                testName, getCategoryNameFromPage(data));
            Assert.assertTrue("PDP breadcrumb should contain updated category name after invalidation",
                getPdpBreadcrumbText(data).contains(testName));

        } finally {
            updateCategoryName(data.categoryId, data.originalCategoryName);
            postJson(CACHE_INVALIDATION_ENDPOINT, categoryUidsPayload(data.categoryUid), 200);
        }
    }

    private void runCacheNamesWorkflow(TestData data) throws Exception {
        assertPdpResolves(data);
        String originalNameOnCategory = getProductNameFromCategoryPage(data);
        Assert.assertNotNull("Category page should render product " + data.productSku, originalNameOnCategory);
        String originalNameOnPdp = getProductNameFromPdp(data);
        Assert.assertNotNull("PDP should render " + data.productSku + " with a name", originalNameOnPdp);

        String testName = "CIF-IT-CN-" + data.productSku + "-" + System.currentTimeMillis();
        updateProductName(data.productSku, testName);
        try {
            Assert.assertEquals("Category listing should serve stale cached name before cache-name invalidation",
                originalNameOnCategory, getProductNameFromCategoryPage(data));
            Assert.assertEquals("PDP should serve stale cached name before cache-name invalidation",
                originalNameOnPdp, getProductNameFromPdp(data));

            // Invalidate both productlist (category listing) and product (PDP) buckets.
            postJson(CACHE_INVALIDATION_ENDPOINT,
                cacheNamesPayload(
                    "cif-components-it-site/components/commerce/productlist",
                    "cif-components-it-site/components/commerce/product"),
                200);

            Assert.assertEquals("Category listing should serve updated name after cache-name invalidation",
                testName, getProductNameFromCategoryPage(data));
            Assert.assertEquals("PDP should serve updated name after cache-name invalidation",
                testName, getProductNameFromPdp(data));

        } finally {
            updateProductName(data.productSku, data.originalProductName);
            postJson(CACHE_INVALIDATION_ENDPOINT, productSkusPayload(data.productSku), 200);
        }
    }

    private void runInvalidateAllWorkflow(TestData data) throws Exception {
        assertPdpResolves(data);
        String originalProductOnCategory = getProductNameFromCategoryPage(data);
        Assert.assertNotNull("Category page should render product " + data.productSku, originalProductOnCategory);
        String originalProductOnPdp = getProductNameFromPdp(data);
        Assert.assertNotNull("PDP should render " + data.productSku + " with a name", originalProductOnPdp);
        String originalCategoryName = getCategoryNameFromPage(data);
        Assert.assertNotNull("Category page should render a category name", originalCategoryName);
        Assert.assertTrue("PDP breadcrumb should initially contain category '" + originalCategoryName + "'",
            getPdpBreadcrumbText(data).contains(originalCategoryName));

        String testProductName = "CIF-IT-AllP-" + data.productSku + "-" + System.currentTimeMillis();
        String testCategoryName = "CIF-IT-AllC-" + data.categoryId + "-" + System.currentTimeMillis();
        updateProductName(data.productSku, testProductName);
        updateCategoryName(data.categoryId, testCategoryName);
        try {
            Assert.assertEquals("Category listing should serve stale product name before invalidateAll",
                originalProductOnCategory, getProductNameFromCategoryPage(data));
            Assert.assertEquals("PDP should serve stale product name before invalidateAll",
                originalProductOnPdp, getProductNameFromPdp(data));
            Assert.assertEquals("Category title should be stale before invalidateAll",
                originalCategoryName, getCategoryNameFromPage(data));
            Assert.assertTrue("PDP breadcrumb should still contain stale category name before invalidateAll",
                getPdpBreadcrumbText(data).contains(originalCategoryName));

            postJson(CACHE_INVALIDATION_ENDPOINT, invalidateAllPayload(), 200);

            Assert.assertEquals("Category listing should serve updated product name after invalidateAll",
                testProductName, getProductNameFromCategoryPage(data));
            Assert.assertEquals("PDP should serve updated product name after invalidateAll",
                testProductName, getProductNameFromPdp(data));
            Assert.assertEquals("Category title should be updated after invalidateAll",
                testCategoryName, getCategoryNameFromPage(data));
            Assert.assertTrue("PDP breadcrumb should contain updated category name after invalidateAll",
                getPdpBreadcrumbText(data).contains(testCategoryName));

        } finally {
            updateProductName(data.productSku, data.originalProductName);
            updateCategoryName(data.categoryId, data.originalCategoryName);
            postJson(CACHE_INVALIDATION_ENDPOINT, invalidateAllPayload(), 200);
        }
    }

    private void runRegexPatternsWorkflow(TestData data) throws Exception {
        assertPdpResolves(data);
        String originalNameOnCategory = getProductNameFromCategoryPage(data);
        Assert.assertNotNull("Category page should render product " + data.productSku, originalNameOnCategory);
        String originalNameOnPdp = getProductNameFromPdp(data);
        Assert.assertNotNull("PDP should render " + data.productSku + " with a name", originalNameOnPdp);

        String testName = "CIF-IT-RX-" + data.productSku + "-" + System.currentTimeMillis();
        updateProductName(data.productSku, testName);
        try {
            Assert.assertEquals("Category listing should serve stale cached name before regex invalidation",
                originalNameOnCategory, getProductNameFromCategoryPage(data));
            Assert.assertEquals("PDP should serve stale cached name before regex invalidation",
                originalNameOnPdp, getProductNameFromPdp(data));

            // Regex matches any cached GraphQL JSON containing the product SKU.
            postJson(CACHE_INVALIDATION_ENDPOINT,
                regexPatternsPayload("\\\"sku\\\":\\\\s*\\\"" + data.productSku + "\\\""), 200);

            Assert.assertEquals("Category listing should serve updated name after regex invalidation",
                testName, getProductNameFromCategoryPage(data));
            Assert.assertEquals("PDP should serve updated name after regex invalidation",
                testName, getProductNameFromPdp(data));

        } finally {
            updateProductName(data.productSku, data.originalProductName);
            postJson(CACHE_INVALIDATION_ENDPOINT, productSkusPayload(data.productSku), 200);
        }
    }

    // ============================================================================================
    // CLASSIC / AEM 6.5 — Leather Belts (BLT-LEA-001)
    // ============================================================================================

    @Test
    @Category({ IgnoreOnCloud.class, IgnoreOnLts.class })
    public void test65_ProductSkusWorkflow() throws Exception {
        runProductSkusWorkflow(CLASSIC);
    }

    @Test
    @Category({ IgnoreOnCloud.class, IgnoreOnLts.class })
    public void test65_CategoryUidsWorkflow() throws Exception {
        runCategoryUidsWorkflow(CLASSIC);
    }

    @Test
    @Category({ IgnoreOnCloud.class, IgnoreOnLts.class })
    public void test65_CacheNamesWorkflow() throws Exception {
        runCacheNamesWorkflow(CLASSIC);
    }

    @Test
    @Category({ IgnoreOnCloud.class, IgnoreOnLts.class })
    public void test65_InvalidateAllWorkflow() throws Exception {
        runInvalidateAllWorkflow(CLASSIC);
    }

    @Test
    @Category({ IgnoreOnCloud.class, IgnoreOnLts.class })
    public void test65_RegexPatternsWorkflow() throws Exception {
        runRegexPatternsWorkflow(CLASSIC);
    }

    // ============================================================================================
    // LTS — Metal Belts (BLT-MET-001)
    // ============================================================================================

    @Test
    @Category({ IgnoreOn65.class, IgnoreOnCloud.class })
    public void testLts_ProductSkusWorkflow() throws Exception {
        runProductSkusWorkflow(LTS);
    }

    @Test
    @Category({ IgnoreOn65.class, IgnoreOnCloud.class })
    public void testLts_CategoryUidsWorkflow() throws Exception {
        runCategoryUidsWorkflow(LTS);
    }

    @Test
    @Category({ IgnoreOn65.class, IgnoreOnCloud.class })
    public void testLts_CacheNamesWorkflow() throws Exception {
        runCacheNamesWorkflow(LTS);
    }

    @Test
    @Category({ IgnoreOn65.class, IgnoreOnCloud.class })
    public void testLts_InvalidateAllWorkflow() throws Exception {
        runInvalidateAllWorkflow(LTS);
    }

    @Test
    @Category({ IgnoreOn65.class, IgnoreOnCloud.class })
    public void testLts_RegexPatternsWorkflow() throws Exception {
        runRegexPatternsWorkflow(LTS);
    }

    // ============================================================================================
    // CLOUD — Fabric Belts (BLT-FAB-001)
    // ============================================================================================

    @Test
    @Category({ IgnoreOn65.class, IgnoreOnLts.class })
    public void testCloud_ProductSkusWorkflow() throws Exception {
        runProductSkusWorkflow(CLOUD);
    }

    @Test
    @Category({ IgnoreOn65.class, IgnoreOnLts.class })
    public void testCloud_CategoryUidsWorkflow() throws Exception {
        runCategoryUidsWorkflow(CLOUD);
    }

    @Test
    @Category({ IgnoreOn65.class, IgnoreOnLts.class })
    public void testCloud_CacheNamesWorkflow() throws Exception {
        runCacheNamesWorkflow(CLOUD);
    }

    @Test
    @Category({ IgnoreOn65.class, IgnoreOnLts.class })
    public void testCloud_InvalidateAllWorkflow() throws Exception {
        runInvalidateAllWorkflow(CLOUD);
    }

    @Test
    @Category({ IgnoreOn65.class, IgnoreOnLts.class })
    public void testCloud_RegexPatternsWorkflow() throws Exception {
        runRegexPatternsWorkflow(CLOUD);
    }
}
