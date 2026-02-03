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
package com.adobe.cq.commerce.core.components.models.teaser;

import com.adobe.cq.wcm.core.components.models.Teaser;

public interface CommerceTeaser extends Teaser {

    /**
     * Name of the resource property that stores the product url_key which is used to build call-to-action link for product page
     * Used by V1 of the CommerceTeaser component.
     */
    String PN_ACTION_PRODUCT_SLUG = "productSlug";

    /**
     * Name of the resource property that stores the product SKU which is used to build call-to-action link for product page
     * Used by V2 of the CommerceTeaser component.
     */
    String PN_ACTION_PRODUCT_SKU = "productSku";

    /**
     * Name of the resource property that stores category identifier which is used to build call-to-action link for category page
     *
     */
    String PN_ACTION_CATEGORY_ID = "categoryId";

    /**
     * Name of the resource property that stores category id type which is used to build call-to-action link for category page
     *
     */
    String PN_ACTION_CATEGORY_ID_TYPE = "categoryIdType";

    /**
     * Retrieves the URL of the image associated with this teaser.
     * 
     * @return A String representing the URL or {@code null} if there is not image associated with the teaser
     */
    String getAssetPath();
}
