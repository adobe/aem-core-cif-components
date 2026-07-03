/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
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
package com.adobe.cq.commerce.mcp.internal.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.commerce.graphql.client.RequestOptions;
import com.adobe.cq.commerce.magento.graphql.Mutation;
import com.adobe.cq.commerce.magento.graphql.MutationQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.magento.graphql.gson.MutationDeserializer;
import com.adobe.cq.commerce.mcp.internal.StoreContext;

/**
 * Executes Magento GraphQL mutations for cart tools. {@code MagentoGraphqlClient.execute} cannot be used for mutations:
 * its implementation hardcodes response deserialization to {@code Query.class}. This adapts the endpoint's own
 * resource to the raw {@link GraphqlClient} instead, using {@link MutationDeserializer#getGson()}.
 */
public class CartMutationClient {

    public Mutation execute(StoreContext ctx, MutationQueryDefinition definition) {
        GraphqlClient graphqlClient = ctx.getResource().adaptTo(GraphqlClient.class);
        if (graphqlClient == null) {
            throw new IllegalStateException("GraphQL client not available for resource " + ctx.getResource().getPath());
        }

        String mutation = Operations.mutation(definition).toString();
        RequestOptions options = new RequestOptions()
            .withGson(MutationDeserializer.getGson())
            .withHeaders(toHeaders(ctx.getClient().getHttpHeaderMap()))
            .withHttpMethod(HttpMethod.POST);

        GraphqlResponse<Mutation, Error> response = graphqlClient.execute(new GraphqlRequest(mutation), Mutation.class, Error.class,
            options);

        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            StringBuilder message = new StringBuilder();
            for (Error error : response.getErrors()) {
                if (message.length() > 0) {
                    message.append("; ");
                }
                message.append(error.getMessage());
            }
            throw new IllegalStateException(message.toString());
        }

        return response.getData();
    }

    private List<Header> toHeaders(Map<String, String[]> headerMap) {
        List<Header> headers = new ArrayList<>();
        if (headerMap != null) {
            for (Map.Entry<String, String[]> entry : headerMap.entrySet()) {
                for (String value : entry.getValue()) {
                    headers.add(new BasicHeader(entry.getKey(), value));
                }
            }
        }
        return headers.isEmpty() ? null : headers;
    }
}
