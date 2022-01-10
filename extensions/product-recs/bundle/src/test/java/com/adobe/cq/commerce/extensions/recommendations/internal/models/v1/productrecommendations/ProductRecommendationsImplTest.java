/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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
package com.adobe.cq.commerce.extensions.recommendations.internal.models.v1.productrecommendations;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.common.ValueMapDecorator;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.extensions.recommendations.testing.TestContext.newAemContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ProductRecommendationsImplTest {

    private ProductRecommendationsImpl productRecommendations;

    private static final String PRODUCT_RECS_PATH = "/content/landingPage/jcr:content/root/responsivegrid/product-recs";
    private static final String EMPTY_PRODUCT_RECS_PATH = "/content/landingPage/jcr:content/root/responsivegrid/product-recs-empty";
    private static final String PRECONFIGURED_RECS_PATH = "/content/landingPage/jcr:content/root/responsivegrid/product-recs-preconfigured";
    private static final String INCLUDE_CATEGORY_RECS_PATH = "/content/landingPage/jcr:content/root/responsivegrid/product-recs-include-category";
    private static final String EXCLUDE_CATEGORY_RECS_PATH = "/content/landingPage/jcr:content/root/responsivegrid/product-recs-exclude-category";
    private static final String INCLUDE_PRICE_RANGE_RECS_PATH = "/content/landingPage/jcr:content/root/responsivegrid/product-recs-include-price";
    private static final String EXCLUDE_PRICE_RANGE_RECS_PATH = "/content/landingPage/jcr:content/root/responsivegrid/product-recs-exclude-price";

    @Rule
    public final AemContext context = newAemContext("/context/jcr-content.json");

    private void setupTest(String componentPath) {
        // Mock resource and resolver
        Resource resource = context.resourceResolver().getResource(componentPath);
        Page currentPage = context.pageManager().getContainingPage(resource);
        context.currentPage(currentPage);
        context.currentResource(resource);

        SlingBindings bindings = new SlingBindings();
        context.request().setAttribute(SlingBindings.class.toString(), bindings);
        bindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, currentPage);

        productRecommendations = context.request().adaptTo(ProductRecommendationsImpl.class);
    }

    @Test
    public void testEmptyComponent() {
        setupTest(EMPTY_PRODUCT_RECS_PATH);
        assertTrue(productRecommendations.getPreconfigured());
        assertNull(productRecommendations.getTitle());
        assertNull(productRecommendations.getRecommendationType());
        assertNull(productRecommendations.getCategoryInclusions());
        assertNull(productRecommendations.getCategoryExclusions());
        assertNull(productRecommendations.getPriceRangeInclusions());
        assertNull(productRecommendations.getPriceRangeExclusions());
        assertFalse(productRecommendations.getAddToWishListEnabled());
    }

    @Test
    public void testPreconfigured() {
        setupTest(PRECONFIGURED_RECS_PATH);
        assertTrue(productRecommendations.getPreconfigured());
        assertNull(productRecommendations.getTitle());
        assertNull(productRecommendations.getRecommendationType());
        assertNull(productRecommendations.getCategoryInclusions());
        assertNull(productRecommendations.getCategoryExclusions());
        assertNull(productRecommendations.getPriceRangeInclusions());
        assertNull(productRecommendations.getPriceRangeExclusions());
        assertFalse(productRecommendations.getAddToWishListEnabled());
    }

    @Test
    public void testCategoryInclusion() {
        setupTest(INCLUDE_CATEGORY_RECS_PATH);
        assertFalse(productRecommendations.getPreconfigured());
        assertEquals("Product Recs", productRecommendations.getTitle());
        assertEquals("most-viewed", productRecommendations.getRecommendationType());
        assertEquals("shorts-men,pants-men", productRecommendations.getCategoryInclusions());
        assertNull(productRecommendations.getCategoryExclusions());
        assertNull(productRecommendations.getPriceRangeInclusions());
        assertNull(productRecommendations.getPriceRangeExclusions());
        assertFalse(productRecommendations.getAddToWishListEnabled());
    }

    @Test
    public void testCategoryExclusion() {
        setupTest(EXCLUDE_CATEGORY_RECS_PATH);
        assertFalse(productRecommendations.getPreconfigured());
        assertEquals("Recommended products", productRecommendations.getTitle());
        assertEquals("most-viewed", productRecommendations.getRecommendationType());
        assertNull(productRecommendations.getCategoryInclusions());
        assertEquals("tops-women", productRecommendations.getCategoryExclusions());
        assertNull(productRecommendations.getPriceRangeInclusions());
        assertNull(productRecommendations.getPriceRangeExclusions());
        assertFalse(productRecommendations.getAddToWishListEnabled());
    }

    @Test
    public void testPriceRangeInclusion() {
        setupTest(INCLUDE_PRICE_RANGE_RECS_PATH);
        assertFalse(productRecommendations.getPreconfigured());
        assertEquals("Recommended products", productRecommendations.getTitle());
        assertEquals("most-viewed", productRecommendations.getRecommendationType());
        assertNull(productRecommendations.getCategoryInclusions());
        assertNull(productRecommendations.getCategoryExclusions());
        assertEquals(Double.valueOf(10), productRecommendations.getPriceRangeInclusions().getMinPrice());
        assertEquals(Double.valueOf(100), productRecommendations.getPriceRangeInclusions().getMaxPrice());
        assertNull(productRecommendations.getPriceRangeExclusions());
        assertFalse(productRecommendations.getAddToWishListEnabled());
    }

    @Test
    public void testPriceRangeExclusion() {
        setupTest(EXCLUDE_PRICE_RANGE_RECS_PATH);
        assertFalse(productRecommendations.getPreconfigured());
        assertEquals("Recommended products", productRecommendations.getTitle());
        assertEquals("most-viewed", productRecommendations.getRecommendationType());
        assertNull(productRecommendations.getCategoryInclusions());
        assertNull(productRecommendations.getCategoryExclusions());
        assertNull(productRecommendations.getPriceRangeInclusions());
        assertEquals(Double.valueOf(30), productRecommendations.getPriceRangeExclusions().getMinPrice());
        assertEquals(Double.valueOf(50), productRecommendations.getPriceRangeExclusions().getMaxPrice());
        assertFalse(productRecommendations.getAddToWishListEnabled());
    }

    @Test
    public void testAddToWishListEnabled() {
        context.contentPolicyMapping(ProductRecommendationsImpl.RESOURCE_TYPE, "enableAddToWishList", Boolean.TRUE);
        setupTest(PRECONFIGURED_RECS_PATH);
        assertTrue(productRecommendations.getAddToWishListEnabled());
    }

    @Test
    public void testAddToWishListDisabledByConfiguration() {
        context.registerAdapter(Resource.class, ComponentsConfiguration.class, new ComponentsConfiguration(new ValueMapDecorator(
            ImmutableMap.of(
                "enableWishLists", Boolean.FALSE))));
        context.contentPolicyMapping(ProductRecommendationsImpl.RESOURCE_TYPE, "enableAddToWishList", Boolean.TRUE);
        setupTest(PRECONFIGURED_RECS_PATH);
        assertFalse(productRecommendations.getAddToWishListEnabled());
    }
}
