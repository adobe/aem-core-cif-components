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
package com.adobe.cq.commerce.core.components.models.productlist;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.osgi.annotation.versioning.ConsumerType;

import com.adobe.cq.commerce.core.components.models.experiencefragment.CommerceExperienceFragmentContainer;
import com.adobe.cq.commerce.core.components.models.page.PageMetadata;
import com.adobe.cq.commerce.core.components.models.productcollection.ProductCollection;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoryRetriever;
import com.adobe.cq.commerce.core.components.storefrontcontext.CategoryStorefrontContext;
import com.adobe.cq.wcm.core.components.models.Component;

@ConsumerType
public interface ProductList extends Component, ProductCollection, PageMetadata {

    /**
     * Name of the boolean resource property indicating if the product list should render the category title.
     */
    String PN_SHOW_TITLE = "showTitle";

    /**
     * Name of the boolean resource property indicating if the product list should render the category image.
     */
    String PN_SHOW_IMAGE = "showImage";

    /**
     * Name of the child node where the fragment elements are stored
     */
    String NN_FRAGMENTS = "fragments";

    /**
     * Returns {@code true} if the category / product list title should be rendered.
     *
     * @return {@code true} if category / product list title should be shown, {@code false} otherwise
     */
    boolean showTitle();

    /**
     * Returns the title of this {@code ProductList}.
     *
     * @return the title of this list item or {@code null}
     */
    @Nullable
    String getTitle();

    String getImage();

    boolean showImage();

    /**
     * Returns in instance of the category retriever for fetching category data via GraphQL.
     *
     * @return category retriever instance
     */
    AbstractCategoryRetriever getCategoryRetriever();

    /**
     * The version 1 of the productlist component always returns <code>false</code> as it does not support this feature.
     * The version 2 of the productlist component does support this feature but it requires a Magento EE instance with
     * at least Magento version 2.4.2.
     * 
     * @return <code>true</code> if the product data contains staged changes, <code>false</code> otherwise.
     * @since com.adobe.cq.commerce.core.components.models.productlist 3.2.0
     */
    default Boolean isStaged() {
        return false;
    };

    /**
     * Return the categories storefront context for a list of products
     *
     * @return context of the categories in the product list
     */
    CategoryStorefrontContext getStorefrontContext();

    /**
     * Return the experience fragment resources that match the configured locations for the current category
     *
     * @return a list of experience fragment resources
     */
    default List<CommerceExperienceFragmentContainer> getExperienceFragments() {
        return Collections.emptyList();
    }
}
