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

import org.apache.sling.api.resource.Resource;

import com.adobe.cq.commerce.core.components.datalayer.CategoryData;
import com.adobe.cq.commerce.core.components.datalayer.CategoryListData;

public class CategoryListDataImpl extends ComponentDataImpl implements CategoryListData {

    public CategoryListDataImpl(DataLayerComponent component, Resource resource) {
        super(component, resource);
    }

    static public class CategoryDataImpl implements CategoryData {
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
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getImage() {
            return image;
        }
    }

    @Override
    public CategoryData[] getCategories() {
        return component.getDataLayerCategories();
    }
}
