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

package com.adobe.cq.commerce.core.components.models.teaser;

import com.adobe.cq.wcm.core.components.models.Teaser;

public interface CommerceTeaser extends Teaser {

    /**
     * Name of the resource property that stores the Product Slug which is used to build Call-to-Action link for Product Page
     *
     */
    String PN_ACTION_PRODUCT_SLUG = "productSlug";

    /**
     * Name of the resource property that stores Category Id which is used to build Call-to-Action link for Category Page
     *
     */
    String PN_ACTION_CATEGORY_ID = "categoryId";
}
