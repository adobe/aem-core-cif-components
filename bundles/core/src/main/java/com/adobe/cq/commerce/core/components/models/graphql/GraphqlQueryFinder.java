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

package com.adobe.cq.commerce.core.components.models.graphql;

import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public class GraphqlQueryFinder {
    private final GraphqlQueryBuilder queryBuilder = new GraphqlQueryBuilder();
    private final ResourceResolver resolver;

    public GraphqlQueryFinder(ResourceResolver resolver) {
        this.resolver = resolver;
    }

    public String fromResourceType(String resourceType) {
        String query = findQuery(resourceType, this::getContentBasedQuery);

        if (query != null)
            return query;

        query = findQuery(resourceType, this::getPlainQuery);

        return query;
    }

    String findQuery(String resourceType, Function<Resource, String> getQuery) {
        if (StringUtils.isBlank(resourceType)) {
            return null;
        }

        Resource component = resolver.getResource(resourceType);
        if (component == null) {
            return null;
        }

        String query = getQuery.apply(component);

        if (query != null) {
            return query;
        }

        query = findQuery(component.getResourceSuperType(), getQuery);

        return query;
    }

    String getPlainQuery(Resource component) {
        Resource queryResource = component.getChild("query.graphql/jcr:content");
        return queryResource != null ? queryResource.getValueMap().get("jcr:data", String.class) : null;
    }

    String getContentBasedQuery(Resource component) {
        Resource queryResource = component.getChild("graphql");
        if (queryResource != null) {
            queryResource = queryResource.getResourceResolver().getResource("/mnt/override/" + queryResource.getPath());
            return queryBuilder.fromResource(queryResource);
        } else {
            return null;
        }
    }
}