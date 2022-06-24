/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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
package com.adobe.cq.commerce.core.testing;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.HttpUriRequest;
import org.mockito.ArgumentMatcher;

import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.google.gson.Gson;

/**
 * Matcher class used to match a GraphQL query. This is used to properly mock GraphQL responses.
 */
public class GraphqlQueryMatcher extends ArgumentMatcher<HttpUriRequest> {

    private String contains;
    private String method;

    public GraphqlQueryMatcher(String contains) {
        this(contains, null);
    }

    public GraphqlQueryMatcher(String contains, String method) {
        this.contains = contains;
        this.method = method;
    }

    @Override
    public boolean matches(Object obj) {
        if (!(obj instanceof HttpUriRequest)) {
            return false;
        }

        HttpUriRequest httpUriRequest = (HttpUriRequest) obj;

        if (method != null) {
            if (!httpUriRequest.getMethod().equalsIgnoreCase(method)) {
                return false;
            }
        }

        if (httpUriRequest instanceof HttpEntityEnclosingRequest) {
            // GraphQL query is in POST body
            HttpEntityEnclosingRequest httpEntityEnclosingRequest = (HttpEntityEnclosingRequest) httpUriRequest;
            try {
                String body = IOUtils.toString(httpEntityEnclosingRequest.getEntity().getContent(), StandardCharsets.UTF_8);
                Gson gson = new Gson();
                GraphqlRequest graphqlRequest = gson.fromJson(body, GraphqlRequest.class);
                return graphqlRequest.getQuery().contains(contains);
            } catch (Exception e) {
                return false;
            }
        } else {
            // GraphQL query is in the URL 'query' parameter
            HttpUriRequest req = (HttpUriRequest) obj;
            String uri = null;
            try {
                uri = URLDecoder.decode(req.getURI().toString(), StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                return false;
            }
            String graphqlQuery = uri.substring(uri.indexOf("?query=") + 7);
            return graphqlQuery.contains(contains);
        }
    }

}
