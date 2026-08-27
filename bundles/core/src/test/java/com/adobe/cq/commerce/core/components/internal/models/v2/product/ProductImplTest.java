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
package com.adobe.cq.commerce.core.components.internal.models.v2.product;

import java.io.IOException;
import java.util.List;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.google.common.collect.ImmutableMap;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProductImplTest extends com.adobe.cq.commerce.core.components.internal.models.v1.product.ProductImplTest {

    @Override
    protected void adaptToProduct() {
        // This ensures we re-run all the unit tests with version 2 of ProductImpl
        productModel = context.request().adaptTo(ProductImpl.class);
    }

    @Override
    public void testProduct() {
        testProductImpl(true);
    }

    @Override
    public void testGroupedProduct() throws IOException {
        testGroupedProductImpl(true);
    }

    @Override
    public void testBundleProduct() throws IOException {
        testBundleProductImpl(true);
    }

    /**
     * By default (no {@code enableContentStaging} configuration) the {@code staged} field is added to the product and
     * variant queries so that the author-only "Staged" badge keeps working on Adobe Commerce deployments.
     */
    @Test
    public void testStagedFieldQueriedByDefault() {
        Assert.assertTrue("staged must be queried by default", executedQueriesContainStaged());
    }

    /**
     * When {@code enableContentStaging} is set to {@code false} (e.g. for Magento Open Source backends that do not
     * support the Content Staging {@code staged} field), the {@code staged} field must not be added to any query so
     * that the query stays schema-valid.
     */
    @Test
    public void testStagedFieldOmittedWhenStagingDisabled() {
        ValueMap configMap = new ValueMapDecorator(ImmutableMap.of(
            "cq:graphqlClient", "default",
            "magentoStore", "my-store",
            "enableUIDSupport", "true",
            "enableContentStaging", false));
        ComponentsConfiguration stagingDisabled = new ComponentsConfiguration(configMap);
        when(pageResource.adaptTo(ComponentsConfiguration.class)).thenReturn(stagingDisabled);

        Assert.assertFalse("staged must not be queried when content staging is disabled", executedQueriesContainStaged());
    }

    private boolean executedQueriesContainStaged() {
        adaptToProduct();
        // Trigger the product data fetch which executes the GraphQL query.
        productModel.getName();

        ArgumentCaptor<GraphqlRequest> captor = ArgumentCaptor.forClass(GraphqlRequest.class);
        verify(graphqlClient, atLeastOnce()).execute(captor.capture(), any(), any(), any());

        List<GraphqlRequest> requests = captor.getAllValues();
        return requests.stream().anyMatch(request -> request.getQuery().contains("staged"));
    }
}
