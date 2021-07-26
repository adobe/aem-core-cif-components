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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import com.adobe.cq.commerce.core.components.datalayer.CategoryData;
import com.adobe.cq.commerce.core.components.internal.models.v1.product.AssetImpl;
import com.adobe.cq.wcm.core.components.models.datalayer.AssetData;

public class CategoryDataImpl implements CategoryData {
    private String id;
    private String name;
    private String image;

    public CategoryDataImpl(String id, String name, String image) {
        this.id = id;
        this.name = name;
        this.image = image;
    }

    @Override
    public String getId() {
        return StringUtils.join("category", DataLayerComponent.ID_SEPARATOR, StringUtils.substring(DigestUtils.sha256Hex(id), 0, 10));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public AssetData getImage() {
        if (image != null) {
            AssetImpl asset = new AssetImpl();
            asset.setPath(image);
            asset.setType("image");
            return new AssetDataImpl(asset);
        }

        return null;
    }
}