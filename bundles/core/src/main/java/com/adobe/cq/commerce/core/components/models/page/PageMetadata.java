/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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
package com.adobe.cq.commerce.core.components.models.page;

import com.adobe.cq.commerce.core.components.models.productlist.ProductList;

public interface PageMetadata {

    /**
     * @return The content for the meta description tag of the HTML page.
     */
    String getMetaDescription();

    /**
     * @return The content for the meta keywords tag of the HTML page.
     */
    String getMetaKeywords();

    /**
     * Although this method refers to "metaTitle", this is used to set the title tag of the HTML page.
     * The method is not called <code>getTitle()</code> to avoid confusion with {@link ProductList#getTitle()}
     * 
     * @return The content for the title tag of the HTML page.
     */
    String getMetaTitle();

    /**
     * @return The fully-qualified canonical url, to set the canonical link element of the HTML page.
     */
    String getCanonicalUrl();
}
