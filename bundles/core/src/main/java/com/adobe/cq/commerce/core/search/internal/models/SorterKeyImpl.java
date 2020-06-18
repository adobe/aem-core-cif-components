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

package com.adobe.cq.commerce.core.search.internal.models;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.adobe.cq.commerce.core.search.models.Sorter;
import com.adobe.cq.commerce.core.search.models.SorterKey;

public class SorterKeyImpl implements SorterKey {
    private final String name;
    private final String label;
    private Map<String, String> currentOrderParameters;
    private Map<String, String> oppositeOrderParameters;
    private boolean selected;
    private Sorter.Order order;

    public SorterKeyImpl(String name, String label) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("The name for sorter key is empty");
        }
        if (StringUtils.isBlank(label)) {
            throw new IllegalArgumentException("The label for sorter key is empty");
        }

        this.name = name;
        this.label = label;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public Sorter.Order getOrder() {
        return order;
    }

    public void setOrder(Sorter.Order order) {
        this.order = order;
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public Map<String, String> getCurrentOrderParameters() {
        return currentOrderParameters;
    }

    public void setCurrentOrderParameters(Map<String, String> currentOrderParameters) {
        this.currentOrderParameters = currentOrderParameters;
    }

    @Override
    public Map<String, String> getOppositeOrderParameters() {
        return oppositeOrderParameters;
    }

    public void setOppositeOrderParameters(Map<String, String> oppositeOrderParameters) {
        this.oppositeOrderParameters = oppositeOrderParameters;
    }
}
