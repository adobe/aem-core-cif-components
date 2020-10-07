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
package com.adobe.cq.commerce.core.components.internal.datalayer;

import javax.inject.Inject;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.caconfig.ConfigurationBuilder;

import com.adobe.cq.commerce.core.components.datalayer.CategoryData;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class DataLayerComponent {
    public static final String ID_SEPARATOR = "-";

    @Inject
    protected Resource resource;

    private String id;
    private Boolean dataLayerEnabled;
    private ComponentData componentData;

    private boolean isDataLayerEnabled() {
        if (dataLayerEnabled == null) {
            dataLayerEnabled = false;
            if (resource != null) {
                ConfigurationBuilder builder = resource.adaptTo(ConfigurationBuilder.class);
                if (builder != null) {
                    ValueMap dataLayerConfig = builder
                        .name("com.adobe.cq.wcm.core.components.internal.DataLayerConfig").asValueMap();
                    dataLayerEnabled = dataLayerConfig.get("enabled", false);
                }
            }
        }

        return dataLayerEnabled;
    }

    public ComponentData getData() {
        if (!isDataLayerEnabled()) {
            return null;
        }
        if (componentData == null) {
            componentData = getComponentData();
        }
        return componentData;
    }

    protected ComponentData getComponentData() {
        return new ComponentDataImpl(this, resource);
    }

    protected String generateId() {
        String resourceType = resource.getResourceType();
        String prefix = StringUtils.substringAfterLast(resourceType, "/");
        String path = resource.getPath();
        return StringUtils.join(prefix, ID_SEPARATOR, StringUtils.substring(DigestUtils.sha256Hex(path), 0, 10));
    }

    public String getId() {
        if (id == null) {
            id = generateId();
        }
        return id;
    };

    @JsonIgnore
    public Resource getDataLayerAssetResource() {
        return null;
    }

    @JsonIgnore
    public String getDataLayerType() {
        return null;
    }

    @JsonIgnore
    public String getDataLayerTitle() {
        return null;
    }

    @JsonIgnore
    public String getDataLayerDescription() {
        return null;
    }

    @JsonIgnore
    public String getDataLayerText() {
        return null;
    }

    @JsonIgnore
    public String[] getDataLayerTags() {
        return null;
    }

    @JsonIgnore
    public String getDataLayerUrl() {
        return null;
    }

    @JsonIgnore
    public String getDataLayerLinkUrl() {
        return null;
    }

    @JsonIgnore
    public String getDataLayerTemplatePath() {
        return null;
    }

    @JsonIgnore
    public String getDataLayerLanguage() {
        return null;
    }

    @JsonIgnore
    public String[] getDataLayerShownItems() {
        return null;
    }

    @JsonIgnore
    public String getDataLayerSKU() {
        return null;
    }

    @JsonIgnore
    public Double getDataLayerPrice() {
        return null;
    }

    @JsonIgnore
    public String getDataLayerCurrency() {
        return null;
    }

    @JsonIgnore
    public CategoryData[] getDataLayerCategories() {
        return null;
    }
}
