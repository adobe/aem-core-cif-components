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
package com.adobe.cq.commerce.core.components.internal.models.v2.product;

import java.io.IOException;

public class ProductImplTest extends com.adobe.cq.commerce.core.components.internal.models.v1.product.ProductImplTest {

    @Override
    protected void adaptToProduct() {
        // This ensures we re-run all the unit tests with version 2 of ProductImpl
        productModel = context.request().adaptTo(ProductImpl.class);
    }

    @Override
    public void testProduct() {
        testProductImpl(true);
    }

    @Override
    public void testGroupedProduct() throws IOException {
        testGroupedProductImpl(true);
    }

    @Override
    public void testBundleProduct() throws IOException {
        testBundleProductImpl(true);
    }
}
