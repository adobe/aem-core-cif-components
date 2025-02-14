/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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
package com.adobe.cq.commerce.core.components.models.product;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.annotation.versioning.ConsumerType;

import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.page.PageMetadata;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.adobe.cq.commerce.core.components.storefrontcontext.ProductStorefrontContext;
import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Product is the sling model interface for the CIF core product component.
 */
@ConsumerType
public interface Product extends Component, PageMetadata {

    String TITLE_SECTION = "TITLE";
    String PRICE_SECTION = "PRICE";
    String SKU_SECTION = "SKU";
    String IMAGE_SECTION = "IMAGE";
    String OPTIONS_SECTION = "OPTIONS";
    String QUANTITY_SECTION = "QUANTITY";
    String ACTIONS_SECTION = "ACTIONS";
    String DESCRIPTION_SECTION = "DESCRIPTION";
    String DETAILS_SECTION = "DETAILS";

    /**
     * Name of the boolean resource property indicating if the product component should load prices on the client-side.
     */
    String PN_LOAD_CLIENT_PRICE = "loadClientPrice";

    Boolean getFound();

    String getName();

    String getDescription();

    String getSku();

    Price getPriceRange();

    Boolean getInStock();

    Boolean isConfigurable();

    Boolean isGroupedProduct();

    Boolean isVirtualProduct();

    Boolean isBundleProduct();

    default Boolean isGiftCardProduct() {
        return false;
    }

    /**
     * The version 1 of the product component always returns <code>false</code> as it does not support this feature.
     * The version 2 of the product component does support this feature but it requires a Magento EE instance with
     * at least Magento version 2.4.2.
     *
     * @return <code>true</code> if the product data contains staged changes, <code>false</code> otherwise.
     * @since com.adobe.cq.commerce.core.components.models.product 3.1.0
     */
    default Boolean isStaged() {
        return false;
    }

    String getVariantsJson();

    /**
     * Returns a JSON-LD representation of the product.
     *
     * @return a JSON-LD string for the product
     */
    default String getJsonLd() {
        return null;
    }

    List<Variant> getVariants();

    List<GroupItem> getGroupedProductItems();

    List<Asset> getAssets();

    String getAssetsJson();

    List<VariantAttribute> getVariantAttributes();

    /**
     * @return
     * @deprecated Per component client-side price loading is deprecated. This information is exposed in the
     *             {@link com.adobe.cq.commerce.core.components.models.storeconfigexporter.StoreConfigExporter} and enabled site wide.
     */
    @Deprecated
    Boolean loadClientPrice();

    AbstractProductRetriever getProductRetriever();

    /**
     * Return the product storefront context
     *
     * @return context of the product
     */
    ProductStorefrontContext getStorefrontContext();

    /**
     * Returns true when the Add to Wish List button is enabled.
     *
     * @return
     */
    default boolean getAddToWishListEnabled() {
        return false;
    }

    /**
     * Returns a set of sections to be displayed.
     *
     * @return
     */
    default Set<String> getVisibleSections() {
        Set<String> defaultSections = new HashSet<>();
        defaultSections.add(TITLE_SECTION);
        defaultSections.add(PRICE_SECTION);
        defaultSections.add(SKU_SECTION);
        defaultSections.add(IMAGE_SECTION);
        defaultSections.add(OPTIONS_SECTION);
        defaultSections.add(QUANTITY_SECTION);
        defaultSections.add(ACTIONS_SECTION);
        defaultSections.add(DESCRIPTION_SECTION);
        defaultSections.add(DETAILS_SECTION);
        return defaultSections;
    }
}
