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
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.PropertyDataFetcher;
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

    private Gson gson;
    private GraphQL graphQL;

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

        // To DEBUG: set 'notprivacysafe.graphql.GraphQL' to DEBUG level
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
            vars = (Map<String, Object>) graphqlRequest.getVariables(); // TODO: convert?
        }

        // To DEBUG: set 'notprivacysafe.graphql.GraphQL' to DEBUG level
        ExecutionResult executionResult = execute(graphqlRequest.getQuery(), graphqlRequest.getOperationName(), vars);
        writeResponse(executionResult, response);
    }

    private ExecutionResult execute(String query, String operationName, Map<String, Object> variables) {
        Builder builder = new Builder().query(query);
        if (operationName != null) {
            builder.operationName(operationName);
        }
        if (variables != null) {
            builder.variables(variables);
        }
        return graphQL.execute(builder);
    }

    private void writeResponse(ExecutionResult executionResult, SlingHttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        Map<String, Object> spec = executionResult.toSpecification();
        String json = gson.toJson(spec);
        IOUtils.write(json, response.getOutputStream(), StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private TypeDefinitionRegistry buildTypeDefinitionRegistry() throws IOException {
        String filename = "magento-schema-2.3.4.json";
        String json = IOUtils.toString(GraphqlServlet.class.getClassLoader().getResourceAsStream(filename), StandardCharsets.UTF_8);

        Type type = TypeToken.getParameterized(Map.class, String.class, Object.class).getType();
        Map<String, Object> map = gson.fromJson(json, type);
        Map<String, Object> data = (Map<String, Object>) map.get("data");

        Document document = new IntrospectionResultToSchema().createSchemaDefinition(data);
        String sdl = new SchemaPrinter().print(document);
        return new SchemaParser().parse(sdl);
    }

    private GraphqlResponse<Query, Error> readGraphqlResponse(String filename) {
        String json = null;
        try {
            json = IOUtils.toString(GraphqlServlet.class.getClassLoader().getResourceAsStream(filename), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Cannot read GraphQL response from " + filename, e);
            return null;
        }
        Type type = TypeToken.getParameterized(GraphqlResponse.class, Query.class, Error.class).getType();
        return QueryDeserializer.getGson().fromJson(json, type);
    }

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
                if (obj instanceof AbstractResponse<?>) {
                    return ((AbstractResponse<?>) obj).get(name);
                }
                return new PropertyDataFetcher<>(name).get(env); // use default property fetcher
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
                LOGGER.debug("Field: " + fieldName);
                Map<String, Object> args = env.getArguments();
                if (MapUtils.isNotEmpty(args)) {
                    args.forEach((key, value) -> LOGGER.debug("Arg: " + key + " --> " + value + " (" + value.getClass() + ")"));
                }
                switch (fieldName) {
                    case "products": {
                        GraphqlResponse<Query, Error> graphqlResponse = readGraphqlResponse("magento-graphql-products-search.json");
                        return graphqlResponse.getData().getProducts();
                    }
                    case "storeConfig": {
                        GraphqlResponse<Query, Error> graphqlResponse = readGraphqlResponse("magento-graphql-storeconfig.json");
                        return graphqlResponse.getData().getStoreConfig();
                    }
                    case "category": {
                        GraphqlResponse<Query, Error> graphqlResponse = readGraphqlResponse("magento-graphql-categories.json");
                        return graphqlResponse.getData().getCategory();
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
            .build();
    }
}
