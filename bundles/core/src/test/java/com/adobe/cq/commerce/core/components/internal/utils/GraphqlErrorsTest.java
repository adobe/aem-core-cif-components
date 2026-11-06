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
package com.adobe.cq.commerce.core.components.internal.utils;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.services.CategoryUrlParameterRetriever;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractRetriever;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.magento.graphql.gson.Error;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GraphqlErrorsTest {

    @Test
    public void testNullSources() {
        assertTrue(GraphqlErrors.getErrors((AbstractRetriever) null).isEmpty());
        assertTrue(GraphqlErrors.getErrors((SearchResultsSet) null).isEmpty());
        assertNull(GraphqlErrors.getTransportError((AbstractRetriever) null));
        assertNull(GraphqlErrors.getTransportError((SearchResultsSet) null));
    }

    @Test
    public void testUninitializedAndNullRetrieverErrors() {
        AbstractRetriever retriever = new CategoryUrlParameterRetriever(mock(MagentoGraphqlClient.class));
        assertNull(GraphqlErrors.getTransportError(retriever));
        Whitebox.setInternalState(retriever, "errors", null);
        assertNull(GraphqlErrors.getTransportError(retriever));
    }

    @Test
    public void testNullAndUninitializedSearchErrors() {
        SearchResultsSet results = mock(SearchResultsSet.class);
        when(results.getErrors()).thenReturn(null);
        assertNull(GraphqlErrors.getTransportError(results));
        when(results.getErrors()).thenThrow(new IllegalStateException("not initialized"));
        assertNull(GraphqlErrors.getTransportError(results));
    }

    @Test
    public void testOnlyExactRuntimeCategoryIsTransportFailure() {
        Error transportError = new Error();
        transportError.setCategory(MagentoGraphqlClient.RUNTIME_ERROR_CATEGORY);
        Error graphqlError = new Error();
        graphqlError.setCategory("internal");
        Error unclassifiedError = new Error();
        SearchResultsSet results = mock(SearchResultsSet.class);
        when(results.getErrors()).thenReturn(Arrays.asList(null, graphqlError, unclassifiedError, transportError));
        assertSame(transportError, GraphqlErrors.getTransportError(results));
        when(results.getErrors()).thenReturn(Arrays.asList(null, graphqlError, unclassifiedError));
        assertNull(GraphqlErrors.getTransportError(results));
        when(results.getErrors()).thenReturn(Collections.emptyList());
        assertNull(GraphqlErrors.getTransportError(results));
    }

    @Test
    public void testRetrieverTransportError() {
        Error error = new Error();
        error.setCategory(MagentoGraphqlClient.RUNTIME_ERROR_CATEGORY);
        AbstractRetriever retriever = new CategoryUrlParameterRetriever(mock(MagentoGraphqlClient.class));
        Whitebox.setInternalState(retriever, "errors", Collections.singletonList(error));
        assertSame(error, GraphqlErrors.getTransportError(retriever));
    }
}
