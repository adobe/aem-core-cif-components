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

package com.adobe.cq.commerce.core.components.internal.models.v1;

import java.lang.reflect.Type;
import java.util.Collections;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.graphql.client.RequestOptions;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.day.cq.commons.inherit.ComponentInheritanceValueMap;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

/**
 * This is a wrapper class for {@link GraphqlClient}. The constructor adapts a {@link Resource} to
 * the GraphqlClient class and also looks for the <code>cq:magentoStore</code> property on the resource
 * path in order to set the Magento <code>Store</code> HTTP header. This wrapper also sets the custom
 * Magento Gson deserializer from {@link QueryDeserializer}.
 */
public class GraphqlClientWrapper implements GraphqlClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphqlClientWrapper.class);

    public static final String STORE_CODE_PROPERTY = "cq:magentoStore";

    private GraphqlClient graphqlClient;
    private RequestOptions requestOptions;

    public GraphqlClientWrapper(Resource resource) {
        graphqlClient = resource.adaptTo(GraphqlClient.class);
        if (graphqlClient == null) {
            LOGGER.warn("GraphQL client not available for resource " + resource.getPath());
        }

        requestOptions = new RequestOptions().withGson(QueryDeserializer.getGson());

        InheritanceValueMap properties;
        Page page = resource.getResourceResolver().adaptTo(PageManager.class).getContainingPage(resource);
        if (page != null) {
            properties = new HierarchyNodeInheritanceValueMap(page.getContentResource());
        } else {
            properties = new ComponentInheritanceValueMap(resource);
        }
        String storeCode = properties.getInherited(STORE_CODE_PROPERTY, String.class);
        if (storeCode != null) {
            Header storeHeader = new BasicHeader("Store", storeCode);
            requestOptions.withHeaders(Collections.singletonList(storeHeader));
        }
    }

    @Override
    public String getIdentifier() {
        return graphqlClient.getIdentifier();
    }

    /**
     * <b>This method uses the {@link RequestOptions} set by the GraphqlClientWrapper class.</b>
     * <br/>
     * <br/>
     * {@inheritDoc}
     */
    @Override
    public <T, U> GraphqlResponse<T, U> execute(GraphqlRequest request, Type typeOfT, Type typeOfU) {
        return graphqlClient.execute(request, typeOfT, typeOfU, requestOptions);
    }

    /**
     * <b>Only use this method if you do NOT want to use the {@link RequestOptions} set by the GraphqlClientWrapper class.</b>
     * <br/>
     * <br/>
     * {@inheritDoc}
     */
    @Override
    public <T, U> GraphqlResponse<T, U> execute(GraphqlRequest request, Type typeOfT, Type typeOfU, RequestOptions options) {
        return graphqlClient.execute(request, typeOfT, typeOfU, options);
    }

}
