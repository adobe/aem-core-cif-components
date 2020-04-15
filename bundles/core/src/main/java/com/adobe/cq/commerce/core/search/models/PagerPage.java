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

package com.adobe.cq.commerce.core.search.models;

import java.util.Map;

/**
 * Contains metadata required to support display of pagination options.
 */
public interface PagerPage {

    /**
     * The number of this page.
     *
     * @return the page number, 1 based index
     */
    int getPageNumber();

    /**
     * Get the parameters for this page.
     *
     * @return the key value pair parameters for this page
     */
    Map<String, String> getParameters();

    /**
     * Whether this particular page should be displayed in the pager.
     *
     * @return true if should be displayed, false if intended to be hidden
     */
    boolean isDisplayed();

}
