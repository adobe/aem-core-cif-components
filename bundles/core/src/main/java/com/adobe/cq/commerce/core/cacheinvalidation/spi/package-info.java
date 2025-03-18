/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2025 Adobe
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

/**
 * <p>
 * Experimental package providing Service Provider Interface (SPI) for cache invalidation.
 * </p>
 * 
 * <p>
 * This package defines interfaces and contracts that allow third-party implementations to integrate
 * with AEM Commerce Core's cache invalidation system. It enables custom cache invalidation strategies
 * and mechanisms to be plugged into the core commerce functionality.
 * </p>
 * 
 * <p>
 * <strong>Note:</strong> This is an experimental API that may change in future releases.
 * </p>
 */
@Version("0.0.1")
package com.adobe.cq.commerce.core.cacheinvalidation.spi;

import org.osgi.annotation.versioning.Version;
