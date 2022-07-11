/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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
package com.adobe.cq.commerce.core.components.models.product;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * VariantValue is a view model interface representing a possible value for
 * a VariantAttribute.
 */
@ConsumerType
public interface VariantValue {
    enum SwatchType {
        IMAGE,
        TEXT,
        COLOR
    }

    String getLabel();

    default String getCssClassModifier() {
        return null;
    }

    Integer getId();

    default String getUid() {
        return null;
    }

    default SwatchType getSwatchType() {
        return null;
    }
}
