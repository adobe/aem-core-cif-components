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
package com.adobe.cq.commerce.core.search.internal.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.search.internal.converters.FilterAttributeMetadataConverter;
import com.adobe.cq.commerce.core.search.models.FilterAttributeMetadata;
import com.adobe.cq.commerce.core.search.services.SearchFilterService;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.commerce.magento.graphql.Attribute;
import com.adobe.cq.commerce.magento.graphql.AttributeInput;
import com.adobe.cq.commerce.magento.graphql.CustomAttributeMetadata;
import com.adobe.cq.commerce.magento.graphql.CustomAttributeMetadataQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;
import com.adobe.cq.commerce.magento.graphql.__InputValue;
import com.adobe.cq.commerce.magento.graphql.__Type;
import com.adobe.cq.commerce.magento.graphql.__TypeQuery;
import com.adobe.cq.commerce.magento.graphql.__TypeQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.day.cq.wcm.api.Page;

@Component(service = { SearchFilterServiceImpl.class, SearchFilterService.class })
@Designate(ocd = SearchFilterServiceImpl.Configuration.class)
public class SearchFilterServiceImpl implements SearchFilterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchFilterServiceImpl.class);

    @ObjectClassDefinition(name = "CIF Search Filter Service")
    @interface Configuration {

        @AttributeDefinition(
            name = "Enforce POST",
            description = "If enabled, the service uses POST requests to execute the custom "
                + "attribute metadata query. This may be helpful for catalogs with many custom attributes, where the query string exceeds "
                + "the size limits of GET requests. Defaults to false")
        boolean enforcePost() default false;

    }

    private boolean enforcePost;

    @Activate
    protected void activate(Configuration configuration) {
        enforcePost = configuration.enforcePost();
    }

    @Override
    public List<FilterAttributeMetadata> retrieveCurrentlyAvailableCommerceFilters(final Page page) {
        // This is used to configure the cache in the GraphqlClient with a cache name of
        // --> com.adobe.cq.commerce.core.search.services.SearchFilterService
        return Optional.ofNullable(page.adaptTo(Resource.class))
            .map(r -> new SyntheticResource(r.getResourceResolver(), r.getPath(), SearchFilterService.class.getName()))
            .map(r -> r.adaptTo(MagentoGraphqlClient.class))
            .map(magentoGraphqlClient -> retrieveCurrentlyAvailableCommerceFiltersInfo(magentoGraphqlClient).getLeft())
            .orElseGet(Collections::emptyList);
    }

    public List<FilterAttributeMetadata> retrieveCurrentlyAvailableCommerceFilters(final SlingHttpServletRequest request, Page page) {
        return retrieveCurrentlyAvailableCommerceFiltersInfo(request, page).getLeft();
    }

    public Pair<List<FilterAttributeMetadata>, List<Error>> retrieveCurrentlyAvailableCommerceFiltersInfo(
        final SlingHttpServletRequest request, Page page) {
        // This is used to configure the cache in the GraphqlClient with a cache name of
        // --> com.adobe.cq.commerce.core.search.services.SearchFilterService
        Resource r = page.adaptTo(Resource.class);
        if (r != null) {
            SyntheticResource syntheticResource = new SyntheticResource(r.getResourceResolver(), r.getPath(), SearchFilterService.class
                .getName());
            SlingHttpServletRequestWrapper slingHttpServletRequestWrapper = new SlingHttpServletRequestWrapper(request) {
                @Override
                public Resource getResource() {
                    return syntheticResource;
                }
            };
            MagentoGraphqlClient magentoGraphqlClient = slingHttpServletRequestWrapper.adaptTo(MagentoGraphqlClient.class);
            if (magentoGraphqlClient != null) {
                Pair<List<FilterAttributeMetadata>, List<Error>> filterAttributeMetadataInfo = retrieveCurrentlyAvailableCommerceFiltersInfo(
                    magentoGraphqlClient);
                return filterAttributeMetadataInfo;
            }
        }
        return Pair.of(Collections.emptyList(), Collections.emptyList());
    }

    private Pair<List<FilterAttributeMetadata>, List<Error>> retrieveCurrentlyAvailableCommerceFiltersInfo(
        MagentoGraphqlClient magentoGraphqlClient) {
        // First we query Magento for the required attribute and filter information
        final Pair<List<__InputValue>, List<Error>> availableFiltersInfo = fetchAvailableSearchFilters(magentoGraphqlClient);
        final Pair<List<Attribute>, List<Error>> attributesInfo = fetchAttributeMetadata(magentoGraphqlClient, availableFiltersInfo
            .getLeft());
        final List<Error> errors = new ArrayList<>();
        if (availableFiltersInfo.getRight() != null) {
            errors.addAll(availableFiltersInfo.getRight());
        }
        if (attributesInfo.getRight() != null) {
            errors.addAll(attributesInfo.getRight());
        }
        // Then we combine this data into a useful set of data usable by other systems
        FilterAttributeMetadataConverter converter = new FilterAttributeMetadataConverter(attributesInfo.getLeft());
        return Pair.of(availableFiltersInfo.getLeft().stream().map(converter).collect(Collectors.toList()), errors);
    }

    private Pair<List<Attribute>, List<Error>> fetchAttributeMetadata(final MagentoGraphqlClient magentoGraphqlClient,
        final List<__InputValue> availableFilters) {

        if (magentoGraphqlClient == null) {
            LOGGER.error("MagentoGraphQL client is null, unable to make query to fetch attribute metadata.");
            return Pair.of(Collections.emptyList(), Collections.emptyList());
        }

        List<AttributeInput> attributeInputs = availableFilters.stream().map(inputField -> {
            AttributeInput attributeInput = new AttributeInput();
            attributeInput.setAttributeCode(inputField.getName());
            attributeInput.setEntityType("4");
            return attributeInput;
        }).collect(Collectors.toList());

        CustomAttributeMetadataQueryDefinition queryArgs = attributeQuery -> attributeQuery
            .items(_queryBuilder -> _queryBuilder
                .attributeCode()
                .attributeType()
                .inputType());
        final QueryQuery attributeQuery = Operations.query(query -> query.customAttributeMetadata(attributeInputs, queryArgs));

        final GraphqlResponse<Query, Error> response = enforcePost
            ? magentoGraphqlClient.execute(attributeQuery.toString(), HttpMethod.POST)
            : magentoGraphqlClient.execute(attributeQuery.toString());

        // If there are errors we'll log them and return a safe but empty list
        if (CollectionUtils.isNotEmpty(response.getErrors())) {
            response.getErrors()
                .forEach(err -> LOGGER.error("An error has occurred: {} ({})", err.getMessage(), err.getCategory()));
            return Pair.of(Collections.emptyList(), response.getErrors());
        }

        CustomAttributeMetadata cam = response.getData().getCustomAttributeMetadata();
        return Pair.of(cam != null ? response.getData().getCustomAttributeMetadata().getItems() : Collections.emptyList(),
            Collections.emptyList());
    }

    /**
     * Fetches a list of available search filters from the commerce backend.
     *
     * @param magentoGraphqlClient client for making Magento GraphQL requests
     * @return key value pair of the attribute code or identifier and filter type for that attribute
     */
    private Pair<List<__InputValue>, List<Error>> fetchAvailableSearchFilters(final MagentoGraphqlClient magentoGraphqlClient) {

        if (magentoGraphqlClient == null) {
            LOGGER.error("MagentoGraphQL client is null, unable to make introspection call to fetch available filter attributes.");
            return Pair.of(Collections.emptyList(), Collections.emptyList());
        }

        __TypeQueryDefinition typeQuery = q -> q
            .name()
            .description()
            .inputFields(i -> i
                .name()
                .type(__TypeQuery::name));

        String query = Operations.query(q -> q.__type("ProductAttributeFilterInput", typeQuery)).toString();

        final GraphqlResponse<Query, Error> response = magentoGraphqlClient.execute(query);

        // If there are errors in the response we'll log them out and return a safe but empty value
        if (CollectionUtils.isNotEmpty(response.getErrors())) {
            response.getErrors()
                .forEach(err -> LOGGER.error("An error has occurred: {} ({})", err.getMessage(), err.getCategory()));
            return Pair.of(Collections.emptyList(), response.getErrors());
        }

        __Type type = response.getData().__getType();
        return Pair.of(type != null ? type.getInputFields() : Collections.emptyList(), Collections.emptyList());
    }
}
