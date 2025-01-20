/*******************************************************************************
 *
 *    Copyright 2025 Adobe. All rights reserved.
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
package com.adobe.cq.commerce.core.cacheinvalidation.internal;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.cacheinvalidation.config.CacheInvalidationConfig;

@Component(service = CacheInvalidationService.class, immediate = true)
@Designate(ocd = CacheInvalidationConfig.class)
public class CacheInvalidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheInvalidationService.class);

    private boolean enableCacheInvalidation;

    @Activate
    @Modified
    protected void activate(CacheInvalidationConfig config) {
        this.enableCacheInvalidation = config.enableCacheInvalidation();
        LOGGER.info("Cache Invalidation enabled: {}", enableCacheInvalidation);
    }


    public boolean isCacheInvalidationEnabled() {
        return enableCacheInvalidation;
    }
}
