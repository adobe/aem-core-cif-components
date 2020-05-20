/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.examples.servlets;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.Products;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shopify.graphql.support.AbstractResponse;
import graphql.ExecutionInput.Builder;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.TypeResolutionEnvironment;
import graphql.introspection.IntrospectionResultToSchema;
import graphql.language.Document;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import graphql.schema.idl.FieldWiringEnvironment;
import graphql.schema.idl.InterfaceWiringEnvironment;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.SchemaPrinter;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.WiringFactory;

@Component(
    service = { Servlet.class },
    property = {
        "sling.servlet.paths=/apps/cif-components-examples/graphql",
        "sling.servlet.methods=" + HttpConstants.METHOD_GET,
        "sling.servlet.methods=" + HttpConstants.METHOD_POST
    })
public class GraphqlServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphqlServlet.class);

    private static final String QUERY_PARAMETER = "query";
    private static final String VARIABLES_PARAMETER = "variables";
    private static final String OPERATION_NAME_PARAMETER = "operationName";

    private static final String PRODUCTS_FILTER_ARG = "filter";
    private static final String PRODUCTS_SEARCH_ARG = "search";
    private static final String CATEGORY_ID_ARG = "id";
    private static final String SKU_IN_REGEX = "\\{sku=\\{in=\\[.+\\]\\}\\}";
    private static final String SKU_EQ_REGEX = "\\{sku=\\{eq=.+\\}\\}";
    private static final String CATEGORY_ID_REGEX = "\\{category_id=\\{eq=.+\\}\\}";

    private Gson gson;
    private GraphQL graphQL;
    private Map<String, GraphqlResponse<Query, Error>> graphqlResponsesCache = new HashMap<>();

    @Override
    public void init() throws ServletException {
        gson = new Gson();
        TypeDefinitionRegistry typeRegistry;
        RuntimeWiring wiring;
        try {
            typeRegistry = buildTypeDefinitionRegistry();
            wiring = buildRuntimeWiring();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize GraphQL schema", e);
        }
        GraphQLSchema graphQLSchema = new SchemaGenerator().makeExecutableSchema(typeRegistry, wiring);
        graphQL = GraphQL.newGraphQL(graphQLSchema).build();
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        String query = request.getParameter(QUERY_PARAMETER);
        String operationName = request.getParameter(OPERATION_NAME_PARAMETER);
        String variables = request.getParameter(VARIABLES_PARAMETER);

        Map<String, Object> vars = null;
        if (StringUtils.isNotBlank(variables)) {
            Type type = TypeToken.getParameterized(Map.class, String.class, Object.class).getType();
            vars = gson.fromJson(variables, type);
        }

        ExecutionResult executionResult = execute(query, operationName, vars);
        writeResponse(executionResult, response);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        String body = IOUtils.toString(request.getReader());
        Type type = TypeToken.getParameterized(GraphqlRequest.class, Query.class, Error.class).getType();
        GraphqlRequest graphqlRequest = gson.fromJson(body, type);

        Map<String, Object> vars = null;
        if (graphqlRequest.getVariables() != null) {
            vars = (Map<String, Object>) graphqlRequest.getVariables();
        }

        ExecutionResult executionResult = execute(graphqlRequest.getQuery(), graphqlRequest.getOperationName(), vars);
        writeResponse(executionResult, response);
    }

    /**
     * Executes the given GraphQL <code>query</code> with the optional <code>operationName</code> and <code>variables</code> parameters.
     * 
     * @param query The GraphQL query.
     * @param operationName An optional operation name for that query.
     * @param variables An optional map of variables for the query.
     * @return The execution result.
     */
    private ExecutionResult execute(String query, String operationName, Map<String, Object> variables) {
        LOGGER.debug("Executing query {}", query);
        Builder builder = new Builder().query(query);
        if (operationName != null) {
            builder.operationName(operationName);
        }
        if (variables != null) {
            builder.variables(variables);
        }
        return graphQL.execute(builder);
    }

    /**
     * Write the result of a GraphQL query execution in the given Servlet response.
     * 
     * @param executionResult The GraphQL query execution result.
     * @param response The Servlet response.
     * @throws IOException If an I/O error occurs.
     */
    private void writeResponse(ExecutionResult executionResult, SlingHttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        Map<String, Object> spec = executionResult.toSpecification();
        String json = gson.toJson(spec);
        IOUtils.write(json, response.getOutputStream(), StandardCharsets.UTF_8);
    }

    /**
     * Reads the given resource file and returns the content.
     * 
     * @param filename The name of the file.
     * @return The string content of the file.
     * @throws IOException If an I/O error occurs.
     */
    private String readResource(String filename) throws IOException {
        return IOUtils.toString(GraphqlServlet.class.getClassLoader().getResourceAsStream("graphql/" + filename), StandardCharsets.UTF_8);
    }

    /**
     * Reads and parses the GraphQL response from the given filename.
     * 
     * @param filename The file with the GraphQL JSON response.
     * @return A parsed GraphQL response object.
     */
    private GraphqlResponse<Query, Error> readGraphqlResponse(String filename) {
        if (graphqlResponsesCache.containsKey(filename)) {
            return graphqlResponsesCache.get(filename);
        }

        String json = null;
        try {
            json = readResource(filename);
        } catch (IOException e) {
            LOGGER.error("Cannot read GraphQL response from " + filename, e);
            return null;
        }
        Type type = TypeToken.getParameterized(GraphqlResponse.class, Query.class, Error.class).getType();
        GraphqlResponse<Query, Error> graphqlResponse = QueryDeserializer.getGson().fromJson(json, type);
        graphqlResponsesCache.put(filename, graphqlResponse);
        return graphqlResponse;
    }

    /**
     * Initialises and parses the GraphQL schema.
     * 
     * @return The registry of type definitions.
     * @throws IOException If an I/O error occurs.
     */
    @SuppressWarnings("unchecked")
    private TypeDefinitionRegistry buildTypeDefinitionRegistry() throws IOException {
        String json = readResource("magento-luma-schema-2.3.5.json");

        Type type = TypeToken.getParameterized(Map.class, String.class, Object.class).getType();
        Map<String, Object> map = gson.fromJson(json, type);
        Map<String, Object> data = (Map<String, Object>) map.get("data");

        Document document = new IntrospectionResultToSchema().createSchemaDefinition(data);
        String sdl = new SchemaPrinter().print(document);
        return new SchemaParser().parse(sdl);
    }

    /**
     * Configures and builds the execution engine of the GraphQL server.
     * 
     * @return The runtime wiring of the server.
     * @throws IOException If an I/O error occurs.
     */
    private RuntimeWiring buildRuntimeWiring() throws IOException {

        // Fields like url_key cannot be fetched because graphql-java calls the method getUrl_key()
        // because the models have camel-case methods like getUrlKey()
        // --> we hence use a default fetcher that calls AbstractResponse.get("url_key")
        // This is also going to be much more efficient than the default PropertyDataFetcher from graphql-java

        DataFetcher<Object> defaultDataFetcher = new DataFetcher<Object>() {
            @Override
            public Object get(DataFetchingEnvironment env) {
                Object obj = env.getSource();
                String name = env.getField().getName();
                return ((AbstractResponse<?>) obj).get(name);
            }
        };

        TypeResolver typeResolver = new TypeResolver() {
            @Override
            public GraphQLObjectType getType(TypeResolutionEnvironment env) {
                Object obj = env.getObject();
                return env.getSchema().getObjectType(obj.getClass().getSimpleName()); // Java classes have the same name as GraphQL types
            }
        };

        WiringFactory wiringFactory = new WiringFactory() {

            @Override
            public DataFetcher<?> getDefaultDataFetcher(FieldWiringEnvironment env) {
                return defaultDataFetcher;
            }

            // We provide a default typeResolver for all GraphQL interfaces

            @Override
            public boolean providesTypeResolver(InterfaceWiringEnvironment env) {
                return true;
            }

            @Override
            public TypeResolver getTypeResolver(InterfaceWiringEnvironment env) {
                return typeResolver;
            }
        };

        DataFetcher<Object> staticDataFetcher = new DataFetcher<Object>() {
            @Override
            public Object get(DataFetchingEnvironment env) {
                String fieldName = env.getField().getName();
                String fieldAlias = env.getField().getAlias();
                LOGGER.debug("Field: {} {}", fieldName, StringUtils.isNotBlank(fieldAlias) ? ("(Alias: " + fieldAlias + ")") : "");
                Map<String, Object> args = env.getArguments();
                if (MapUtils.isNotEmpty(args)) {
                    args.forEach((key, value) -> LOGGER.debug("Arg: {} --> {} ({})", key, value, value.getClass()));
                }
                switch (fieldName) {
                    case "products": {
                        return readProductsResponse(env);
                    }
                    case "storeConfig": {
                        GraphqlResponse<Query, Error> graphqlResponse = readGraphqlResponse("magento-graphql-storeconfig.json");
                        return graphqlResponse.getData().getStoreConfig();
                    }
                    case "category": {
                        return readCategoryResponse(env);
                    }
                    case "customAttributeMetadata": {
                        GraphqlResponse<Query, Error> graphqlResponse = readGraphqlResponse("magento-graphql-attributes.json");
                        return graphqlResponse.getData().getCustomAttributeMetadata();
                    }
                    default:
                        return null;
                }
            }
        };

        return RuntimeWiring.newRuntimeWiring()
            .wiringFactory(wiringFactory)
            .type("Query", builder -> builder.dataFetcher("products", staticDataFetcher))
            .type("Query", builder -> builder.dataFetcher("storeConfig", staticDataFetcher))
            .type("Query", builder -> builder.dataFetcher("category", staticDataFetcher))
            .type("Query", builder -> builder.dataFetcher("customAttributeMetadata", staticDataFetcher))
            .build();
    }

    /**
     * Based on the GraphQL query, this method returns a Magento <code>Products</code> object
     * that "matches" the data expected by each CIF component. Each CIF component indeed expects a
     * specific JSON response. Luckily, each GraphQL query sent by each component is different so
     * we can "detect" what response should be returned.
     * 
     * @param env The metadata of the GraphQL query.
     * @return A Magento <code>Products</code> object.
     */
    private Products readProductsResponse(DataFetchingEnvironment env) {

        DataFetchingFieldSelectionSet selectionSet = env.getSelectionSet();
        if (selectionSet.contains("items/related_products")) {
            return readProductsFrom("magento-graphql-relatedproducts.json");
        } else if (selectionSet.contains("items/upsell_products")) {
            return readProductsFrom("magento-graphql-upsellproducts.json");
        } else if (selectionSet.contains("items/crosssell_products")) {
            return readProductsFrom("magento-graphql-crosssellproducts.json");
        }

        Map<String, Object> args = env.getArguments();
        // We return different responses based on the products filter argument
        Object productsFilter = args.get(PRODUCTS_FILTER_ARG);
        if (productsFilter != null) {
            String filter = productsFilter.toString();
            if (filter.matches(SKU_IN_REGEX)) {
                return readProductsFrom("magento-graphql-productcarousel.json");
            } else if (filter.matches(SKU_EQ_REGEX)) {
                return readProductsFrom("magento-graphql-productteaser.json");
            } else if (filter.matches(CATEGORY_ID_REGEX)) {
                return readProductsFrom("magento-graphql-category-products.json");
            }
        }

        if (args.containsKey(PRODUCTS_SEARCH_ARG)) {
            return readProductsFrom("magento-graphql-searchresults.json");
        }

        return readProductsFrom("magento-graphql-products.json");
    }

    /**
     * Reads the JSON of the given file and deserialises the content in a Magento <code>Products</code> object.
     * 
     * @param filename The file that contains the products JSON response.
     * @return A Magento <code>Products</code> object.
     */
    private Products readProductsFrom(String filename) {
        GraphqlResponse<Query, Error> graphqlResponse = readGraphqlResponse(filename);
        return graphqlResponse.getData().getProducts();
    }

    /**
     * Based on the GraphQL query, this method returns a Magento <code>CategoryTree</code> object
     * that "matches" the data expected by each CIF component. Each CIF component indeed expects a
     * specific JSON response. Luckily, each GraphQL query sent by each component is different so
     * we can "detect" what response should be returned.
     * 
     * @param env The metadata of the GraphQL query.
     * @return A Magento <code>CategoryTree</code> object.
     */
    private CategoryTree readCategoryResponse(DataFetchingEnvironment env) {

        // If the query has aliases, it's the FeaturedCategoryList component
        String fieldAlias = env.getField().getAlias();
        if (StringUtils.isNotBlank(fieldAlias)) {
            GraphqlResponse<Query, Error> graphqlResponse = readGraphqlResponse("magento-graphql-featuredcategorylist.json");
            return (CategoryTree) graphqlResponse.getData().get(fieldAlias);
        }

        // Default query is fetching the category tree except if the category id is not "2"
        String filename = "magento-graphql-categories.json";

        DataFetchingFieldSelectionSet selectionSet = env.getSelectionSet();
        if (selectionSet.contains("products")) {
            filename = "magento-graphql-category-products.json"; // Query to fetch category products
        } else {
            Object id = env.getArgument(CATEGORY_ID_ARG);
            if (id != null && !id.toString().equals("2")) {
                filename = "magento-graphql-category.json"; // Query to only fetch some category data
            }
        }

        GraphqlResponse<Query, Error> graphqlResponse = readGraphqlResponse(filename);
        return graphqlResponse.getData().getCategory();
    }
}
