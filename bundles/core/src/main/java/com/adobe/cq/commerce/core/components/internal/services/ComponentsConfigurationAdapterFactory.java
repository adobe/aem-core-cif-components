/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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
package com.adobe.cq.commerce.core.components.internal.services;

import java.util.Collections;
import java.util.Map;

import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.wcm.launches.utils.LaunchUtils;

@Component(
    service = { AdapterFactory.class },
    property = {
        AdapterFactory.ADAPTABLE_CLASSES + "=" + ComponentsConfigurationAdapterFactory.RESOURCE_CLASS_NAME,
        AdapterFactory.ADAPTER_CLASSES + "=" + ComponentsConfigurationAdapterFactory.COMPONENTS_CONFIGURATION_CLASS_NAME })
public class ComponentsConfigurationAdapterFactory implements AdapterFactory {

    protected static final String RESOURCE_CLASS_NAME = "org.apache.sling.api.resource.Resource";
    protected static final String COMPONENTS_CONFIGURATION_CLASS_NAME = "com.adobe.cq.commerce.core.components.services.ComponentsConfiguration";

    private static final String SUBSERVICE_NAME = "cif-components-configuration";
    private static final Logger LOG = LoggerFactory.getLogger(ComponentsConfigurationAdapterFactory.class);

    private static final Map<String, Object> authInfo = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SUBSERVICE_NAME);

    private static final String CONFIGURATION_NAME = "cloudconfigs/commerce";

    @Reference(target = "(" + ServiceUserMapped.SUBSERVICENAME + "=" + SUBSERVICE_NAME + ")")
    private ServiceUserMapped serviceUserMapped;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
        if (!(adaptable instanceof Resource)) {
            return null;
        }
        try (ResourceResolver serviceResolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            String resourcePath = ((Resource) adaptable).getPath();
            Resource resource = serviceResolver.getResource(resourcePath);

            if (resource == null) {
                LOG.debug("Service user permissions of {} are not sufficient to view resource at {}", serviceResolver.getUserID(),
                    resourcePath);
                return null;
            }

            if (LaunchUtils.isLaunchBasedPath(resource.getPath())) {
                // In Launches we have to resolve the ComponentConfigurations from the production resource as there is still an issue
                // with CA Configs not working properly in Launches in 6.5.x. Additionally, if the resource was created in the Launch
                // it will not exist in production yet and so the returned target resource is null. Handle that by walking up the tree
                // until we find any resource and try to get the configuration from there.
                Resource sourceResource = resource;
                Resource targetResource = null;
                while (targetResource == null && sourceResource != null) {
                    targetResource = LaunchUtils.getTargetResource(sourceResource, null);
                    sourceResource = sourceResource.getParent();
                }
                if (targetResource != null) {
                    resource = targetResource;
                }
            }

            ConfigurationBuilder cfgBuilder = resource.adaptTo(ConfigurationBuilder.class);
            ComponentsConfiguration configuration = new ComponentsConfiguration(cfgBuilder.name(CONFIGURATION_NAME).asValueMap());
            return (AdapterType) configuration;
        } catch (LoginException e) {
            throw new RuntimeException(e);
        }
    }
}
