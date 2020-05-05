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

package com.adobe.cq.commerce.core.search.internal.models;

import java.util.Map;

import com.adobe.cq.commerce.core.search.models.PagerPage;

/**
 * A simple class for storing pagination page information.
 */
public class PagerPageImpl implements PagerPage {

    private int pageNumber;
    private Map<String, String> parameters;
    private boolean displayed;

    public PagerPageImpl(final int pageNumber, final Map<String, String> parameters, final boolean displayed) {
        this.pageNumber = pageNumber;
        this.parameters = parameters;
        this.displayed = displayed;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public boolean isDisplayed() {
        return displayed;
    }

}
