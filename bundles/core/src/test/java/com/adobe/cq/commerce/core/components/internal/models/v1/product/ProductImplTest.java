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

package com.adobe.cq.commerce.core.components.internal.models.v1.product;

import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.product.Variant;
import com.adobe.cq.commerce.core.components.models.product.VariantAttribute;
import com.adobe.cq.commerce.magento.graphql.ComplexTextValue;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptions;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptionsValues;
import com.adobe.cq.commerce.magento.graphql.ConfigurableVariant;
import com.adobe.cq.commerce.magento.graphql.CurrencyEnum;
import com.adobe.cq.commerce.magento.graphql.MediaGalleryEntry;
import com.adobe.cq.commerce.magento.graphql.ProductStockStatus;
import com.adobe.cq.commerce.magento.graphql.SimpleProduct;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductImplTest {

    ProductImpl slingModel;

    @Before
    public void setup() {
        this.slingModel = new ProductImpl();

        // Assets
        MediaGalleryEntry assetA = mock(MediaGalleryEntry.class);
        when(assetA.getLabel()).thenReturn("Image A");
        when(assetA.getPosition()).thenReturn(200);
        when(assetA.getMediaType()).thenReturn("image");
        when(assetA.getDisabled()).thenReturn(false);

        MediaGalleryEntry assetB = mock(MediaGalleryEntry.class);
        when(assetB.getLabel()).thenReturn("Image B");
        when(assetB.getPosition()).thenReturn(100);
        when(assetB.getMediaType()).thenReturn("image");
        when(assetB.getDisabled()).thenReturn(false);

        MediaGalleryEntry assetC = mock(MediaGalleryEntry.class);
        when(assetC.getPosition()).thenReturn(150);
        when(assetC.getMediaType()).thenReturn("video");
        when(assetC.getDisabled()).thenReturn(false);

        MediaGalleryEntry assetD = mock(MediaGalleryEntry.class);
        when(assetD.getPosition()).thenReturn(400);
        when(assetD.getMediaType()).thenReturn("image");
        when(assetD.getDisabled()).thenReturn(true);

        // Variant attributes
        ConfigurableProductOptionsValues valueA = mock(ConfigurableProductOptionsValues.class);
        when(valueA.getLabel()).thenReturn("Value A");
        ConfigurableProductOptionsValues valueB = mock(ConfigurableProductOptionsValues.class);
        when(valueB.getLabel()).thenReturn("Value B");

        ConfigurableProductOptions variantAttribute = mock(ConfigurableProductOptions.class);
        when(variantAttribute.getLabel()).thenReturn("Option A");
        when(variantAttribute.getValues()).thenReturn(Arrays.asList(valueA, valueB));

        // Variant
        SimpleProduct gqlVariantProduct = mock(SimpleProduct.class, RETURNS_DEEP_STUBS);
        when(gqlVariantProduct.getPrice().getRegularPrice().getAmount().getCurrency()).thenReturn(CurrencyEnum.USD);
        when(gqlVariantProduct.getPrice().getRegularPrice().getAmount().getValue()).thenReturn(1234.5678d);
        when(gqlVariantProduct.getStockStatus()).thenReturn(ProductStockStatus.IN_STOCK);
        when(gqlVariantProduct.getMediaGalleryEntries()).thenReturn(Collections.emptyList());
        ConfigurableVariant gqlVariant = mock(ConfigurableVariant.class);
        when(gqlVariant.getProduct()).thenReturn(gqlVariantProduct);

        // Base product
        ConfigurableProduct gqlProduct = mock(ConfigurableProduct.class);
        when(gqlProduct.getMediaGalleryEntries()).thenReturn(Arrays.asList(assetA, assetB, assetC, assetD));
        when(gqlProduct.getVariants()).thenReturn(Collections.singletonList(gqlVariant));
        when(gqlProduct.getConfigurableOptions()).thenReturn(Collections.singletonList(variantAttribute));
        Whitebox.setInternalState(this.slingModel, "product", gqlProduct);

        // Set number formatter
        Whitebox.setInternalState(this.slingModel, "priceFormatter", NumberFormat.getCurrencyInstance(Locale.US));
    }

    @Test
    public void testGetVariantsNotConfigurableProduct() {
        SimpleProduct gqlProduct = mock(SimpleProduct.class);
        Whitebox.setInternalState(this.slingModel, "product", gqlProduct);

        List<Variant> variants = this.slingModel.getVariants();
        Assert.assertNotNull(variants);
        Assert.assertEquals(0, variants.size());
    }

    @Test
    public void testGetVariants() {
        List<Variant> variants = this.slingModel.getVariants();
        Assert.assertNotNull(variants);
        Assert.assertEquals(1, variants.size());

        Variant firstVariant = variants.get(0);
        Assert.assertEquals("$1,234.57", firstVariant.getFormattedPrice());
        Assert.assertTrue(firstVariant.getInStock());
    }

    @Test
    public void testGetAssets() {
        Assert.assertEquals(2, slingModel.getAssets().size());
        Assert.assertEquals("Image B", slingModel.getAssets().get(0).getLabel());
        Assert.assertEquals("Image A", slingModel.getAssets().get(1).getLabel());
    }

    @Test
    public void testGetVariantAttributesNotConfigurableProduct() {
        SimpleProduct gqlProduct = mock(SimpleProduct.class);
        Whitebox.setInternalState(this.slingModel, "product", gqlProduct);

        List<VariantAttribute> attributes = this.slingModel.getVariantAttributes();
        Assert.assertNotNull(attributes);
        Assert.assertEquals(0, attributes.size());
    }

    @Test
    public void testGetVariantAttributes() {
        List<VariantAttribute> attributes = this.slingModel.getVariantAttributes();

        Assert.assertNotNull(attributes);
        Assert.assertEquals(1, attributes.size());

        VariantAttribute first = attributes.get(0);
        Assert.assertEquals("Option A", first.getLabel());
        Assert.assertEquals(2, first.getValues().size());
        Assert.assertEquals("Value A", first.getValues().get(0).getLabel());
        Assert.assertEquals("Value B", first.getValues().get(1).getLabel());
    }

    @Test
    public void testSafeDescriptionWithNull() {
        SimpleProduct product = mock(SimpleProduct.class, RETURNS_DEEP_STUBS);
        when(product.getDescription()).thenReturn(null);

        Whitebox.setInternalState(this.slingModel, "product", product);

        Assert.assertNull(this.slingModel.getDescription());
    }

    @Test
    public void testSafeDescriptionHtmlNull() {
        SimpleProduct product = mock(SimpleProduct.class, RETURNS_DEEP_STUBS);
        ComplexTextValue value = mock(ComplexTextValue.class, RETURNS_DEEP_STUBS);
        when(value.getHtml()).thenReturn(null);
        when(product.getDescription()).thenReturn(value);

        Whitebox.setInternalState(this.slingModel, "product", product);

        Assert.assertNull(this.slingModel.getDescription());
    }

    @Test
    public void testSafeDescription() {
        String sampleString = "<strong>abc</strong>";
        SimpleProduct product = mock(SimpleProduct.class, RETURNS_DEEP_STUBS);
        ComplexTextValue value = mock(ComplexTextValue.class, RETURNS_DEEP_STUBS);
        when(value.getHtml()).thenReturn(sampleString);
        when(product.getDescription()).thenReturn(value);

        Whitebox.setInternalState(this.slingModel, "product", product);

        Assert.assertEquals(sampleString, this.slingModel.getDescription());
    }

}
