/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe Systems Incorporated
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
package com.adobe.cq.commerce.core.components.models.teaser;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

import com.adobe.cq.wcm.core.components.models.ListItem;

@ProviderType
public interface CifTeaser {

    /**
     * Name of the resource property that defines whether or not the teaser has Call-to-Action elements
     *
     * @since com.adobe.cq.wcm.core.components.models 12.4.0
     */
    String PN_ACTIONS_ENABLED = "actionsEnabled";

    /**
     * Name of the child node where the Call-to-Action elements are stored
     *
     * @since com.adobe.cq.wcm.core.components.models 12.4.0
     */
    String NN_ACTIONS = "actions";

    /**
     * Name of the resource property that stores the Call-to-Action text
     *
     * @since com.adobe.cq.wcm.core.components.models 12.4.0
     */
    String PN_ACTION_TEXT = "text";

    /**
     * Name of the resource property that stores the Call-to-Action link for Product Page
     *
     * @since com.adobe.cq.wcm.core.components.models 12.4.0
     */
    String PN_ACTION_PRODUCT_SKU = "productSKU";

    /**
     * Name of the resource property that stores the Call-to-Action link for Category Page
     *
     * @since com.adobe.cq.wcm.core.components.models 12.4.0
     */
    String PN_ACTION_CATEGORY_ID = "categoryId";

    /**
     * Name of the resource property that defines whether or not the title value is taken from the linked page.
     *
     * @since com.adobe.cq.wcm.core.components.models 12.4.0
     */
    String PN_TITLE_FROM_PAGE = "titleFromPage";

    /**
     * Name of the resource property that defines whether or not the description value is taken from the linked page.
     *
     * @since com.adobe.cq.wcm.core.components.models 12.4.0
     */
    String PN_DESCRIPTION_FROM_PAGE = "descriptionFromPage";

    /**
     * Checks if the teaser has Call-to-Action elements
     *
     * @return {@code true} if teaser has CTAs, {@code false} otherwise
     * @since com.adobe.cq.wcm.core.components.models 12.4.0
     */
    default boolean isActionsEnabled() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the list of Call-to-Action elements
     *
     * @return the list of CTAs
     * @since com.adobe.cq.wcm.core.components.models 12.4.0
     */
    default List<ListItem> getActions() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns this teaser's title, if one was defined.
     *
     * @return the teaser's title or {@code null}
     * @since com.adobe.cq.wcm.core.components.models 12.4.0
     */
    default String getTitle() {
        throw new UnsupportedOperationException();
    }
}
