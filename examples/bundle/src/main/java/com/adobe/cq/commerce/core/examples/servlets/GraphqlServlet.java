/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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
package com.adobe.cq.commerce.core.examples.servlets;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern CATEGORY_UID_EQ_PATTERN = Pattern.compile("\\{category_uid=\\{eq=(.+)\\}\\}");
    private static final Pattern SKU_IN_PATTERN = Pattern.compile("\\{sku=\\{in=\\[(.+)\\]\\}\\}");
    private static final Pattern SKU_EQ_PATTERN = Pattern.compile("\\{sku=\\{eq=(.+)\\}\\}");
    private static final Pattern URL_KEY_EQ_PATTERN = Pattern.compile("\\{url_key=\\{eq=(.+)\\}\\}");

    private static final String CATEGORY_UID = "uid-1";
    private static final String CATEGORY_STAGED_PRODUCTS_UID = "uid-2";
    private static final String CATEGORY_PRODUCT_CAROUSEL_UID = "uid-3";

    private static final String STAGED_PRODUCT_URL_KEY = "chaz-crocodile-hoodie";
    private static final String STAGED_PRODUCT_SKU = "MH02";

    private static final String PRODUCT_SKU = "MH01";
    private static final String UNKNOWN_PRODUCT_URL_KEY = "unknown-product";

    private static final String GROUPED_PRODUCT_URL_KEY = "set-of-sprite-yoga-straps";
    private static final String GROUPED_PRODUCT_SKU = "24-WG085_Group";

    private static final String BUNDLE_PRODUCT_URL_KEY = "sprite-yoga-companion-kit";
    private static final String BUNDLE_PRODUCT_SKU = "24-WG080";

    private static final String ATTRIBUTES_JSON = "magento-graphql-attributes.json";
    private static final String RELATED_PRODUCTS_JSON = "magento-graphql-relatedproducts.json";
    private static final String UPSELL_PRODUCTS_JSON = "magento-graphql-upsellproducts.json";
    private static final String CROSSSELL_PRODUCTS_JSON = "magento-graphql-crosssellproducts.json";
    private static final String PRODUCT_CAROUSEL_JSON = "magento-graphql-productcarousel.json";
    private static final String PRODUCT_CAROUSEL_CATEGORY_JSON = "magento-graphql-productcarousel-category.json";
    private static final String PRODUCT_TEASER_JSON = "magento-graphql-productteaser.json";
    private static final String PRODUCTS_COLLECTION_JSON = "magento-graphql-products-collection.json";
    private static final String PRODUCTS_COLLECTION_WITH_STAGED_PRODUCTS_JSON = "magento-graphql-products-collection-with-staged-products.json";
    private static final String GROUPED_PRODUCT_JSON = "magento-graphql-grouped-product.json";
    private static final String PRODUCTS_JSON = "magento-graphql-products.json";
    private static final String CATEGORY_LIST_TREE_JSON = "magento-graphql-categories-list.json";
    private static final String CATEGORY_UID_JSON = "magento-graphql-category.json";
    private static final String CATEGORY_JSON = "magento-graphql-category.json";
    private static final String CATEGORY_WITH_STAGED_PRODUCTS_JSON = "magento-graphql-category-with-staged-products.json";
    private static final String UNKNOWN_CATEGORY_JSON = "magento-graphql-category-empty.json";
    private static final String FEATURED_CATEGORY_LIST_JSON = "magento-graphql-featuredcategorylist.json";
    private static final String CATEGORIES_CAROUSEL_JSON = "magento-graphql-categories-carousel.json";
    private static final String PRODUCTS_BREADCRUMB_JSON = "magento-graphql-products-breadcrumb.json";
    private static final String CATEGORYLIST_BREADCRUMB_JSON = "magento-graphql-categorylist-breadcrumb.json";
    private static final String BUNDLE_PRODUCT_JSON = "magento-graphql-bundle-product.json";
    private static final String BUNDLE_PRODUCT_ITEMS_JSON = "magento-graphql-bundle-product-items.json";
    private static final String STAGED_PRODUCT_JSON = "magento-graphql-products-staged.json";
    private static final String UNKNOWN_PRODUCT_JSON = "magento-graphql-products-empty.json";

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
        String json = readResource("magento-schema-2.4.2ee.json");

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
                LOGGER.debug("Field: {} {}", fieldName, StringUtils.isNotBlank(fieldAlias) ? ("(Alias: " + fieldAlias + ")") : "");
                Map<String, Object> args = env.getArguments();
                if (MapUtils.isNotEmpty(args)) {
                    args.forEach((key, value) -> LOGGER.debug("Arg: {} --> {} ({})", key, value, value.getClass()));
                }
                switch (fieldName) {
                    case "products": {
                        return readProductsResponse(env);
                    }
                    case "categoryList": {
                        return readCategoryListResponse(env);
                    }
                    case "customAttributeMetadata": {
                        GraphqlResponse<Query, Error> graphqlResponse = readGraphqlResponse(ATTRIBUTES_JSON);
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
            .type("Query", builder -> builder.dataFetcher("category", staticDataFetcher))
            .type("Query", builder -> builder.dataFetcher("categoryList", staticDataFetcher))
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
            return readProductsFrom(RELATED_PRODUCTS_JSON);
        } else if (selectionSet.contains("items/upsell_products")) {
            return readProductsFrom(UPSELL_PRODUCTS_JSON);
        } else if (selectionSet.contains("items/crosssell_products")) {
            return readProductsFrom(CROSSSELL_PRODUCTS_JSON);
        } else if (selectionSet.contains("items/categories/breadcrumbs")) {
            return readProductsFrom(PRODUCTS_BREADCRUMB_JSON);
        }

        Map<String, Object> args = env.getArguments();
        // We return different responses based on the products filter argument
        Object productsFilter = args.get(PRODUCTS_FILTER_ARG);
        if (productsFilter != null) {
            String filter = productsFilter.toString();
            Matcher skuInMatcher = SKU_IN_PATTERN.matcher(filter);
            Matcher skuEqMatcher = SKU_EQ_PATTERN.matcher(filter);
            Matcher urlKeyEqPattern = URL_KEY_EQ_PATTERN.matcher(filter);
            Matcher uidPattern = CATEGORY_UID_EQ_PATTERN.matcher(filter);

            if (skuInMatcher.matches()) {
                // The filter {sku:{in:[...]}} can be a query for the carousel (3 skus) or a client-side query to fetch prices
                // on the product (1 sku), productlist (6 skus), or search pages (6 skus)
                String[] skus = skuInMatcher.group(1).split(",");
                LOGGER.debug("Got sku:in filter with {} sku(s): {}", skus.length, skuInMatcher.group(1));
                if (skus.length == 1) {
                    if (GROUPED_PRODUCT_SKU.equals(skus[0])) {
                        return readProductsFrom(GROUPED_PRODUCT_JSON);
                    } else if (BUNDLE_PRODUCT_SKU.equals(skus[0])) {
                        return readProductsFrom(BUNDLE_PRODUCT_JSON);
                    }
                    return readProductsFrom(PRODUCTS_JSON);
                } else if (skus.length == 6) {
                    return readProductsFrom(PRODUCTS_COLLECTION_JSON);
                }
                return readProductsFrom(PRODUCT_CAROUSEL_JSON);
            } else if (skuEqMatcher.matches()) {
                if (skuEqMatcher.group(1).equals(BUNDLE_PRODUCT_SKU)) {
                    if (selectionSet.contains("items/description")) {
                        return readProductsFrom(BUNDLE_PRODUCT_JSON);
                    }
                    return readProductsFrom(BUNDLE_PRODUCT_ITEMS_JSON);
                } else if (skuEqMatcher.group(1).equals(GROUPED_PRODUCT_SKU)) {
                    return readProductsFrom(GROUPED_PRODUCT_JSON);
                } else if (skuEqMatcher.group(1).equals(PRODUCT_SKU)) {
                    return readProductsFrom(PRODUCTS_JSON);
                } else if (skuEqMatcher.group(1).equals(STAGED_PRODUCT_SKU)) {
                    return readProductsFrom(STAGED_PRODUCT_JSON);
                }
                return readProductsFrom(PRODUCT_TEASER_JSON);
            } else if (uidPattern.matches()) {
                if (CATEGORY_UID.equals(uidPattern.group(1))) {
                    return readProductsFrom(PRODUCTS_COLLECTION_JSON);
                } else if (CATEGORY_STAGED_PRODUCTS_UID.equals(uidPattern.group(1))) {
                    return readProductsFrom(PRODUCTS_COLLECTION_WITH_STAGED_PRODUCTS_JSON);
                }
            } else if (urlKeyEqPattern.matches()) {
                if (GROUPED_PRODUCT_URL_KEY.equals(urlKeyEqPattern.group(1))) {
                    return readProductsFrom(GROUPED_PRODUCT_JSON);
                } else if (BUNDLE_PRODUCT_URL_KEY.equals(urlKeyEqPattern.group(1))) {
                    return readProductsFrom(BUNDLE_PRODUCT_JSON);
                } else if (STAGED_PRODUCT_URL_KEY.equals(urlKeyEqPattern.group(1))) {
                    return readProductsFrom(STAGED_PRODUCT_JSON);
                } else if (UNKNOWN_PRODUCT_URL_KEY.equals(urlKeyEqPattern.group(1))) {
                    return readProductsFrom(UNKNOWN_PRODUCT_JSON);
                }
            }
        }

        if (args.containsKey(PRODUCTS_SEARCH_ARG)) {
            return readProductsFrom(PRODUCTS_COLLECTION_JSON);
        }

        return readProductsFrom(PRODUCTS_JSON);
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
     * Based on the GraphQL query, this method returns a list of Magento <code>CategoryTree</code> objects
     * that "matches" the data expected by each CIF component. Each CIF component indeed expects a
     * specific JSON response. Luckily, each GraphQL query sent by each component is different so
     * we can "detect" what response should be returned.
     *
     * @param env The metadata of the GraphQL query.
     * @return A list of Magento <code>CategoryTree</code> objects.
     */
    private List<CategoryTree> readCategoryListResponse(DataFetchingEnvironment env) {

        GraphqlResponse<Query, Error> graphqlResponse = null;
        Map<String, Map<String, Object>> filters = env.getArgument("filters");
        DataFetchingFieldSelectionSet selectionSet = env.getSelectionSet();

        // Only category the Breadcrumb components selects this field
        if (selectionSet.contains("breadcrumbs")) {
            graphqlResponse = readGraphqlResponse(CATEGORYLIST_BREADCRUMB_JSON);
        } else if (filters.containsKey("url_key") && filters.get("url_key").containsKey("eq")) {
            if (filters.get("url_key").get("eq").equals("outdoor")) {
                // The URLProvider example will return category uid
                graphqlResponse = readGraphqlResponse(CATEGORY_UID_JSON);
            } else if (filters.get("url_key").get("eq").equals("outdoor-staged")) {
                // The URLProvider example will return category uid
                graphqlResponse = readGraphqlResponse(CATEGORY_WITH_STAGED_PRODUCTS_JSON);
            } else if (filters.get("url_key").get("eq").equals("unknown-category")) {
                // return empty response
                graphqlResponse = readGraphqlResponse(UNKNOWN_CATEGORY_JSON);
            } else {
                graphqlResponse = readGraphqlResponse(CATEGORY_UID_JSON);
            }
        } else if (filters.containsKey("category_uid")) {
            if (filters.get("category_uid").containsKey("in") && (((List<String>) (filters.get("category_uid").get("in"))).size() == 4)) {
                // The CategoriesCarousel example will require 4 items
                graphqlResponse = readGraphqlResponse(CATEGORIES_CAROUSEL_JSON);
            } else if (filters.get("category_uid").containsKey("eq")) {
                if (filters.get("category_uid").get("eq").equals("uid-1")) {
                    // The ProductList example will require 1 item
                    graphqlResponse = readGraphqlResponse(CATEGORY_JSON);
                } else if (filters.get("category_uid").get("eq").equals("uid-2")) {
                    graphqlResponse = readGraphqlResponse(CATEGORY_WITH_STAGED_PRODUCTS_JSON);
                } else if (filters.get("category_uid").get("eq").equals("Mg==")) {
                    // The navigation example will require item "Mg==" as the default root category
                    graphqlResponse = readGraphqlResponse(CATEGORY_LIST_TREE_JSON);
                } else if (filters.get("category_uid").get("eq").equals(CATEGORY_PRODUCT_CAROUSEL_UID)) {
                    // The products carousel with category example will require "uid-3" as the category uid
                    graphqlResponse = readGraphqlResponse(PRODUCT_CAROUSEL_CATEGORY_JSON);
                }
            }
        }

        if (graphqlResponse == null) {
            graphqlResponse = readGraphqlResponse(FEATURED_CATEGORY_LIST_JSON);
        }

        return graphqlResponse.getData().getCategoryList();
    }
}
