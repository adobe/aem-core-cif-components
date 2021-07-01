/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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
package com.adobe.cq.commerce.extensions.recommendations.internal.models.v1.productrecommendations;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class ProductRecommendationsImplTest {

    private ProductRecommendationsImpl productRecommendations;

    private static final String PRODUCT_RECS_PATH = "/content/landingPage/jcr:content/root/responsivegrid/product-recs";

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");

    private static AemContext createContext(String contentPath) {
        return new AemContext((AemContextCallback) context -> {
            // Load page structure
            context.load().json(contentPath, "/content");
        }, ResourceResolverType.JCR_MOCK);
    }

    @Before
    public void setupTest() {
        // Mock resource and resolver
        Resource resource = Mockito.spy(context.resourceResolver().getResource(PRODUCT_RECS_PATH));
        ResourceResolver resolver = Mockito.spy(resource.getResourceResolver());
        when(resource.getResourceResolver()).thenReturn(resolver);
        context.currentResource(resource);
        productRecommendations = context.request().adaptTo(ProductRecommendationsImpl.class);
    }

    @Test
    public void testFiltersEnabled() {
        assertNull(productRecommendations.getCategoryExclusions());
        assertNotNull(productRecommendations.getCategoryInclusions());
        assertEquals("shorts-men,pants-men", productRecommendations.getCategoryInclusions());
    }

    @Test
    public void testPriceRange() {
        assertNotNull(productRecommendations.getPriceRangeExclusions());
        assertNotNull(productRecommendations.getPriceRangeInclusions());
    }

    @Test
    public void testStringProperties() {
        assertEquals("Recommended products", productRecommendations.getTitle());
        assertEquals("most-viewed", productRecommendations.getRecommendationType());
        assertEquals("configurable,grouped,downloadable", productRecommendations.getTypeInclusions());
        assertNull("", productRecommendations.getTypeExclusions());
        assertEquals("search", productRecommendations.getVisibilityExclusions());
        assertNull(productRecommendations.getVisibilityInclusions());
        assertEquals("WJ08", productRecommendations.getProductInclusions());
        assertNull(productRecommendations.getProductExclusions());
    }

    @Test
    public void testBooleanProperties() {
        assertFalse(productRecommendations.excludeLowStock());
        assertTrue(productRecommendations.excludeOutOfStock());
    }
}
