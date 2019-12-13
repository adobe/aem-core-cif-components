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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.script.Bindings;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.scripting.sightly.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.graphql.GraphqlQueryFinder;
import com.adobe.cq.commerce.core.components.models.graphql.GraphqlRecordFactory;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.sightly.WCMUsePojo;
import com.day.cq.wcm.api.Page;

public class GraphqlUsePojo extends WCMUsePojo {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphqlUsePojo.class);
    protected final GraphqlRecordFactory graphqlRecordFactory = new GraphqlRecordFactory(new GraphqlRecordFactory.Context() {
        @Override
        public Page getCurrentPage() {
            return GraphqlUsePojo.this.getCurrentPage();
        }

        @Override
        public String getMediaBaseUrl() {
            return GraphqlUsePojo.this.getMediaBaseUrl();
        }

        @Override
        public String getVariantSku() {
            return GraphqlUsePojo.this.getVariantSku();
        }
    });

    private GraphqlQueryFinder queryFinder;
    protected MagentoGraphqlClient magentoGraphqlClient;
    protected Record data;
    private Bindings bindings0;

    @Override
    public void activate() throws Exception {
        magentoGraphqlClient = MagentoGraphqlClient.create(getResource());
        graphqlRecordFactory.setFormatter(graphqlRecordFactory.new PriceFormatter());
        queryFinder = new GraphqlQueryFinder(getResourceResolver());
        Field bindingsField = WCMUsePojo.class.getDeclaredField("bindings");
        bindingsField.setAccessible(true);
        bindings0 = (Bindings) bindingsField.get(this);

        Object[] args = get("variables", Object[].class);
        if (args != null) {
            executeQuery();
        }
    }

    protected Query executeQuery() {
        return executeQuery(null);
    }

    protected Query executeQuery(Map<String, Object> values) {
        if (magentoGraphqlClient == null) {
            return null;
        }

        String queryString = queryFinder.fromResourceType(this.getResource().getResourceType());
        if (StringUtils.isBlank(queryString)) {
            return null;
        }

        Map<String, Object> vars = new HashMap<>();
        if (values != null) {
            vars.putAll(values);
        } else {
            Object[] args = get("variables", Object[].class);
            if (args != null) {
                for (Object arg : args) {
                    final String key = String.valueOf(arg);
                    vars.put(key, getProperties().get(key));
                }
            }
        }
        GraphqlResponse<Query, Error> response = magentoGraphqlClient.execute(queryString, vars);
        Query query = response.getData();
        data = graphqlRecordFactory.recordFrom(query);
        return query;
    }

    public Record getData() {
        return data;
    }

    protected void setFormatter(GraphqlRecordFactory.Formatter formatter) {
        graphqlRecordFactory.setFormatter(formatter);
    }

    protected String getMediaBaseUrl() {
        if (data == null)
            return null;

        Record storeConfig = (Record) data.getProperty("storeConfig");
        if (storeConfig == null)
            return null;

        return (String) storeConfig.getProperty("secure_base_media_url");
    }

    protected String getVariantSku() {
        return null;
    }
}
