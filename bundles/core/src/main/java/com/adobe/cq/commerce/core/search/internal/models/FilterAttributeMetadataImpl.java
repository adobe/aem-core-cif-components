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

import com.adobe.cq.commerce.core.search.models.FilterAttributeMetadata;

public class FilterAttributeMetadataImpl implements FilterAttributeMetadata {

    /**
     * An input of type select.
     */
    public static final String INPUT_TYPE_SELECT = "select";
    /**
     * An input of type text.
     */
    public static final String INPUT_TYPE_TEXT = "text";
    /**
     * An input of type select.
     */
    public static final String INPUT_TYPE_BOOLEAN = "boolean";
    /**
     * An input of type select.
     */
    public static final String INPUT_TYPE_PRICE = "price";
    /**
     * An attribute of type Int.
     */
    public static final String ATTRIBUTE_TYPE_INT = "Int";
    /**
     * An attribute of type Int.
     */
    public static final String ATTRIBUTE_TYPE_STRING = "String";
    /**
     * An attribute of type Float.
     */
    public static final String ATTRIBUTE_TYPE_FLOAT = "Float";

    private String filterInputType;
    private String attributeCode;
    private String attributeInputType;
    private String attributeType;

    @Override
    public String getFilterInputType() {
        return filterInputType;
    }

    public void setFilterInputType(final String filterInputType) {
        this.filterInputType = filterInputType;
    }

    @Override
    public String getAttributeCode() {
        return attributeCode;
    }

    public void setAttributeCode(final String attributeCode) {
        this.attributeCode = attributeCode;
    }

    @Override
    public String getAttributeInputType() {
        return attributeInputType;
    }

    public void setAttributeInputType(final String attributeInputType) {
        this.attributeInputType = attributeInputType;
    }

    @Override
    public String getAttributeType() {
        return attributeType;
    }

    public void setAttributeType(final String attributeType) {
        this.attributeType = attributeType;
    }
}
