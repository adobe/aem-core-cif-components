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
package com.adobe.cq.commerce.core.components.internal.services;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.services.ComponentsConfigurationProvider;
import com.google.common.collect.ImmutableMap;

@Component
public class ComponentsConfigurationProviderImpl implements ComponentsConfigurationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentsConfigurationProviderImpl.class);

    private static final String SUBSERVICE_NAME = "cif-components-configuration";

    private static final Map<String, Object> authInfo = ImmutableMap.of(ResourceResolverFactory.SUBSERVICE, SUBSERVICE_NAME);

    private static final String CONFIGURATION_NAME = "cloudconfigs/commerce";

    @Reference(target = "(" + ServiceUserMapped.SUBSERVICENAME + "=" + SUBSERVICE_NAME + ")")
    private ServiceUserMapped serviceUserMapped;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private ConfigurationResourceResolver configurationResourceResolver;

    private ResourceResolver serviceResolver;

    protected void activate(ComponentContext context) {
        try {
            serviceResolver = resolverFactory.getServiceResourceResolver(authInfo);
        } catch (LoginException e) {
            throw new RuntimeException();
        }
    }

    protected void deactivate() {
        serviceResolver.close();
    }

    @Override
    @Nonnull
    public ValueMap getContextAwareConfigurationProperties(@Nonnull String path) {
        if (StringUtils.isEmpty(path)) {
            LOG.warn("Empty path supplied, nothing to do here");
            return ValueMap.EMPTY;
        }

        Resource res = serviceResolver.getResource(path);
        if (res == null) {
            LOG.warn("No resource found at {}", path);
            return ValueMap.EMPTY;
        }
        ConfigurationBuilder cfgBuilder = res.adaptTo(ConfigurationBuilder.class);

        return cfgBuilder.name(CONFIGURATION_NAME).asValueMap();

    }

    @Override
    @Nullable
    public Resource getContextConfigurationResource(@Nonnull String path) {
        if (StringUtils.isEmpty(path)) {
            LOG.warn("Empty path supplied, nothing to do here");
            return null;
        }

        Resource res = serviceResolver.getResource(path);
        if (res == null) {
            LOG.warn("No resource found at {}", path);
            return null;
        }

        Resource configurationResource = configurationResourceResolver.getResource(res, "settings", CONFIGURATION_NAME);
        return configurationResource.isResourceType("cq:Page") ? configurationResource.getChild("jcr:content") : configurationResource;

    }
}
