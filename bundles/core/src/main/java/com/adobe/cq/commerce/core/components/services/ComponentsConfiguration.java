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

package com.adobe.cq.commerce.core.components.services;

import java.util.HashMap;

import org.apache.sling.api.resource.ValueMap;

import com.adobe.cq.commerce.common.ValueMapDecorator;

/**
 * Simple POJO to store the configuration properties
 */
public final class ComponentsConfiguration {

    public static final ComponentsConfiguration EMPTY = new ComponentsConfiguration(ValueMap.EMPTY);

    private ValueMap internalProperties;

    /**
     * Creates an object of this type.
     * 
     * @param vm a {@link ValueMap} containing the properties
     */
    public ComponentsConfiguration(ValueMap vm) {
        this.internalProperties = vm;
    }

    /**
     * @see {@link ValueMap#get(String, Class)}
     */
    public <T> T get(String name, Class<T> type) {
        return internalProperties.get(name, type);
    }

    /**
     * @see {@link ValueMap#get(String, Object)}
     */
    public <T> T get(String name, T defaultValue) {
        return internalProperties.get(name, defaultValue);
    }

    /**
     * Returns a {@link ValueMap} with the configuration properties.
     * 
     * @return a {@link ValueMap} object.
     */
    public ValueMap getValueMap() {
        return new ValueMapDecorator(new HashMap<String, Object>(internalProperties));
    }

    /**
     * @see {@link ValueMap#size()}
     */
    public int size() {
        return internalProperties.size();
    }

}
