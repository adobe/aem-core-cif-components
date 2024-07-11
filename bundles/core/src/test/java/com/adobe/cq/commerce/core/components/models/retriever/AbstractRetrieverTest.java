/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2024 Adobe
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
package com.adobe.cq.commerce.core.components.models.retriever;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

public class AbstractRetrieverTest {

    private TestRetriever subject;

    @Mock
    private MagentoGraphqlClient client;
    @Mock
    private GraphqlResponse<Query, Error> response;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        subject = new TestRetriever(client);
    }

    @Test
    public void testSetQuery() {
        String testQuery = "test query";
        subject.setQuery(testQuery);
        assertEquals(testQuery, subject.getQuery());
    }

    @Test
    public void testGetErrorsEmpty() {
        when(client.execute(any())).thenReturn(response);
        when(response.getErrors()).thenReturn(null);
        subject.populate();
        assertTrue(subject.getErrors().isEmpty());
        assertFalse(subject.hasErrors());
    }

    @Test
    public void testGetErrors() {
        when(client.execute(any())).thenReturn(response);
        when(response.getErrors()).thenReturn(Collections.singletonList(new Error()));
        subject.populate();
        assertEquals(1, subject.getErrors().size());
        assertTrue(subject.hasErrors());
    }

    @Test(expected = IllegalStateException.class)
    public void testGetErrorsWithoutPopulate() {
        subject.getErrors();
    }

    @Test
    public void testCreateWithNullClient() {
        try {
            new TestRetriever(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("No GraphQL client provided", e.getMessage());
        }
    }

    private static class TestRetriever extends AbstractRetriever {

        public TestRetriever(MagentoGraphqlClient client) {
            super(client);
        }

        @Override
        protected void populate() {
            GraphqlResponse<Query, Error> response = executeQuery();
            errors = response.getErrors();
        }

        @Override
        protected GraphqlResponse<Query, Error> executeQuery() {
            return client.execute(query);
        }

        public String getQuery() {
            return query;
        }

        @Override
        public String generateQuery() {
            return "";
        }
    }
}
