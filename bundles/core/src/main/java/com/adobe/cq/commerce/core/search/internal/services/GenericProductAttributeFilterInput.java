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
package com.adobe.cq.commerce.core.search.internal.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.FilterMatchTypeInput;
import com.adobe.cq.commerce.magento.graphql.FilterRangeTypeInput;
import com.adobe.cq.commerce.magento.graphql.ProductAttributeFilterInput;
import com.shopify.graphql.support.Input;

/**
 * @deprecated Use {@link ProductAttributeFilterInput#setCustomFilter} instead.
 */
@Deprecated
public class GenericProductAttributeFilterInput extends ProductAttributeFilterInput {

    private Map<String, Input<FilterEqualTypeInput>> equalInputs = new HashMap<>();

    private Map<String, Input<FilterMatchTypeInput>> matchInputs = new HashMap<>();

    private Map<String, Input<FilterRangeTypeInput>> rangeInputs = new HashMap<>();

    public void addEqualTypeInput(final String key, final FilterEqualTypeInput input) {
        equalInputs.put(key, Input.optional(input));
    }

    public void addMatchTypeInput(final String key, final FilterMatchTypeInput input) {
        matchInputs.put(key, Input.optional(input));
    }

    public void addRangeTypeInput(final String key, final FilterRangeTypeInput input) {
        rangeInputs.put(key, Input.optional(input));
    }

    public void appendTo(StringBuilder _queryBuilder) {
        String separator = "";
        _queryBuilder.append('{');

        for (Entry<String, Input<FilterEqualTypeInput>> input : equalInputs.entrySet()) {
            _queryBuilder.append(separator);
            separator = ",";
            _queryBuilder.append(input.getKey() + ":");
            if ((input.getValue() != null) && (input.getValue().getValue() != null)) {
                input.getValue().getValue().appendTo(_queryBuilder);
            } else {
                _queryBuilder.append("null");
            }
        }

        for (Entry<String, Input<FilterRangeTypeInput>> input : rangeInputs.entrySet()) {
            _queryBuilder.append(separator);
            separator = ",";
            _queryBuilder.append(input.getKey() + ":");
            if ((input.getValue() != null) && (input.getValue().getValue() != null)) {
                input.getValue().getValue().appendTo(_queryBuilder);
            } else {
                _queryBuilder.append("null");
            }
        }

        for (Entry<String, Input<FilterMatchTypeInput>> input : matchInputs.entrySet()) {
            _queryBuilder.append(separator);
            separator = ",";
            _queryBuilder.append(input.getKey() + ":");
            if ((input.getValue() != null) && (input.getValue().getValue() != null)) {
                input.getValue().getValue().appendTo(_queryBuilder);
            } else {
                _queryBuilder.append("null");
            }
        }

        _queryBuilder.append('}');
    }
}
