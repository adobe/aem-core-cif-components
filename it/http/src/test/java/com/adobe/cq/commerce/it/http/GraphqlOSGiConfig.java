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

package com.adobe.cq.commerce.it.http;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds the OSGI config to be updated from the ITs.
 */
public class GraphqlOSGiConfig {

    Map<String, Object> config;

    public GraphqlOSGiConfig() {
        config = new HashMap<>();
    }

    public Map<String, Object> build() {
        return this.config;
    }

    public GraphqlOSGiConfig withIdentifier(String identifier) {
        config.put("identifier", identifier);
        return this;
    }

    public GraphqlOSGiConfig withUrl(String url) {
        config.put("url", url);
        return this;
    }

    public GraphqlOSGiConfig withHttpMethod(String httpMethod) {
        config.put("httpMethod", httpMethod);
        return this;
    }

    public GraphqlOSGiConfig withAcceptSelfSignedCertificates(boolean acceptSelfSignedCertificates) {
        config.put("acceptSelfSignedCertificates", Boolean.valueOf(acceptSelfSignedCertificates).toString());
        return this;
    }

    public GraphqlOSGiConfig withAllowHttpProtocol(boolean allowHttpProtocol) {
        config.put("allowHttpProtocol", Boolean.valueOf(allowHttpProtocol).toString());
        return this;
    }
}
