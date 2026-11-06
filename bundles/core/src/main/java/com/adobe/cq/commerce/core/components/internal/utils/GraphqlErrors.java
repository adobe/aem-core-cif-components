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

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractRetriever;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.magento.graphql.gson.Error;

public final class GraphqlErrors {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphqlErrors.class);

    private GraphqlErrors() {}

    /**
     * Read errors only after fetching the retriever's data.
     */
    public static List<Error> getErrors(AbstractRetriever retriever) {
        return retriever == null ? Collections.emptyList() : getErrors(retriever::getErrors);
    }

    public static List<Error> getErrors(SearchResultsSet results) {
        return results == null ? Collections.emptyList() : getErrors(results::getErrors);
    }

    private static List<Error> getErrors(Supplier<List<Error>> supplier) {
        try {
            List<Error> errors = supplier.get();
            return errors == null ? Collections.emptyList() : errors;
        } catch (IllegalStateException ex) {
            // Custom retrievers may fetch data without initializing AbstractRetriever.errors.
            LOGGER.debug("GraphQL errors are unavailable after fetching data: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    public static Error getTransportError(AbstractRetriever retriever) {
        return getTransportError(getErrors(retriever));
    }

    public static Error getTransportError(SearchResultsSet results) {
        return getTransportError(getErrors(results));
    }

    private static Error getTransportError(List<Error> errors) {
        for (Error error : errors) {
            if (error != null && MagentoGraphqlClient.RUNTIME_ERROR_CATEGORY.equals(error.getCategory())) {
                return error;
            }
        }
        return null;
    }
}
