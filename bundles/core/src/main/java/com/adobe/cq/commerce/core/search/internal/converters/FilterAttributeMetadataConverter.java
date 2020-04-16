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

package com.adobe.cq.commerce.core.search.internal.converters;

import java.util.List;
import java.util.function.Function;

import com.adobe.cq.commerce.core.search.internal.models.FilterAttributeMetadataImpl;
import com.adobe.cq.commerce.core.search.models.FilterAttributeMetadata;
import com.adobe.cq.commerce.magento.graphql.Attribute;
import com.adobe.cq.commerce.magento.graphql.__InputValue;

/**
 * This class converts a Magento InputField into a more usable and metadata-enriched GraphQL package independent class.
 */
public class FilterAttributeMetadataConverter implements Function<__InputValue, FilterAttributeMetadata> {

    private List<Attribute> allAttributeMetadata;

    /**
     * Constructor for fitler attribute metadata convert.
     *
     * @param allAttributeMetadata metadata for all attributes
     */
    public FilterAttributeMetadataConverter(final List<Attribute> allAttributeMetadata) {
        this.allAttributeMetadata = allAttributeMetadata;
    }

    @Override
    public FilterAttributeMetadata apply(final __InputValue inputField) {
        FilterAttributeMetadataImpl metadata = new FilterAttributeMetadataImpl();

        metadata.setAttributeCode(inputField.getName());
        metadata.setFilterInputType(inputField.getType().getName());

        allAttributeMetadata.stream()
            .filter(attribute -> inputField.getName().equals(attribute.getAttributeCode()))
            .findFirst().ifPresent(attribute -> {
                metadata.setAttributeType(attribute.getAttributeType());
                metadata.setAttributeInputType(attribute.getInputType());
            });

        return metadata;
    }

}
