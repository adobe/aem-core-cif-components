/*
 *  Copyright 2021 Adobe. All rights reserved.
 *
 *   This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.adobe.cq.commerce.core.components.models.teaser;

import com.adobe.cq.wcm.core.components.models.ListItem;

public interface CommerceTeaserActionItem extends ListItem {

    /**
     * Returns the category id associated with this teaser action.
     * 
     * @return a String representing the category or <code>null</code> if no category id is configured
     */
    String getCategoryId();

    /**
     * Returns the product slung associated with this teaser action.
     * 
     * @return a String representing the product slug or <code>null</code> if no product slug is configured
     */
    String getProductSlug();

}
