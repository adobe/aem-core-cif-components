/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.components.internal.models.v2.product;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.testing.Utils;

public class ProductImplTest extends com.adobe.cq.commerce.core.components.internal.models.v1.product.ProductImplTest {

    private ProductImpl productModel;

    @Test
    public void testProductIsStaged() {
        productModel = context.request().adaptTo(ProductImpl.class);
        Assert.assertTrue("The product has staged data", productModel.isStaged());
    }

    @Test
    public void testGroupedProductIsStaged() throws IOException {
        Utils.setupHttpResponse("graphql/magento-graphql-groupedproduct-result.json", httpClient, 200);
        productModel = context.request().adaptTo(ProductImpl.class);
        Assert.assertTrue(productModel.isGroupedProduct());
        Assert.assertTrue("The product has staged data", productModel.isStaged());
    }

    @Test
    public void testBundleProductIsStaged() throws IOException {
        Utils.setupHttpResponse("graphql/magento-graphql-bundleproduct-result.json", httpClient, 200);
        productModel = context.request().adaptTo(ProductImpl.class);
        Assert.assertTrue(productModel.isBundleProduct());
        Assert.assertTrue("The product has staged data", productModel.isStaged());
    }

    @Test
    public void testVirtualProductIsNotStaged() throws IOException {
        Utils.setupHttpResponse("graphql/magento-graphql-virtualproduct-result.json", httpClient, 200);
        productModel = context.request().adaptTo(ProductImpl.class);
        Assert.assertTrue(productModel.isVirtualProduct());
        Assert.assertFalse("The product doesn't have staged data", productModel.isStaged());
    }
}
