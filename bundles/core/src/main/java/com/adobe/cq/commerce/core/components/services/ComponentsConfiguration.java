/*
 *  Copyright 2020 Adobe. All rights reserved.
 *
 *   This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.adobe.cq.commerce.core.components.services;

import org.apache.sling.api.resource.ValueMap;

/**
 * Simple POJO to store the configuration properties
 */
public final class ComponentsConfiguration {

    private ValueMap internalProperties;

    /**
     * Creates an object of this type.
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
     * @return a {@link ValueMap} object. Note that this is the same object that was used in the constructor
     */
    public ValueMap getValueMap() {
        //TODO we should return a copy.
        return internalProperties;
    }

    /**
     * @see {@link ValueMap#size()}
     */
    public int size() {
        return internalProperties.size();
    }

}
