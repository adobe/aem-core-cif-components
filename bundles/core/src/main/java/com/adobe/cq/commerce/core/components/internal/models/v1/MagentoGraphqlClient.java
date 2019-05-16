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
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
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
public class MagentoGraphqlClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MagentoGraphqlClient.class);

    public static final String STORE_CODE_PROPERTY = "cq:magentoStore";

    private GraphqlClient graphqlClient;
    private RequestOptions requestOptions;

    /**
     * Instantiates and returns a new MagentoGraphqlClient.
     * This method returns <code>null</code> if the client cannot be instantiated.
     * 
     * @param resource The JCR resource to use to adapt to the lower-level {@link GraphqlClient}.
     * @return A new MagentoGraphqlClient instance.
     */
    public static MagentoGraphqlClient create(Resource resource) {
        try {
            return new MagentoGraphqlClient(resource);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return null;
        }
    }

    private MagentoGraphqlClient(Resource resource) {
        graphqlClient = resource.adaptTo(GraphqlClient.class);
        if (graphqlClient == null) {
            throw new RuntimeException("GraphQL client not available for resource " + resource.getPath());
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

    /**
     * Executes the given Magento request and returns the response.
     * 
     * @param query The GraphQL query.
     * @return The GraphQL response.
     */
    public GraphqlResponse<Query, Error> execute(String query) {
        return graphqlClient.execute(new GraphqlRequest(query), Query.class, Error.class, requestOptions);
    }

}
