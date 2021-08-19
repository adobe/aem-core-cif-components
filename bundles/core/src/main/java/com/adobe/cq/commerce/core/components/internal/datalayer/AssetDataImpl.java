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
package com.adobe.cq.commerce.core.components.internal.datalayer;

import java.util.Date;
import java.util.Map;

import com.adobe.cq.commerce.core.components.models.product.Asset;
import com.adobe.cq.wcm.core.components.models.datalayer.AssetData;
import com.adobe.cq.wcm.core.components.util.ComponentUtils;

public class AssetDataImpl implements AssetData {
    private final Asset asset;

    public AssetDataImpl(Asset asset) {
        this.asset = asset;
    }

    @Override
    public String getId() {
        return ComponentUtils.generateId(asset.getType(), asset.getPath());
    }

    @Override
    public String getFormat() {
        return asset.getType();
    }

    @Override
    public String getUrl() {
        return asset.getPath();
    }

    @Override
    public String[] getTags() {
        return new String[0];
    }

    @Override
    public Date getLastModifiedDate() {
        return null;
    }

    // Workaround until https://github.com/adobe/aem-core-wcm-components/issues/1415 is fixed
    public Map<String, Object> getSmartTags() {
        return null;
    }
}
