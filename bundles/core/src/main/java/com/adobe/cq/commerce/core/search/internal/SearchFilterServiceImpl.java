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

package com.adobe.cq.commerce.core.search.internal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.search.FilterAttributeMetadata;
import com.adobe.cq.commerce.core.search.SearchFilterService;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.Attribute;
import com.adobe.cq.commerce.magento.graphql.AttributeInput;
import com.adobe.cq.commerce.magento.graphql.CustomAttributeMetadataQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.magento.graphql.introspection.FilterIntrospectionQuery;
import com.adobe.cq.commerce.magento.graphql.introspection.IntrospectionQuery;

@Component(service = SearchFilterService.class)
public class SearchFilterServiceImpl implements SearchFilterService {

    // The "cache" life of the custom attributes for filter queries
    private static final long ATTRIBUTE_CACHE_LIFE_MS = 600000;
    private List<FilterAttributeMetadata> availableFilterMetadata = null;
    private Long lastFetched = null;

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchFilterServiceImpl.class);

    @Override
    public List<FilterAttributeMetadata> retrieveCurrentlyAvailableCommerceFilters(final Resource resource) {

        if (shouldRefreshData()) {
            MagentoGraphqlClient magentoIntrospectionGraphqlClient = MagentoGraphqlClient.create(resource, true);
            Map<String, String> availableFilters = fetchAvailableSearchFilters(magentoIntrospectionGraphqlClient);
            MagentoGraphqlClient magentoGraphqlClient = MagentoGraphqlClient.create(resource, false);
            availableFilterMetadata = fetchAttributeMetadata(magentoGraphqlClient, availableFilters);
        }

        return availableFilterMetadata;
    }

    private List<FilterAttributeMetadata> fetchAttributeMetadata(final MagentoGraphqlClient magentoGraphqlClient,
        final Map<String, String> availableFilters) {

        List<AttributeInput> attributeInputs = availableFilters.entrySet().stream().map(stringStringEntry -> {
            AttributeInput attributeInput = new AttributeInput();
            attributeInput.setAttributeCode(stringStringEntry.getKey());
            attributeInput.setEntityType("4");
            return attributeInput;
        }).collect(Collectors.toList());

        CustomAttributeMetadataQueryDefinition queryArgs = attributeQuery -> attributeQuery
            .items(_queryBuilder -> _queryBuilder
                .attributeCode()
                .attributeType()
                .inputType());
        final QueryQuery attributeQuery = Operations.query(query -> query.customAttributeMetadata(attributeInputs, queryArgs));

        if (magentoGraphqlClient == null) {
            LOGGER.error("MagentoGraphQL client is null, unable to make query to fetch attribute metadata.");
            return new ArrayList<>();
        }

        final GraphqlResponse<Query, Error> response = magentoGraphqlClient.execute(
            attributeQuery.toString());

        final List<FilterAttributeMetadata> attributeMetadataCollection = availableFilters.entrySet().stream().map(stringStringEntry -> {
            final FilterAttributeMetadataImpl filterAttributeMetadata = new FilterAttributeMetadataImpl();

            filterAttributeMetadata.setAttributeCode(stringStringEntry.getKey());
            filterAttributeMetadata.setFilterInputType(stringStringEntry.getValue());

            final Optional<Attribute> attributeData = response.getData().getCustomAttributeMetadata().getItems().stream()
                .filter(item -> item.getAttributeCode().equals(stringStringEntry.getKey()))
                .findFirst();
            if (attributeData.isPresent()) {
                filterAttributeMetadata.setAttributeInputType(attributeData.get().getInputType());
                filterAttributeMetadata.setAttributeType(attributeData.get().getAttributeType());
            }
            return filterAttributeMetadata;
        }).collect(Collectors.toList());

        return attributeMetadataCollection;

    }

    /**
     * Determines whether or not a refresh of data is required.
     * TODO: should be refactored to separate decision making class or subsystem
     *
     * @return true if data should be refreshed.
     */
    private boolean shouldRefreshData() {

        Long now = Instant.now().toEpochMilli();
        if (availableFilterMetadata == null || lastFetched == null || (now - lastFetched) > ATTRIBUTE_CACHE_LIFE_MS) {
            lastFetched = now;
            return true;
        }

        return false;

    }

    /**
     * Fetches a list of available search filters from the commerce backend.
     *
     * @param magentoGraphqlClient client for making Magento GraphQL requests
     * @return key value pair of the attribute code or identifier and filter type for that attribute
     */
    private Map<String, String> fetchAvailableSearchFilters(final MagentoGraphqlClient magentoGraphqlClient) {

        if (magentoGraphqlClient == null) {
            LOGGER.error("MagentoGraphQL client is null, unable to make introspection call to fetch available filter attributes.");
            return new HashMap<>();
        }

        final GraphqlResponse<IntrospectionQuery, Error> response = magentoGraphqlClient.executeIntrospection(
            FilterIntrospectionQuery.QUERY);

        if (!checkAndLogErrors(response.getErrors())) {

            Map<String, String> inputFieldCandidates = new HashMap<>();
            response.getData().getType().getInputFields().stream().forEach(inputField -> {
                inputFieldCandidates.put(inputField.getName(), inputField.getType().getName());
            });
            return inputFieldCandidates;
        }

        return new HashMap<>();
    }

    /**
     * Checks the graphql response for errors and logs out to the error console if any are found
     *
     * @param errors the {@link List <Error>} if any
     * @return {@link true} if any errors were found, {@link false} otherwise
     */
    private boolean checkAndLogErrors(List<Error> errors) {
        if (errors != null && errors.size() > 0) {
            errors.stream()
                .forEach(err -> LOGGER.error("An error has occurred: {} ({})", err.getMessage(), err.getCategory()));
            return true;
        } else {
            return false;
        }
    }

}
