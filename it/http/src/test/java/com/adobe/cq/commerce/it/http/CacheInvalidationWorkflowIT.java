/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2024 Adobe
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
 ~ See the License for the specific language governing permissions
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.cq.commerce.it.http;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import junit.category.IgnoreOn65;
import junit.category.IgnoreOnCloud;

import static org.junit.Assert.assertTrue;

import java.util.Random;

/**
 * Simple Cache Invalidation Test - Tests both product and category cache invalidation
 */
public class CacheInvalidationWorkflowIT extends CommerceTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(CacheInvalidationWorkflowIT.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Magento Configuration
    private static final String MAGENTO_BASE_URL = "https://mcprod.catalogservice-commerce.fun";
    private static final String MAGENTO_REST_URL = MAGENTO_BASE_URL + "/rest/V1";
    private static final String MAGENTO_ADMIN_TOKEN = "etk0tf7974shom72dyphbxqxsqd2eqe5";
    private static final String CACHE_INVALIDATION_ENDPOINT = "/bin/cif/invalidate-cache";
    private static final String STORE_PATH = "/content/venia/us/en";

    private CloseableHttpClient httpClient;

    // Store original names for cleanup
    private String lastProductSku = null;
    private String lastOriginalProductName = null;
    private String lastCategoryId = null;
    private String lastOriginalCategoryName = null;

    @Before
    public void setUp() throws Exception {
        httpClient = HttpClients.createDefault();
        LOG.info("=== CACHE INVALIDATION WORKFLOW TEST SETUP ===");
        LOG.info("🌍 Magento URL: {}", MAGENTO_BASE_URL);
    }

    @After
    public void tearDown() throws Exception {
        LOG.info("🧹 CLEANUP: Reverting names back to original values...");

        try {
            // Revert product name if we changed it
            if (lastProductSku != null && lastOriginalProductName != null) {
                LOG.info("   🔄 Reverting product '{}' to original name: '{}'", lastProductSku, lastOriginalProductName);
                updateMagentoProductName(lastProductSku, lastOriginalProductName);
                LOG.info("   ✅ Product name reverted");
            }

            // Revert category name if we changed it
            if (lastCategoryId != null && lastOriginalCategoryName != null) {
                LOG.info("   🔄 Reverting category '{}' to original name: '{}'", lastCategoryId, lastOriginalCategoryName);
                updateMagentoCategoryName(lastCategoryId, lastOriginalCategoryName);
                LOG.info("   ✅ Category name reverted");
            }

        } catch (Exception e) {
            LOG.error("❌ Failed to revert names during cleanup: {}", e.getMessage(), e);
        }

        if (httpClient != null) {
            httpClient.close();
        }

        LOG.info("🧹 Cleanup complete");
    }

    /**
     * Debug HTML Parsing Test - Test parsing category and product names from real HTML
     */
    @Test
    public void testDebugHtmlParsing() throws Exception {
        String categoryPageUrl = "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-leather-belts.html";
        String productSku = "BLT-LEA-001";

        LOG.info("🔍 DEBUG: Testing HTML parsing for URL: {}", categoryPageUrl);

        try {
            // Step 1: Load the page
            SlingHttpResponse response = adminAuthor.doGet(categoryPageUrl, 200);
            Document doc = Jsoup.parse(response.getContent());
            LOG.info("✅ Page loaded successfully, HTML length: {}", response.getContent().length());

            // Step 2: Extract category name - EXACTLY from your HTML structure
            LOG.info("🔍 Looking for category name...");

            // Method 1: <span class="category__title">Leather Belts</span>
            Elements categorySpans = doc.select("span.category__title");
            LOG.info("Found {} elements with span.category__title", categorySpans.size());
            if (categorySpans.size() > 0) {
                String categoryName = categorySpans.first().text().trim();
                LOG.info("✅ CATEGORY NAME FOUND: '{}'", categoryName);
            } else {
                LOG.warn("❌ No category__title span found");
                // Debug alternative selectors
                Elements h1Title = doc.select("h1 .category__title");
                LOG.info("Alternative h1 .category__title found: {}", h1Title.size());
                if (h1Title.size() > 0) {
                    LOG.info("Alternative category name: '{}'", h1Title.first().text());
                }
            }

            // Step 3: Extract product name - EXACTLY from your HTML structure
            LOG.info("🔍 Looking for product name with SKU '{}'...", productSku);

            // Method 1: Find product by SKU then get title
            Elements productItems = doc.select("a.productcollection__item[data-product-sku='" + productSku + "']");
            LOG.info("Found {} product items with SKU '{}'", productItems.size(), productSku);

            if (productItems.size() > 0) {
                Element productItem = productItems.first();

                // Extract from: <div class="productcollection__item-title"><span>Black Leather Belt 8250</span></div>
                Elements productTitleSpans = productItem.select("div.productcollection__item-title span");
                LOG.info("Found {} title spans for product", productTitleSpans.size());

                if (productTitleSpans.size() > 0) {
                    String productName = productTitleSpans.first().text().trim();
                    LOG.info("✅ PRODUCT NAME FOUND: '{}'", productName);
                } else {
                    LOG.warn("❌ No productcollection__item-title span found in product");
                    // Debug alternative
                    Elements altSpans = productItem.select(".productcollection__item-title span");
                    LOG.info("Alternative .productcollection__item-title span found: {}", altSpans.size());
                    if (altSpans.size() > 0) {
                        LOG.info("Alternative product name: '{}'", altSpans.first().text());
                    }
                }

                // Also check title attribute
                String titleAttr = productItem.attr("title");
                LOG.info("Product title attribute: '{}'", titleAttr);

            } else {
                LOG.warn("❌ No product found with SKU '{}'", productSku);
                // Debug what products we do find
                Elements allProducts = doc.select("a.productcollection__item[data-product-sku]");
                LOG.info("Total products found: {}", allProducts.size());
                for (Element product : allProducts) {
                    String sku = product.attr("data-product-sku");
                    String title = product.attr("title");
                    LOG.info("  - Product SKU: '{}', Title: '{}'", sku, title);
                }
            }

            LOG.info("🎯 HTML Parsing Debug Complete!");

        } catch (Exception e) {
            LOG.error("❌ Debug test failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * AEM 6.5 Product Test - Leather Belts Category with Black Leather Belt (BLT-LEA-001)
     */
    @Test
    @Category(IgnoreOnCloud.class)
    public void test65_Product_CacheInvalidation() throws Exception {
        runProductCacheInvalidationTest(
                "BLT-LEA-001", // SKU
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-leather-belts.html", // Category page
                "venia-leather-belts", // URL key
                "AEM 6.5 - Product"
        );
    }

    /**
     * AEM 6.5 Category Test - Leather Belts Category with Black Leather Belt (BLT-LEA-001)
     */
    @Test
    @Category(IgnoreOnCloud.class)
    public void test65_Category_CacheInvalidation() throws Exception {
        runCategoryCacheInvalidationTest(
                "BLT-LEA-001", // SKU
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-leather-belts.html", // Category page
                "venia-leather-belts", // URL key
                "AEM 6.5 - Category"
        );
    }

    /**
     * Cloud Product Test - Fabric Belts Category with Canvas Fabric Belt (BLT-FAB-001)
     */
    @Test
    @Category(IgnoreOn65.class)
    public void testCloud_Product_CacheInvalidation() throws Exception {
        runProductCacheInvalidationTest(
                "BLT-FAB-001", // SKU
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-fabric-belts.html", // Category page
                "venia-fabric-belts", // URL key
                "Cloud - Product"
        );
    }

    /**
     * Cloud Category Test - Fabric Belts Category with Canvas Fabric Belt (BLT-FAB-001)
     */
    @Test
    @Category(IgnoreOn65.class)
    public void testCloud_Category_CacheInvalidation() throws Exception {
        runCategoryCacheInvalidationTest(
                "BLT-FAB-001", // SKU
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-fabric-belts.html", // Category page
                "venia-fabric-belts", // URL key
                "Cloud - Category"
        );
    }

    private void runProductCacheInvalidationTest(String productSku, String categoryPageUrl, String categoryUrlKey, String environment) throws Exception {
        LOG.info("=== PRODUCT CACHE INVALIDATION TEST - {} ===", environment);
        LOG.info("🎯 SKU: {}", productSku);
        LOG.info("📂 Category Page: {}", categoryPageUrl);
        LOG.info("🔑 Category URL Key: {}", categoryUrlKey);
        LOG.info("📄 Testing PRODUCT cache invalidation only");

        String originalProductName = null;
        Random random = new Random();
        String randomSuffix = generateRandomString(6); // Generate 6-character random string

        try {
            // STEP 1: Get product name from Magento
            LOG.info("📋 STEP 1: Getting original product name from Magento");
            JsonNode productData = getMagentoProductData(productSku);
            originalProductName = productData.get("name").asText();
            LOG.info("   ✓ Magento Product Name: '{}'", originalProductName);

            // STEP 2: Update product name in Magento
            String updatedProductName = originalProductName + " " + randomSuffix;
            LOG.info("🔄 STEP 2: Updating Magento product name");
            updateMagentoProductName(productSku, updatedProductName);
            LOG.info("   ✓ Updated Magento Product: '{}'", updatedProductName);

            // STEP 3: Verify AEM still shows old data
            LOG.info("📋 STEP 3: Checking AEM still shows cached data");
            String aemProductName = getCurrentProductNameFromAEMPage(categoryPageUrl, productSku);
            LOG.info("   AEM Product Shows: '{}'", aemProductName);
            LOG.info("   Updated Magento Product: '{}'", updatedProductName);
            boolean productCacheWorking = !aemProductName.equals(updatedProductName);
            LOG.info("   Product Cache Working: {}", productCacheWorking ? "✅ YES" : "❌ NO");

            // STEP 4: Call cache invalidation (product only)
            LOG.info("🚀 STEP 4: Calling cache invalidation servlet for PRODUCT only");
            boolean cacheInvalidated = callCacheInvalidationServlet(productSku, null); // No category
            assertTrue("Cache invalidation servlet call failed", cacheInvalidated);

            // STEP 5: Wait and verify cache is cleared
            LOG.info("⏳ STEP 5: Waiting for cache invalidation...");
            Thread.sleep(10000); // Wait 10 seconds

            LOG.info("🔍 STEP 6: Checking AEM now shows fresh product data");
            String freshProductName = getCurrentProductNameFromAEMPage(categoryPageUrl, productSku);
            LOG.info("   Fresh Product Check: '{}'", freshProductName);
            boolean productUpdated = freshProductName.equals(updatedProductName);
            LOG.info("   Product Updated: {}", productUpdated ? "✅ YES" : "❌ NO");

            assertTrue("Product cache invalidation failed - AEM not showing fresh data", productUpdated);
            LOG.info("🎉 SUCCESS: Product cache invalidation test passed!");

        } finally {
            // Restore original product name
            if (originalProductName != null) {
                try {
                    updateMagentoProductName(productSku, originalProductName);
                    LOG.info("🔄 Restored product name: {}", originalProductName);
                } catch (Exception e) {
                    LOG.warn("Could not restore product name: {}", e.getMessage());
                }
            }
        }
    }

    private void runCategoryCacheInvalidationTest(String productSku, String categoryPageUrl, String categoryUrlKey, String environment) throws Exception {
        LOG.info("=== CATEGORY CACHE INVALIDATION TEST - {} ===", environment);
        LOG.info("🎯 SKU: {}", productSku);
        LOG.info("📂 Category Page: {}", categoryPageUrl);
        LOG.info("🔑 Category URL Key: {}", categoryUrlKey);
        LOG.info("📄 Testing CATEGORY cache invalidation only");

        String originalCategoryName = null;
        String categoryId = null;
        Random random = new Random();
        String randomSuffix = generateRandomString(6); // Generate 6-character random string

        try {
            // STEP 1: Get category name from AEM
            LOG.info("📋 STEP 1: Getting original category name from AEM");
            String aemCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            LOG.info("   ✅ AEM Category Name: '{}'", aemCategoryName);

            // STEP 2: Get category data from Magento using GraphQL
            LOG.info("🔍 STEP 2: Getting category data from Magento GraphQL");
            String categoryUid = getCategoryUidFromUrlKey(categoryUrlKey);
            
            // Extract category ID from UID (Base64 decode)
            try {
                categoryId = new String(java.util.Base64.getDecoder().decode(categoryUid), "UTF-8");
                LOG.info("   Category ID from UID: '{}'", categoryId);
            } catch (Exception e) {
                LOG.warn("Could not decode category UID '{}': {}", categoryUid, e.getMessage());
                categoryId = categoryUid; // fallback
            }

            // Get original name from Magento
            JsonNode categoryData = getMagentoCategoryData(categoryId);
            originalCategoryName = categoryData.get("name").asText();
            LOG.info("   ✓ Magento Category Name: '{}'", originalCategoryName);

            // STEP 3: Update category name in Magento
            String updatedCategoryName = originalCategoryName + " " + randomSuffix;
            LOG.info("🔄 STEP 3: Updating Magento category name");
            updateMagentoCategoryName(categoryId, updatedCategoryName);
            LOG.info("   ✓ Updated Magento Category: '{}'", updatedCategoryName);

            // STEP 4: Verify AEM still shows old data
            LOG.info("📋 STEP 4: Checking AEM still shows cached data");
            String currentAemCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            LOG.info("   AEM Category Shows: '{}'", currentAemCategoryName);
            LOG.info("   Updated Magento Category: '{}'", updatedCategoryName);
            boolean categoryCacheWorking = !currentAemCategoryName.equals(updatedCategoryName);
            LOG.info("   Category Cache Working: {}", categoryCacheWorking ? "✅ YES" : "❌ NO");

            // STEP 5: Call cache invalidation (category only)
            LOG.info("🚀 STEP 5: Calling cache invalidation servlet for CATEGORY only");
            boolean cacheInvalidated = callCacheInvalidationServlet(null, categoryUrlKey); // No product
            assertTrue("Cache invalidation servlet call failed", cacheInvalidated);

            // STEP 6: Wait and verify cache is cleared
            LOG.info("⏳ STEP 6: Waiting for cache invalidation...");
            Thread.sleep(10000); // Wait 10 seconds

            LOG.info("🔍 STEP 7: Checking AEM now shows fresh category data");
            String freshCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            LOG.info("   Fresh Category Check: '{}'", freshCategoryName);
            boolean categoryUpdated = freshCategoryName.equals(updatedCategoryName);
            LOG.info("   Category Updated: {}", categoryUpdated ? "✅ YES" : "❌ NO");

            assertTrue("Category cache invalidation failed - AEM not showing fresh data", categoryUpdated);
            LOG.info("🎉 SUCCESS: Category cache invalidation test passed!");

        } finally {
            // Restore original category name
            if (originalCategoryName != null && categoryId != null) {
                try {
                    updateMagentoCategoryName(categoryId, originalCategoryName);
                    LOG.info("🔄 Restored category name: {}", originalCategoryName);
                } catch (Exception e) {
                    LOG.warn("Could not restore category name: {}", e.getMessage());
                }
            }
        }
    }

    private void runCacheInvalidationTest(String productSku, String categoryPageUrl, String environment) throws Exception {
        LOG.info("=== CACHE INVALIDATION TEST - {} ===", environment);
        LOG.info("🎯 SKU: {}", productSku);
        LOG.info("📂 Category Page: {}", categoryPageUrl);
        LOG.info("📄 Using ONLY category page - no separate product page needed");

        String originalProductName = null;
        String originalCategoryName = null;
        String categoryId = null;

        try {
            // STEP 1: Get category name and product name from AEM category page
            LOG.info("📋 STEP 1: Getting names from AEM category page");
            SlingHttpResponse categoryResponse = adminAuthor.doGet(categoryPageUrl, 200);
            Document categoryDoc = Jsoup.parse(categoryResponse.getContent());

            // Get category name from .category__title span (EXACT from your HTML)
            String categoryName = null;

            try {
                LOG.debug("🔍 Looking for category name in HTML...");

                // Method 1: Direct from <span class="category__title">Leather Belts</span>
                Elements titleElements = categoryDoc.select("span.category__title");
                LOG.debug("Found {} elements with span.category__title", titleElements.size());
                if (titleElements.size() > 0) {
                    categoryName = titleElements.first().text().trim();
                    LOG.info("   ✅ AEM Category Name (from span): '{}'", categoryName);
                } else {
                    // Method 2: Try h1 .category__title
                    titleElements = categoryDoc.select("h1 .category__title");
                    if (titleElements.size() > 0) {
                        categoryName = titleElements.first().text().trim();
                        LOG.info("   ✅ AEM Category Name (from h1): '{}'", categoryName);
                    }
                }

                // Get category ID from JSON data-cif-category-context
                Elements categoryRoot = categoryDoc.select("article[data-cif-category-context]");
                if (categoryRoot.size() > 0) {
                    String categoryContext = categoryRoot.first().attr("data-cif-category-context");
                    if (categoryContext != null && !categoryContext.isEmpty()) {
                        try {
                            categoryContext = categoryContext.replace("&quot;", "\"");
                            JsonNode contextJson = OBJECT_MAPPER.readTree(categoryContext);
                            if (contextJson != null && contextJson.has("urlKey")) {
                                categoryId = contextJson.get("urlKey").asText();
                                LOG.info("   ✅ AEM Category ID: '{}'", categoryId);
                            }
                            // Double check category name from JSON if not found above
                            if ((categoryName == null || categoryName.isEmpty()) && contextJson.has("name")) {
                                categoryName = contextJson.get("name").asText();
                                LOG.info("   ✅ AEM Category Name (from JSON): '{}'", categoryName);
                            }
                        } catch (Exception e) {
                            LOG.warn("Could not parse category JSON: {}", e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                LOG.warn("Error getting category name: {}", e.getMessage());
            }

            // No fallback - category must be found from AEM HTML

            // Get product name from SPECIFIC SKU item (EXACT from your HTML)
            String productName = null;
            try {
                LOG.debug("🔍 Looking for product with SKU '{}' in HTML...", productSku);

                // Find the product item with matching SKU: <a class="productcollection__item" data-product-sku="BLT-LEA-001">
                Elements productItems = categoryDoc.select(".productcollection__item[data-product-sku='" + productSku + "']");
                LOG.debug("Found {} product items with SKU '{}'", productItems.size(), productSku);

                if (productItems.size() > 0) {
                    Element productItem = productItems.first();
                    LOG.debug("Found product item: {}", productItem.tagName());

                    // Method 1: From <div class="productcollection__item-title"><span>Black Leather Belt 8250</span></div>
                    Elements titleSpans = productItem.select("div.productcollection__item-title span");
                    LOG.debug("Found {} title spans in product item", titleSpans.size());
                    if (titleSpans.size() > 0) {
                        productName = titleSpans.first().text().trim();
                        LOG.info("   ✅ AEM Product Name (from div span): '{}'", productName);
                    } else {
                        // Method 2: Try just .productcollection__item-title span
                        titleSpans = productItem.select(".productcollection__item-title span");
                        if (titleSpans.size() > 0) {
                            productName = titleSpans.first().text().trim();
                            LOG.info("   ✅ AEM Product Name (from title span): '{}'", productName);
                        } else {
                            // Method 3: From title attribute: title="Black Leather Belt 8250"
                            String titleAttr = productItem.attr("title");
                            if (titleAttr != null && !titleAttr.isEmpty()) {
                                productName = titleAttr.trim();
                                LOG.info("   ✅ AEM Product Name (from title attr): '{}'", productName);
                            } else {
                                // Method 4: From data layer JSON: "dc:title":"Black Leather Belt 8250"
                                String dataLayer = productItem.attr("data-cmp-data-layer");
                                if (dataLayer != null && !dataLayer.isEmpty()) {
                                    try {
                                        dataLayer = dataLayer.replace("&quot;", "\"");
                                        JsonNode jsonData = OBJECT_MAPPER.readTree(dataLayer);
                                        // Get the first (and only) key in the JSON object
                                        if (jsonData.fields().hasNext()) {
                                            JsonNode productData = jsonData.fields().next().getValue();
                                            if (productData.has("dc:title")) {
                                                productName = productData.get("dc:title").asText();
                                                LOG.info("   ✅ AEM Product Name (from JSON): '{}'", productName);
                                            }
                                        }
                                    } catch (Exception e) {
                                        LOG.warn("Could not parse data layer JSON: {}", e.getMessage());
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LOG.warn("❌ No product found with SKU '{}' in category page", productSku);
                    // Let's debug - show what SKUs we do find
                    Elements allProducts = categoryDoc.select(".productcollection__item[data-product-sku]");
                    LOG.debug("Found {} total products in page", allProducts.size());
                    for (Element product : allProducts) {
                        String foundSku = product.attr("data-product-sku");
                        LOG.debug("  - Found product with SKU: '{}'", foundSku);
                    }
                }
            } catch (Exception e) {
                LOG.warn("Error parsing product name: {}", e.getMessage());
            }

            // No fallback - product name must be found from AEM HTML

            // STEP 2: Get original names from Magento using SKU
            LOG.info("📋 STEP 2: Getting original names from Magento using SKU");
            JsonNode productData = getMagentoProductData(productSku);
            originalProductName = productData.get("name").asText();
            LOG.info("   ✓ Magento Product Name: '{}'", originalProductName);

            // Store for cleanup
            lastProductSku = productSku;
            lastOriginalProductName = originalProductName;

            // Get category data from product's category assignment
            LOG.info("🔍 Looking up category data for product...");
            try {
                // Method 1: Try category_links (most common)
                if (productData.has("category_links") && productData.get("category_links").isArray()) {
                    JsonNode categoryLinks = productData.get("category_links");
                    LOG.info("   Found {} category links for product", categoryLinks.size());
                    if (categoryLinks.size() > 0) {
                        String productCategoryId = categoryLinks.get(0).get("category_id").asText();
                        LOG.info("   ✓ Found category ID from category_links: '{}'", productCategoryId);

                        JsonNode categoryData = getMagentoCategoryData(productCategoryId);
                        originalCategoryName = categoryData.get("name").asText();
                        categoryId = productCategoryId;
                        LOG.info("   ✅ Magento Category Name: '{}'", originalCategoryName);

                        // Store for cleanup
                        lastCategoryId = categoryId;
                        lastOriginalCategoryName = originalCategoryName;
                    }
                }
                // Method 2: Try category_ids array
                else if (productData.has("category_ids") && productData.get("category_ids").isArray()) {
                    JsonNode categoryIds = productData.get("category_ids");
                    LOG.info("   Found {} category IDs for product", categoryIds.size());
                    if (categoryIds.size() > 0) {
                        String productCategoryId = categoryIds.get(0).asText();
                        LOG.info("   ✓ Found category ID from category_ids: '{}'", productCategoryId);

                        JsonNode categoryData = getMagentoCategoryData(productCategoryId);
                        originalCategoryName = categoryData.get("name").asText();
                        categoryId = productCategoryId;
                        LOG.info("   ✅ Magento Category Name: '{}'", originalCategoryName);

                        // Store for cleanup
                        lastCategoryId = categoryId;
                        lastOriginalCategoryName = originalCategoryName;
                    }
                }
                // Method 3: Try extension_attributes.category_links
                else if (productData.has("extension_attributes")) {
                    JsonNode extAttrs = productData.get("extension_attributes");
                    if (extAttrs.has("category_links") && extAttrs.get("category_links").isArray()) {
                        JsonNode categoryLinks = extAttrs.get("category_links");
                        LOG.info("   Found {} category links in extension_attributes", categoryLinks.size());
                        if (categoryLinks.size() > 0) {
                            String productCategoryId = categoryLinks.get(0).get("category_id").asText();
                            LOG.info("   ✓ Found category ID from extension_attributes: '{}'", productCategoryId);

                            JsonNode categoryData = getMagentoCategoryData(productCategoryId);
                            originalCategoryName = categoryData.get("name").asText();
                            categoryId = productCategoryId;
                            LOG.info("   ✅ Magento Category Name: '{}'", originalCategoryName);

                            // Store for cleanup
                            lastCategoryId = categoryId;
                            lastOriginalCategoryName = originalCategoryName;
                        }
                    }
                }

                // If still no category found, log warning
                if (originalCategoryName == null) {
                    LOG.warn("   ❌ No category data found in product - tried category_links, category_ids, and extension_attributes");
                }

            } catch (Exception e) {
                LOG.error("❌ Could not get category data from product: {}", e.getMessage(), e);
            }

            // No fallback - category data must come from Magento product data only
            if (originalCategoryName == null) {
                LOG.error("❌ FAILED: Cannot get category data from Magento product - test will fail");
            }

            // STEP 3: Update product and category names in Magento (existing name + random number)
            String randomNumber = String.valueOf(System.currentTimeMillis() % 10000); // Last 4 digits
            String newProductName = originalProductName + " " + randomNumber;
            String newCategoryName = (originalCategoryName != null) ? originalCategoryName + " " + randomNumber : null;

            LOG.info("🔄 STEP 3: Updating Magento names");
            updateMagentoProductName(productSku, newProductName);
            LOG.info("   ✓ Updated Magento Product: '{}'", newProductName);

            if (categoryId != null && originalCategoryName != null) {
                updateMagentoCategoryName(categoryId, newCategoryName);
                LOG.info("   ✓ Updated Magento Category: '{}'", newCategoryName);
            }

            // STEP 4: Verify AEM still shows OLD data (cache working) - check ONLY category page
            LOG.info("📋 STEP 4: Checking AEM category page still shows old data");
            String aemProductCheck = getCurrentProductNameFromAEMPage(categoryPageUrl, productSku); // Use category page only
            String aemCategoryCheck = getCurrentCategoryNameFromAEMPage(categoryPageUrl);

            LOG.info("   AEM Product Shows: '{}'", aemProductCheck);
            LOG.info("   AEM Category Shows: '{}'", aemCategoryCheck);
            LOG.info("   Updated Magento Product: '{}'", newProductName);
            LOG.info("   Updated Magento Category: '{}'", newCategoryName);

            // Verify cache is working (AEM should NOT show the random number)
            boolean productCacheWorking = !aemProductCheck.contains(randomNumber);
            boolean categoryCacheWorking = !aemCategoryCheck.contains(randomNumber);
            LOG.info("   Product Cache Working: {} {}", productCacheWorking ? "✅" : "❌", productCacheWorking ? "YES" : "NO");
            LOG.info("   Category Cache Working: {} {}", categoryCacheWorking ? "✅" : "❌", categoryCacheWorking ? "YES" : "NO");

            assertTrue("Cache not working - AEM showing fresh data immediately", productCacheWorking);

            // STEP 5: Call cache invalidation servlet for both product and category
            LOG.info("🚀 STEP 5: Calling cache invalidation servlet for product and category");
            boolean servletSuccess = callCacheInvalidationServlet(productSku, categoryId);
            assertTrue("Cache invalidation servlet should succeed", servletSuccess);

            // STEP 6: Wait for cache invalidation
            LOG.info("⏳ STEP 6: Waiting for cache invalidation...");
            Thread.sleep(10000); // Wait 10 seconds

            // STEP 7: Verify AEM now shows NEW data (check ONLY category page)
            LOG.info("🔍 STEP 7: Checking AEM category page now shows fresh data");
            String freshProductCheck = getCurrentProductNameFromAEMPage(categoryPageUrl, productSku); // Use category page only
            String freshCategoryCheck = getCurrentCategoryNameFromAEMPage(categoryPageUrl);

            LOG.info("   Fresh Product Check: '{}'", freshProductCheck);
            LOG.info("   Fresh Category Check: '{}'", freshCategoryCheck);

            boolean productUpdated = freshProductCheck.contains(randomNumber);
            boolean categoryUpdated = freshCategoryCheck.contains(randomNumber);

            LOG.info("   Product Updated: {} {}", productUpdated ? "✅" : "❌", productUpdated ? "YES" : "NO");
            LOG.info("   Category Updated: {} {}", categoryUpdated ? "✅" : "❌", categoryUpdated ? "YES" : "NO");

            if (!productUpdated) {
                LOG.info("🔄 Trying one more time after extra wait...");
                Thread.sleep(10000);
                freshProductCheck = getCurrentProductNameFromAEMPage(categoryPageUrl, productSku); // Use category page only
                productUpdated = freshProductCheck.contains(randomNumber);
                LOG.info("   Extra Product Check: '{}' - Updated: {}", freshProductCheck, productUpdated);
            }

            assertTrue("Product cache invalidation failed - AEM not showing fresh data", productUpdated);
            LOG.info("🎉 SUCCESS: Cache invalidation test passed!");

        } finally {
            // Restore original names
            if (originalProductName != null) {
                try {
                    updateMagentoProductName(productSku, originalProductName);
                    LOG.info("🔄 Restored product name: {}", originalProductName);
                } catch (Exception e) {
                    LOG.warn("Could not restore product name: {}", e.getMessage());
                }
            }

            if (originalCategoryName != null && categoryId != null) {
                try {
                    updateMagentoCategoryName(categoryId, originalCategoryName);
                    LOG.info("🔄 Restored category name: {}", originalCategoryName);
                } catch (Exception e) {
                    LOG.warn("Could not restore category name: {}", e.getMessage());
                }
            }
        }
    }

    private JsonNode getMagentoProductData(String sku) throws Exception {
        String url = MAGENTO_REST_URL + "/products/" + sku;
        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + MAGENTO_ADMIN_TOKEN);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String content = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() == 200) {
                return OBJECT_MAPPER.readTree(content);
            } else {
                throw new Exception("Failed to get product data: " + response.getStatusLine().getStatusCode());
            }
        }
    }

    private JsonNode getMagentoCategoryData(String categoryId) throws Exception {
        String url = MAGENTO_REST_URL + "/categories/" + categoryId;
        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + MAGENTO_ADMIN_TOKEN);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String content = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() == 200) {
                return OBJECT_MAPPER.readTree(content);
            } else {
                throw new Exception("Failed to get category data: " + response.getStatusLine().getStatusCode());
            }
        }
    }

    private void updateMagentoProductName(String sku, String newName) throws Exception {
        String url = MAGENTO_REST_URL + "/products/" + sku;
        HttpPut request = new HttpPut(url);
        request.setHeader("Authorization", "Bearer " + MAGENTO_ADMIN_TOKEN);
        request.setHeader("Content-Type", "application/json");

        String payload = String.format("{\"product\":{\"name\":\"%s\"}}", newName);
        request.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new Exception("Failed to update product: " + response.getStatusLine().getStatusCode());
            }
        }
        Thread.sleep(2000);
    }

    private void updateMagentoCategoryName(String categoryId, String newName) throws Exception {
        LOG.info("🔄 Updating Magento category ID '{}' to name '{}'", categoryId, newName);

        String url = MAGENTO_REST_URL + "/categories/" + categoryId;
        LOG.info("   Category update URL: {}", url);

        HttpPut request = new HttpPut(url);
        request.setHeader("Authorization", "Bearer " + MAGENTO_ADMIN_TOKEN);
        request.setHeader("Content-Type", "application/json");

        String payload = String.format("{\"category\":{\"name\":\"%s\"}}", newName);
        LOG.info("   Category update payload: {}", payload);
        request.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = "";
            if (response.getEntity() != null) {
                responseBody = EntityUtils.toString(response.getEntity());
            }

            LOG.info("   Category update response: {} - {}", statusCode, responseBody);

            if (statusCode != 200) {
                LOG.error("❌ Failed to update category '{}': {} - {}", categoryId, statusCode, responseBody);
                throw new Exception("Failed to update category: " + statusCode + " - " + responseBody);
            } else {
                LOG.info("   ✅ Successfully updated category '{}' to '{}'", categoryId, newName);
            }
        }
        Thread.sleep(2000);
    }

    private String getCurrentProductNameFromAEMPage(String categoryPageUrl, String targetSku) throws ClientException {
        try {
            SlingHttpResponse response = adminAuthor.doGet(categoryPageUrl, 200);
            Document doc = Jsoup.parse(response.getContent());

            // Find the specific product by SKU
            Elements productItems = doc.select(".productcollection__item[data-product-sku='" + targetSku + "']");
            if (productItems.size() > 0) {
                Element productItem = productItems.first();

                // 1. From .productcollection__item-title span
                Elements titleElements = productItem.select(".productcollection__item-title span");
                if (titleElements.size() > 0) {
                    String productName = titleElements.first().text().trim();
                    LOG.debug("Found product name from title span: '{}'", productName);
                    return productName;
                }

                // 2. From title attribute
                String titleAttr = productItem.attr("title");
                if (titleAttr != null && !titleAttr.isEmpty()) {
                    LOG.debug("Found product name from title attr: '{}'", titleAttr);
                    return titleAttr.trim();
                }

                // 3. From data layer JSON
                String dataLayer = productItem.attr("data-cmp-data-layer");
                if (dataLayer != null && !dataLayer.isEmpty()) {
                    try {
                        dataLayer = dataLayer.replace("&quot;", "\"");
                        JsonNode jsonData = OBJECT_MAPPER.readTree(dataLayer);
                        JsonNode firstKey = jsonData.fields().next().getValue();
                        if (firstKey.has("dc:title")) {
                            String productName = firstKey.get("dc:title").asText();
                            LOG.debug("Found product name from JSON: '{}'", productName);
                            return productName;
                        }
                    } catch (Exception e) {
                        LOG.warn("Could not parse data layer JSON: {}", e.getMessage());
                    }
                }
            }

            LOG.warn("Could not find product with SKU '{}' in category page", targetSku);
            return "NOT_FOUND";
        } catch (Exception e) {
            LOG.error("Error getting product name from category page: {}", e.getMessage());
            return "ERROR";
        }
    }

    // Keep old method for backward compatibility
    private String getCurrentProductNameFromAEMPage(String categoryPageUrl) throws ClientException {
        return getCurrentProductNameFromAEMPage(categoryPageUrl, "BLT-LEA-001");
    }

    private String getCurrentCategoryNameFromAEMPage(String categoryPageUrl) throws ClientException {
        try {
            SlingHttpResponse response = adminAuthor.doGet(categoryPageUrl, 200);
            Document doc = Jsoup.parse(response.getContent());

            // Look for category title
            Elements title = doc.select(".category__title");
            if (title.size() > 0) {
                return title.first().text().trim();
            }

            // Fallback: breadcrumb
            Elements breadcrumb = doc.select(".cmp-breadcrumb__item--active span[itemprop='name']");
            if (breadcrumb.size() > 0) {
                return breadcrumb.first().text().trim();
            }

            return "NOT_FOUND";
        } catch (Exception e) {
            LOG.error("Error getting category name: {}", e.getMessage());
            return "ERROR";
        }
    }

    private boolean callCacheInvalidationServlet(String productSku, String categoryUrlKey) {
        try {
            // Build payload with both product SKU and category UID if available
            String payload;
            if (categoryUrlKey != null && !categoryUrlKey.isEmpty()) {
                // Get category UID from Magento GraphQL using url_key
                String categoryUid = getCategoryUidFromUrlKey(categoryUrlKey);
                payload = String.format(
                        "{\n" +
                                "    \"productSkus\": [\"%s\"],\n" +
                                "    \"categoryUids\": [\"%s\"],\n" +
                                "    \"storePath\": \"%s\"\n" +
                                "}", productSku, categoryUid, STORE_PATH);
                LOG.info("📝 Cache invalidation payload (product + category): {}", payload);
                LOG.info("📝 Using category URL key '{}' -> UID '{}'", categoryUrlKey, categoryUid);
            } else {
                payload = String.format(
                        "{\n" +
                                "    \"productSkus\": [\"%s\"],\n" +
                                "    \"storePath\": \"%s\"\n" +
                                "}", productSku, STORE_PATH);
                LOG.info("📝 Cache invalidation payload (product only): {}", payload);
            }

            SlingHttpResponse response = adminAuthor.doPost(
                    CACHE_INVALIDATION_ENDPOINT,
                    new StringEntity(payload, ContentType.APPLICATION_JSON),
                    null,
                    200);

            int statusCode = response.getStatusLine().getStatusCode();
            String responseContent = response.getContent();

            LOG.info("📤 Response: Status={}, Content={}", statusCode, responseContent);

            return statusCode == 200;
        } catch (Exception e) {
            LOG.error("❌ Cache invalidation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    // Keep old method for compatibility
    private boolean callCacheInvalidationServlet(String productSku) {
        return callCacheInvalidationServlet(productSku, null);
    }

    /**
     * Generate random string for test purposes.
     */
    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Get category UID from Magento using GraphQL with url_key.
     * Uses GraphQL query: categoryList(filters: {url_key: {eq: "url-key"}}) { uid }
     */
    private String getCategoryUidFromUrlKey(String categoryUrlKey) {
        try {
            LOG.info("🔍 Getting category UID from Magento GraphQL for url_key: '{}'", categoryUrlKey);
            
            // Create GraphQL query
            String graphqlQuery = String.format(
                "{ categoryList(filters: {url_key: {eq: \"%s\"}}) { uid name url_key level path } }",
                categoryUrlKey
            );
            
            LOG.debug("GraphQL Query: {}", graphqlQuery);
            
            String url = MAGENTO_BASE_URL + "/graphql";
            HttpPost request = new HttpPost(url);
            request.setHeader("Content-Type", "application/json");
            
            // Use ObjectMapper to create proper JSON payload
            com.fasterxml.jackson.databind.node.ObjectNode jsonPayload = OBJECT_MAPPER.createObjectNode();
            jsonPayload.put("query", graphqlQuery);
            String payload = OBJECT_MAPPER.writeValueAsString(jsonPayload);
            LOG.debug("GraphQL Payload: {}", payload);
            
            request.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseContent = EntityUtils.toString(response.getEntity());
                int statusCode = response.getStatusLine().getStatusCode();
                
                LOG.debug("GraphQL Response Status: {}", statusCode);
                LOG.debug("GraphQL Response Content: {}", responseContent);
                
                if (statusCode == 200) {
                    JsonNode responseJson = OBJECT_MAPPER.readTree(responseContent);
                    JsonNode data = responseJson.get("data");
                    JsonNode categoryList = data.get("categoryList");
                    
                    if (categoryList != null && categoryList.isArray() && categoryList.size() > 0) {
                        JsonNode category = categoryList.get(0);
                        String uid = category.get("uid").asText();
                        String name = category.get("name").asText();
                        int level = category.get("level").asInt();
                        String path = category.get("path").asText();
                        
                        LOG.info("✅ Found category via GraphQL:");
                        LOG.info("   URL Key: '{}'", categoryUrlKey);
                        LOG.info("   Name: '{}'", name);
                        LOG.info("   UID: '{}'", uid);
                        LOG.info("   Level: {}", level);
                        LOG.info("   Path: '{}'", path);
                        
                        return uid;
                    } else {
                        LOG.error("❌ No category found for url_key: '{}'", categoryUrlKey);
                        throw new RuntimeException("No category found for url_key: " + categoryUrlKey);
                    }
                } else {
                    LOG.error("❌ GraphQL request failed with status: {}", statusCode);
                    LOG.error("❌ GraphQL Response Content: {}", responseContent);
                    LOG.error("❌ GraphQL Request URL: {}", url);
                    LOG.error("❌ GraphQL Request Payload: {}", payload);
                    throw new RuntimeException("GraphQL request failed: " + statusCode + " - " + responseContent);
                }
            }
        } catch (Exception e) {
            LOG.error("❌ Failed to get category UID from GraphQL for url_key '{}': {}", categoryUrlKey, e.getMessage());
            throw new RuntimeException("Failed to get category UID from GraphQL", e);
        }
    }
}