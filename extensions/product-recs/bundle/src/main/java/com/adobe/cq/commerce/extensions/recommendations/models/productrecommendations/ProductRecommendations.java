/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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
package com.adobe.cq.commerce.extensions.recommendations.models.productrecommendations;

import org.osgi.annotation.versioning.ConsumerType;

import com.adobe.cq.commerce.extensions.recommendations.models.common.PriceRange;
import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Sling model for a product recommendation component
 * The model holds all the configured options
 */
@ConsumerType
public interface ProductRecommendations extends Component {

    boolean getPreconfigured();

    String getTitle();

    String getRecommendationType();

    String getCategoryInclusions();

    String getCategoryExclusions();

    PriceRange getPriceRangeInclusions();

    PriceRange getPriceRangeExclusions();

    /**
     * Returns {@code true} when the product recommendations component should show an Add to Wish List button.
     *
     * @return {@code true} when the Add to Wish List button is enabled, {@code false} otherwise
     */
    default boolean getAddToWishListEnabled() {
        return false;
    }

}
