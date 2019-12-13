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

package com.adobe.cq.commerce.core.components.models;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.graphql.GraphqlQueryFinder;
import com.adobe.cq.commerce.core.components.models.graphql.GraphqlRecordFactory;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.day.cq.wcm.api.Page;

public class GraphqlModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphqlModel.class);
    protected final GraphqlRecordFactory graphqlRecordFactory = new GraphqlRecordFactory(new GraphqlRecordFactory.Context() {
        @Override
        public Page getCurrentPage() {
            return currentPage;
        }

        @Override
        public String getMediaBaseUrl() {
            return GraphqlModel.this.getMediaBaseUrl();
        }

        @Override
        public String getVariantSku() {
            return GraphqlModel.this.getVariantSku();
        }
    });

    @Inject
    protected Resource resource;
    @ScriptVariable
    protected ValueMap properties;
    @Inject
    protected Page currentPage;
    protected MagentoGraphqlClient magentoGraphqlClient;
    protected Object data;
    private GraphqlQueryFinder queryFinder;

    @PostConstruct
    private void initModel0() {
        magentoGraphqlClient = MagentoGraphqlClient.create(resource);
        queryFinder = new GraphqlQueryFinder(resource.getResourceResolver());
    }

    protected Query executeQuery() {
        return executeQuery(null);
    }

    protected Query executeQuery(Map<String, Object> values) {
        if (magentoGraphqlClient == null) {
            return null;
        }

        String queryString = queryFinder.fromResourceType(this.resource.getResourceType());
        if (StringUtils.isBlank(queryString)) {
            return null;
        }

        Map<String, Object> vars = new HashMap<>();
        if (values != null) {
            vars.putAll(values);
        }
        GraphqlResponse<Query, Error> response = magentoGraphqlClient.execute(queryString, vars);
        Query query = response.getData();
        data = graphqlRecordFactory.recordFrom(query);
        return query;
    }

    public Object getData() {
        return data;
    }

    protected void setFormatter(GraphqlRecordFactory.Formatter formatter) {
        graphqlRecordFactory.setFormatter(formatter);
    }

    public String getMediaBaseUrl() {
        return null;
    }

    public String getVariantSku() {
        return null;
    }

}
