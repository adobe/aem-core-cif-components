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
package com.adobe.cq.commerce.core.components.internal.models.v1.common;

import org.apache.sling.api.resource.ValueMap;

import com.adobe.cq.wcm.core.components.models.Title;
import com.day.cq.wcm.api.designer.Style;

/**
 * Simple class to avoid re-implementing the same method in all the Sling models with a title property
 * for which the HTML tag can be configured.
 */
public class TitleTypeProvider {

    /**
     * The component itself uses the <code>titleType</code> property but the policy uses the
     * <code>type</code> property so we can reuse the
     * <code>core/wcm/components/commons/datasources/allowedheadingelements/v1</code> datasource from the WCM components.
     */
    protected static final String PN_TITLE_TYPE = "titleType";

    /**
     * Returns the HTML tag type for the title element.
     * 
     * @param currentStyle The style (policy) of the component.
     * @param properties The properties of the component.
     * @return The HTML tag type that should be used to display the component title.
     */
    public static String getTitleType(Style currentStyle, ValueMap properties) {
        return properties.get(PN_TITLE_TYPE, currentStyle.get(Title.PN_DESIGN_DEFAULT_TYPE, String.class));
    }
}
