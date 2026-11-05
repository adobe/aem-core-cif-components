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
package com.adobe.cq.commerce.core.components.internal.servlets;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "CIF Catalog Page Not Found Filter")
public @interface CatalogPageNotFoundFilterConfiguration {

    @AttributeDefinition(
        name = "Service unavailable on backend error",
        description = "Return HTTP 503 for catalog transport failures in disabled WCM mode. Disable to restore the previous behavior.")
    boolean serviceUnavailableOnBackendError() default true;

    @AttributeDefinition(
        name = "Retry after seconds",
        description = "Retry-After value for HTTP 503 responses. Zero or negative values omit the header.")
    int retryAfterSeconds() default 0;
}
