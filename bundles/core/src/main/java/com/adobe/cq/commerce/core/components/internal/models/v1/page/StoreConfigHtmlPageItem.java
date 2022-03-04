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

import com.adobe.cq.commerce.core.components.models.storeconfigexporter.StoreConfigExporter;
import com.adobe.cq.wcm.core.components.models.HtmlPageItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

class StoreConfigHtmlPageItem implements HtmlPageItem {

    private static final String NAME = "store-config";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_CONTENT = "content";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        // we moved from a Map<String,String> to Map<String,String[]> to support multiple http headers with the same name
        // however doing that without unwrapping single element arrays would be a breaking change to any frontend consumers
        .enable(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED);
    private static final Logger LOGGER = LoggerFactory.getLogger(StoreConfigHtmlPageItem.class);

    private final Map<String, Object> attributes = new HashMap<>();

    StoreConfigHtmlPageItem(StoreConfigExporter storeConfigExporter) {
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
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
