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
package com.adobe.cq.commerce.core.components.services;

import java.util.HashMap;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Simple POJO to store the configuration properties
 */
@ProviderType
public final class ComponentsConfiguration {

    public static final ComponentsConfiguration EMPTY = new ComponentsConfiguration(ValueMap.EMPTY);

    private ValueMap internalProperties;

    /**
     * Creates an object of this type.
     * 
     * @param vm a {@link ValueMap} containing the properties
     */
    public ComponentsConfiguration(ValueMap vm) {
        this.internalProperties = new ValueMapDecorator(new HashMap<String, Object>(vm));
    }

    /**
     * @param <T> The class type of the property being fetched.
     * @param name The name of the property.
     * @param type The class of the type.
     * @return Return named value converted to type T or <code>null</code> if non existing or can't be converted.
     */
    public <T> T get(String name, Class<T> type) {
        return internalProperties.get(name, type);
    }

    /**
     * @param <T> The class type of the property being fetched.
     * @param name The name of the property.
     * @param defaultValue The default value to use if the named property does not exist or cannot be converted to the requested type.
     * @return Return named value converted to type T or the default value if non existing or can't be converted.
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
     * @return The number of properties in this configuration.
     */
    public int size() {
        return internalProperties.size();
    }

}
