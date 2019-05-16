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
package com.adobe.cq.commerce.core.components.models.productteaser;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Product Teaser is the sling model interface for the CIF Teaser component.
 */
@ProviderType
public interface ProductTeaser {

    /**
     * Returns name of the configured Product for this {@code ProductTeaser}
     *
     * @return name of the configured Product for this Teaser of {@code null}
     */
    String getName();


    /**
     * Returns formatted price string with currency for this {@code ProductTeaser}
     *
     * @return formatted price string with currency or {@code null}
     */
    String getFormattedPrice();

    /**
     * Returns url of swatch image of the product for display for this {@code ProductTeaser}
     *
     * @return url of the swatch image for the product or {@code null}
     */
    String getImage();

    /**
     * Return the url of the product page for this {@code ProductTeaser}
     *
     * @return the url of the product page of the configured product or {@code null}
     */
    String getUrl();


}
