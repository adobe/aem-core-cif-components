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

/**
 * Contains information about search filters useful in places where filter metadata is required.
 */
public interface FilterAttributeMetadata {

    /**
     * Get the type of the filter.
     *
     * @return string representing the type of filter
     */
    String getFilterInputType();

    /**
     * Get the attribute code for this filterable attribute.
     *
     * @return the attribute code or name of the attribute
     */
    String getAttributeCode();

    /**
     * The input type for the attribute input in the commerce system admin / adminhtml.
     *
     * @return the input type of the input in the commerce system admin.
     */
    String getAttributeInputType();

    /**
     * The type of the attribute in the commerce system.
     *
     * @return the attribute type
     */
    String getAttributeType();

}
