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
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Rule;
import org.junit.Test;

import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ProductRecommendationsImplTest {

    private ProductRecommendationsImpl productRecommendations;

    private static final String PRODUCT_RECS_PATH = "/content/landingPage/jcr:content/root/responsivegrid/product-recs";
    private static final String EMPTY_PRODUCT_RECS_PATH = "/content/landingPage/jcr:content/root/responsivegrid/product-recs";

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");

    private static AemContext createContext(String contentPath) {
        return new AemContext((AemContextCallback) context -> {
            // Load page structure
            context.load().json(contentPath, "/content");
        }, ResourceResolverType.JCR_MOCK);
    }

    private void setupTest(String componentPath) {
        // Mock resource and resolver
        Resource resource = context.resourceResolver().getResource(componentPath);
        context.currentResource(resource);
        productRecommendations = context.request().adaptTo(ProductRecommendationsImpl.class);
    }

    @Test
    public void testEmptyTitle() {
        setupTest(EMPTY_PRODUCT_RECS_PATH);
        assertEquals("Recommended products", productRecommendations.getTitle());
    }

    @Test
    public void testFiltersEnabled() {
        setupTest(PRODUCT_RECS_PATH);
        assertNull(productRecommendations.getCategoryExclusions());
        assertNotNull(productRecommendations.getCategoryInclusions());
        assertEquals("shorts-men,pants-men", productRecommendations.getCategoryInclusions());
    }

    @Test
    public void testPriceRange() {
        setupTest(PRODUCT_RECS_PATH);
        assertNotNull(productRecommendations.getPriceRangeExclusions());
        assertNotNull(productRecommendations.getPriceRangeInclusions());
    }

    @Test
    public void testStringProperties() {
        setupTest(PRODUCT_RECS_PATH);
        assertEquals("Recommended products", productRecommendations.getTitle());
        assertEquals("most-viewed", productRecommendations.getRecommendationType());
    }
}
