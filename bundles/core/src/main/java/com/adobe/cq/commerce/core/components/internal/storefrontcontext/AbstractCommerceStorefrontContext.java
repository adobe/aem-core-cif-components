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
package com.adobe.cq.commerce.core.components.internal.storefrontcontext;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.storefrontcontext.CommerceStorefrontContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractCommerceStorefrontContext implements CommerceStorefrontContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCommerceStorefrontContext.class);
    private static final String CONFIG_CLASS = "com.adobe.cq.commerce.core.components.internal.storefrontcontext.CommerceStorefrontContextConfig";

    private final Resource resource;
    private Boolean storefrontContextEnabled;

    public AbstractCommerceStorefrontContext(Resource resource) {
        this.resource = resource;
    }

    private boolean isStorefrontContextEnabled() {
        if (storefrontContextEnabled == null) {
            storefrontContextEnabled = false;
            if (resource != null) {
                ConfigurationBuilder builder = resource.adaptTo(ConfigurationBuilder.class);
                if (builder != null) {
                    ValueMap storefrontContextConfig = builder
                        .name(CONFIG_CLASS).asValueMap();
                    storefrontContextEnabled = storefrontContextConfig.get("enabled", false);
                }
            }
        }

        return storefrontContextEnabled;
    }

    @Override
    public String getJson() {
        if (isStorefrontContextEnabled()) {
            try {
                return new ObjectMapper().writeValueAsString(this);

            } catch (JsonProcessingException e) {
                LOGGER.error("Unable to generate commerce schema JSON string", e);
            }
        }

        return null;
    }
}
