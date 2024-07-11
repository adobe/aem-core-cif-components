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
package com.adobe.cq.commerce.core.components.client;

import java.util.List;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.osgi.annotation.versioning.ProviderType;

import com.adobe.cq.commerce.core.components.internal.client.MagentoGraphqlClientImpl;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlClientConfiguration;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;

/**
 * This interface gives access to a {@link GraphqlClient} configured for a given context.
 * <p>
 * It is implemented as adapter for {@link SlingHttpServletRequest} and {@link Resource} where the full feature set can only be used with
 * a {@link SlingHttpServletRequest} as an adaptable.
 *
 * @see MagentoGraphqlClientImpl for what feature are supported for any of the adaptables
 */
@ProviderType
public interface MagentoGraphqlClient {

    String STORE_CODE_PROPERTY = "magentoStore";

    String CONFIGURATION_NAME = "cloudconfigs/commerce";

    /**
     * A category string used for {@link Error} instances returned by the {@link MagentoGraphqlClient} implementation in case of a
     * {@link RuntimeException} being caught.
     */
    String RUNTIME_ERROR_CATEGORY = RuntimeException.class.getName();

    /**
     * Executes the given Magento query and returns the response. This method will use
     * the default HTTP method defined in the OSGi configuration of the underlying {@link GraphqlClient}.
     * Use {@link #execute(String, HttpMethod)} if you want to specify the HTTP method yourself.
     *
     * @param query The GraphQL query.
     * @return The GraphQL response.
     */
    GraphqlResponse<Query, Error> execute(String query);

    List<GraphqlResponse<Query, Error>> executeAll(List<String> queries);

    List<GraphqlResponse<Query, Error>> executeAllAsync(List<String> queries);

    /**
     * Executes the given Magento query and returns the response. This method
     * uses the given <code>httpMethod</code> to fetch the data.
     *
     * @param query The GraphQL query.
     * @param httpMethod The HTTP method that will be used to fetch the data.
     * @return The GraphQL response.
     */
    GraphqlResponse<Query, Error> execute(String query, HttpMethod httpMethod);

    /**
     * Returns the complete configuration of the GraphQL client.
     *
     * @return GraphQL client configuration.
     */
    GraphqlClientConfiguration getConfiguration();

    /**
     * Returns the list of custom HTTP headers used by the GraphQL client.
     *
     * @return a {@link Map} with header names as keys and header values as values
     * @deprecated this method will be replaced by {@link MagentoGraphqlClient#getHttpHeaderMap()} in the next major release‡
     */
    @Deprecated
    Map<String, String> getHttpHeaders();

    /**
     * Returns the Map of custom HTTP headers used by the GraphQL client.
     * 
     * @return a {@link Map} with header names as keys and header values as values
     */
    Map<String, String[]> getHttpHeaderMap();
}
