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

import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.google.common.collect.ImmutableMap;

@Component(
    service = { AdapterFactory.class },
    property = {
        AdapterFactory.ADAPTABLE_CLASSES + "=" + ComponentsConfigurationAdapterFactory.RESOURCE_CLASS_NAME,
        AdapterFactory.ADAPTER_CLASSES + "=" + ComponentsConfigurationAdapterFactory.COMPONENTS_CONFIGURATION_CLASS_NAME })
public class ComponentsConfigurationAdapterFactory implements AdapterFactory {

    protected static final String RESOURCE_CLASS_NAME = "org.apache.sling.api.resource.Resource";
    protected static final String COMPONENTS_CONFIGURATION_CLASS_NAME = "com.adobe.cq.commerce.core.components.services.ComponentsConfiguration";

    private static final String SUBSERVICE_NAME = "cif-components-configuration";

    private static final Map<String, Object> authInfo = ImmutableMap.of(ResourceResolverFactory.SUBSERVICE, SUBSERVICE_NAME);

    private static final String CONFIGURATION_NAME = "cloudconfigs/commerce";

    @Reference(target = "(" + ServiceUserMapped.SUBSERVICENAME + "=" + SUBSERVICE_NAME + ")")
    private ServiceUserMapped serviceUserMapped;

    @Reference
    private ResourceResolverFactory resolverFactory;

    private ResourceResolver serviceResolver;

    protected void activate(ComponentContext context) {
        try {
            if (serviceResolver == null) {
                serviceResolver = resolverFactory.getServiceResourceResolver(authInfo);
            }
        } catch (LoginException e) {
            throw new RuntimeException(e);
        }
    }

    protected void deactivate() {
        if (serviceResolver != null) {
            serviceResolver.close();
        }
    }

    @Override
    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
        if (!(adaptable instanceof Resource)) {
            return null;
        }
        Resource res = serviceResolver.getResource(((Resource) adaptable).getPath());
        ConfigurationBuilder cfgBuilder = res.adaptTo(ConfigurationBuilder.class);
        ComponentsConfiguration configuration = new ComponentsConfiguration(cfgBuilder.name(CONFIGURATION_NAME).asValueMap());
        return (AdapterType) configuration;

    }
}
