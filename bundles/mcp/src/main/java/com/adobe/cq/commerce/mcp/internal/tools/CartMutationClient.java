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
import org.apache.sling.api.resource.Resource;

import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.commerce.graphql.client.RequestOptions;
import com.adobe.cq.commerce.magento.graphql.CartQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Mutation;
import com.adobe.cq.commerce.magento.graphql.MutationQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.magento.graphql.gson.MutationDeserializer;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.adobe.granite.ui.components.ds.ValueMapResource;

/**
 * Executes Magento GraphQL mutations for cart tools. {@code MagentoGraphqlClient.execute} cannot be used for mutations:
 * its implementation hardcodes response deserialization to {@code Query.class}. This resolves the raw {@link GraphqlClient}
 * instead, using {@link MutationDeserializer#getGson()}. The endpoint's own resource is not directly adaptable to
 * {@link GraphqlClient} (verified live: {@code resource.adaptTo(GraphqlClient.class)} returns {@code null} for the nav-root
 * page resource) &mdash; {@code MagentoGraphqlClientImpl} resolves the CIF context-aware commerce config first and adapts a
 * synthetic resource wrapping it instead, so this reproduces that same resolution using only public API
 * ({@link ComponentsConfiguration}, {@link ValueMapResource}).
 * <p>
 * {@link #resolveGraphqlClient} and {@link #toHeaders} are also reused by {@link GetOrderTool}, which needs the same
 * raw {@link GraphqlClient} access for a query (Magento's {@code guestOrder}) that isn't a cart mutation either.
 */
public class CartMutationClient {

    /**
     * The cart field selection every cart tool's response is mapped from
     * ({@link com.adobe.cq.commerce.mcp.internal.dto.DtoMapper#cart}) &mdash; shared so
     * {@code view_cart}, {@code add_to_cart}, {@code update_cart_item} and {@code clear_cart} request (and can only
     * request) the exact same shape, whether the fields come from a plain {@code Query.cart(...)} or from a mutation's
     * {@code cart(...)} output selection (both take a {@code CartQueryDefinition}).
     */
    public static CartQueryDefinition cartFields() {
        return c -> c
            .id()
            .totalQuantity()
            .items(i -> i
                .uid()
                .quantity()
                .product(p -> p.sku().name())
                .prices(pr -> pr.price(m -> m.value().currency()).rowTotal(m -> m.value().currency())))
            .prices(cp -> cp.grandTotal(m -> m.value().currency()));
    }

    public Mutation execute(StoreContext ctx, MutationQueryDefinition definition) {
        GraphqlClient graphqlClient = resolveGraphqlClient(ctx.getResource());
        if (graphqlClient == null) {
            throw new IllegalStateException("GraphQL client not available for resource " + ctx.getResource().getPath());
        }
        if (ctx.getClient() == null) {
            throw new IllegalStateException("Commerce configuration not available for resource " + ctx.getResource().getPath());
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

    public static GraphqlClient resolveGraphqlClient(Resource resource) {
        GraphqlClient direct = resource.adaptTo(GraphqlClient.class);
        if (direct != null) {
            return direct;
        }
        ComponentsConfiguration configuration = resource.adaptTo(ComponentsConfiguration.class);
        if (configuration == null || configuration.size() == 0) {
            return null;
        }
        Resource configResource = new ValueMapResource(resource.getResourceResolver(), resource.getPath(),
            resource.getResourceType(), configuration.getValueMap());
        return configResource.adaptTo(GraphqlClient.class);
    }

    public static List<Header> toHeaders(Map<String, String[]> headerMap) {
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
