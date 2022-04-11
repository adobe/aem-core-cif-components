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
package com.adobe.cq.commerce.core.components.internal.models.v3.product;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;

import com.adobe.cq.commerce.core.components.internal.models.v1.product.VariantAttributeImpl;
import com.adobe.cq.commerce.core.components.internal.models.v1.product.VariantValueImpl;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.product.Variant;
import com.adobe.cq.commerce.core.components.models.product.VariantAttribute;
import com.adobe.cq.commerce.core.components.models.product.VariantValue;
import com.adobe.cq.commerce.magento.graphql.ConfigurableAttributeOption;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptions;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptionsValues;
import com.adobe.cq.commerce.magento.graphql.ConfigurableVariant;

@Model(adaptables = SlingHttpServletRequest.class, adapters = Product.class, resourceType = ProductImpl.RESOURCE_TYPE)
public class ProductImpl extends com.adobe.cq.commerce.core.components.internal.models.v2.product.ProductImpl
    implements Product {

    public static final String RESOURCE_TYPE = "core/cif/components/commerce/product/v3/product";

    @PostConstruct
    protected void initModel() {
        super.initModel();
        if (productRetriever != null) {
            productRetriever.extendProductQueryWith(p -> p.onConfigurableProduct(cp -> cp
                .configurableOptions(o -> o
                    .values(v -> v.uid()))
                .variants(v -> v
                    .attributes(a -> a.uid()))));
        }
    }

    @Override
    protected VariantValue mapVariantValue(ConfigurableProductOptionsValues value) {
        VariantValueImpl variantValue = new VariantValueImpl();
        variantValue.setUid(value.getUid().toString());
        variantValue.setLabel(value.getLabel());

        return variantValue;
    }

    @Override
    protected VariantAttribute mapVariantAttribute(ConfigurableProductOptions option) {
        // Get list of values
        List<VariantValue> values = option.getValues().parallelStream().map(this::mapVariantValue)
            .collect(Collectors.toList());

        // Create attribute map
        VariantAttributeImpl attribute = new VariantAttributeImpl();
        attribute.setLabel(option.getLabel());
        attribute.setId(option.getAttributeCode());
        attribute.setValues(values);

        return attribute;
    }

    @Override
    protected Variant mapVariant(ConfigurableVariant variant) {
        Variant mappedVariant = super.mapVariant(variant);

        // Map variant attributes
        for (ConfigurableAttributeOption option : variant.getAttributes()) {
            mappedVariant.getVariantAttributesUid().put(option.getCode(), option.getUid().toString());
        }

        return mappedVariant;
    }
}
