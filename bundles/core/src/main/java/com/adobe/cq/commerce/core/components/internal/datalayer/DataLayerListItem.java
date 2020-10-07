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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;

public abstract class DataLayerListItem extends DataLayerComponent {

    public static final String ITEM_ID_PREFIX = "item";

    protected String parentId;

    protected DataLayerListItem(String parentId, Resource resource) {
        this.parentId = parentId;
        this.resource = resource;
    }

    @Override
    protected String generateId() {
        String prefix = StringUtils.join(parentId, ID_SEPARATOR, ITEM_ID_PREFIX);
        return StringUtils.join(prefix, ID_SEPARATOR, StringUtils.substring(DigestUtils.sha256Hex(resource.getPath()), 0, 10));
    }
}
