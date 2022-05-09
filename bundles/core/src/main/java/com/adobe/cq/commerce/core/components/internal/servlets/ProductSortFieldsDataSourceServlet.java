/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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
package com.adobe.cq.commerce.core.components.internal.servlets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.magento.graphql.SortField;
import com.adobe.cq.commerce.magento.graphql.SortFields;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.cq.commons.jcr.JcrConstants;

/**
 * A {@link DataSource} implementation for the available product sort fields.
 */
@Component(
    immediate = true,
    service = Servlet.class,
    property = { "sling.servlet.resourceTypes=core/cif/components/commerce/productcollection/sortfields" })
public class ProductSortFieldsDataSourceServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        Resource suffixResource = request.getRequestPathInfo().getSuffixResource();
        if (suffixResource == null) {
            return;
        }

        MagentoGraphqlClient magentoGraphqlClient = suffixResource.adaptTo(MagentoGraphqlClient.class);
        if (magentoGraphqlClient == null) {
            return;
        }

        List<Resource> values = new ArrayList<>();

        String query = "{products(filter:{}) {sort_fields {default options {label value}}}}";
        SortFields sortFields = magentoGraphqlClient.execute(query).getData().getProducts().getSortFields();

        String defaultSortField = sortFields.getDefault();
        ResourceResolver resourceResolver = request.getResourceResolver();

        for (SortField sortField : sortFields.getOptions()) {
            ValueMap vm = new ValueMapDecorator(new HashMap<>());
            vm.put("value", sortField.getValue());
            vm.put("text", sortField.getLabel());
            if (defaultSortField != null && defaultSortField.equals(sortField.getValue())) {
                vm.put("icon", "starStroke");

            }
            values.add(new ValueMapResource(resourceResolver, new ResourceMetadata(), JcrConstants.NT_UNSTRUCTURED, vm));
        }

        if (suffixResource.isResourceType("core/cif/components/commerce/searchresults/v2/searchresults")) {
            if (values.stream().noneMatch(res -> "relevance".equals(res.getValueMap().get("value", String.class)))) {
                ValueMap vm = new ValueMapDecorator(new HashMap<>());
                vm.put("value", "relevance");
                vm.put("text", "Relevance");
                values.add(new ValueMapResource(resourceResolver, new ResourceMetadata(), JcrConstants.NT_UNSTRUCTURED, vm));
            }
        }

        DataSource ds = new SimpleDataSource(values.iterator());
        request.setAttribute(DataSource.class.getName(), ds);
    }
}
