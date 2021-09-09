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
package com.adobe.cq.commerce.core.components.internal.models.v1.page;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.internal.models.v1.storeconfigexporter.StoreConfigExporterImpl;
import com.adobe.cq.wcm.core.components.models.HtmlPageItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class StoreConfigHtmlPageItem implements HtmlPageItem {

    private static final String NAME = "store-config";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_CONTENT = "content";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(StoreConfigHtmlPageItem.class);

    private final Map<String, String> attributes = new HashMap<>();

    StoreConfigHtmlPageItem(StoreConfigExporterImpl storeConfigExporter) {
        try {
            attributes.put(ATTR_NAME, NAME);
            attributes.put(ATTR_CONTENT, OBJECT_MAPPER.writeValueAsString(storeConfigExporter));
        } catch (JsonProcessingException ex) {
            LOGGER.warn("Failed to export store config: {}", ex.getMessage(), ex);
            attributes.put(ATTR_CONTENT, "{}");
        }
    }

    @Override
    public Element getElement() {
        return Element.META;
    }

    @Override
    public Location getLocation() {
        return Location.HEADER;
    }

    @Override
    public Map<String, String> getAttributes() {
        return attributes;
    }
}
