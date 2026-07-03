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

import java.util.Collections;

import org.apache.sling.api.resource.Resource;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.graphql.client.RequestOptions;
import com.adobe.cq.commerce.magento.graphql.Mutation;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.mcp.internal.StoreContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CartMutationClientTest {

    @Test
    public void returnsMutationDataOnSuccess() {
        Mutation mutation = mock(Mutation.class);
        GraphqlResponse<Mutation, Error> response = new GraphqlResponse<>();
        response.setData(mutation);

        Resource resource = mock(Resource.class);
        GraphqlClient graphqlClient = mock(GraphqlClient.class);
        when(resource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);
        when(graphqlClient.<Mutation, Error>execute(any(GraphqlRequest.class), eq(Mutation.class), eq(Error.class),
            any(RequestOptions.class))).thenReturn(response);

        MagentoGraphqlClient magentoClient = mock(MagentoGraphqlClient.class);
        when(magentoClient.getHttpHeaderMap()).thenReturn(Collections.emptyMap());

        StoreContext ctx = mock(StoreContext.class);
        when(ctx.getResource()).thenReturn(resource);
        when(ctx.getClient()).thenReturn(magentoClient);

        CartMutationClient client = new CartMutationClient();
        Mutation result = client.execute(ctx, m -> m.createEmptyCart());

        assertEquals(mutation, result);
    }

    @Test
    public void throwsWithMagentoErrorMessageOnFailure() {
        Error error = new Error();
        error.setMessage("The cart isn't active.");
        GraphqlResponse<Mutation, Error> response = new GraphqlResponse<>();
        response.setErrors(Collections.singletonList(error));

        Resource resource = mock(Resource.class);
        GraphqlClient graphqlClient = mock(GraphqlClient.class);
        when(resource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);
        when(graphqlClient.<Mutation, Error>execute(any(GraphqlRequest.class), eq(Mutation.class), eq(Error.class),
            any(RequestOptions.class))).thenReturn(response);

        MagentoGraphqlClient magentoClient = mock(MagentoGraphqlClient.class);
        when(magentoClient.getHttpHeaderMap()).thenReturn(Collections.emptyMap());

        StoreContext ctx = mock(StoreContext.class);
        when(ctx.getResource()).thenReturn(resource);
        when(ctx.getClient()).thenReturn(magentoClient);

        CartMutationClient client = new CartMutationClient();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> client.execute(ctx, m -> m.createEmptyCart()));
        assertEquals("The cart isn't active.", ex.getMessage());
    }
}
