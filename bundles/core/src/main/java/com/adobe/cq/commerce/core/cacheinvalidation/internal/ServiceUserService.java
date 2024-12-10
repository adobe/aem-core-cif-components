/*******************************************************************************
 *
 *    Copyright 2024 Adobe. All rights reserved.
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

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = { ServiceUserService.class }, immediate = true)
public class ServiceUserService {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceUserService.class);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    public ResourceResolver getServiceUserResourceResolver(String serviceUser) {

        ResourceResolver resourceResolver = null;
        Map<String, Object> param = new HashMap<>();
        param.put(ResourceResolverFactory.SUBSERVICE, serviceUser);

        try {
            // Get the service user ResourceResolver
            resourceResolver = resourceResolverFactory.getServiceResourceResolver(param);
            LOG.info("Successfully obtained ResourceResolver for service user: {}", serviceUser);
        } catch (LoginException e) {
            LOG.error("Failed to obtain ResourceResolver for service user: {}", serviceUser, e);
        }

        return resourceResolver;
    }
}
