/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.components.models.categorylist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.scripting.sightly.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.models.GraphqlModel;
import com.adobe.cq.commerce.magento.graphql.Query;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = FeaturedCategoryListModel.class)
public class FeaturedCategoryListModel extends GraphqlModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturedCategoryListModel.class);
    private static final String CATEGORY_ID_PROP = "categoryIds";

    @PostConstruct
    private void initModel() {
        String[] categoryIdArray = properties.get(CATEGORY_ID_PROP, String[].class);
        if (categoryIdArray != null && categoryIdArray.length > 0) {
            List<Object> categories = new ArrayList<>();
            Map<String, Object> values = new HashMap<>();
            for (String id : categoryIdArray) {
                values.put("id", id);
                Query result = executeQuery(values);
                categories.add(graphqlRecordFactory.recordFrom(result));
            }

            data = categories;
        } else {
            LOGGER.debug("There are no categories configured for CategoryList Component.");
        }
    }

    @Override
    public String getMediaBaseUrl() {
        List list = (List) data;
        if (list.size() > 0) {
            return (String) ((Record) ((Record) list.get(0)).getProperty("storeConfig")).getProperty("secure_base_media_url");
        }
        return null;
    }
}
