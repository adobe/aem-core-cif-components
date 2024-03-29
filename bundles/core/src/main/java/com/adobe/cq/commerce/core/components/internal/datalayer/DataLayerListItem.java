/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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
package com.adobe.cq.commerce.core.components.internal.datalayer;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;

import com.adobe.cq.wcm.core.components.util.ComponentUtils;

import static com.adobe.cq.wcm.core.components.util.ComponentUtils.ID_SEPARATOR;

public abstract class DataLayerListItem extends DataLayerComponent {

    public static final String ITEM_ID_PREFIX = "item";

    protected String parentId;

    protected DataLayerListItem(String parentId, Resource resource) {
        this.parentId = parentId;
        this.resource = resource;
    }

    protected String getIdentifier() {
        return resource.getPath();
    }

    @Override
    protected String generateId() {
        String prefix = StringUtils.join(parentId, ID_SEPARATOR, ITEM_ID_PREFIX);
        return ComponentUtils.generateId(prefix, getIdentifier());
    }
}
