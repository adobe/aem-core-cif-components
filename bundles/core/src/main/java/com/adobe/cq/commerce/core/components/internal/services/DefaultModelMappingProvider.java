/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2024 Adobe
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
package com.adobe.cq.commerce.core.components.internal.services;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.models.RetrievingModel;
import com.adobe.cq.commerce.core.components.services.ModelMappingProvider;

@Component(service = ModelMappingProvider.class)
@Designate(ocd = DefaultModelMappingProvider.Configuration.class)
public class DefaultModelMappingProvider implements ModelMappingProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultModelMappingProvider.class);

    private Map<String, Class<?>> modelMap;

    @Activate
    public void activate(DefaultModelMappingProvider.Configuration configuration, BundleContext bundleContext) throws Exception {
        String[] modelConfigs = configuration.modelConfiguration();
        if (modelConfigs != null) {
            modelMap = new HashMap<>();
            for (String modelConfig : modelConfigs) {
                String[] parts = modelConfig.split("=");
                if (parts.length == 2) {
                    try {
                        Class<?> clazz = bundleContext.getBundle().loadClass(parts[1]);
                        if (!RetrievingModel.class.isAssignableFrom(clazz)) {
                            LOGGER.error("Class {} does not implement RetrievingModel", parts[1]);
                            continue;
                        }
                        modelMap.put(parts[0], clazz);
                    } catch (ClassNotFoundException e) {
                        LOGGER.error("Could not load class {}", parts[1], e);
                    }
                }
            }
        }
    }

    @Override
    public Map<String, Class<?>> getModels() {
        return Collections.unmodifiableMap(modelMap);
    }

    @ObjectClassDefinition(name = "Commerce Components Model Mapping Provider")
    public @interface Configuration {
        @AttributeDefinition(
            name = "Model Configuration",
            description = "List of model configurations for RetrievingModels.")
        String[] modelConfiguration();
    }
}
