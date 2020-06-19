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

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.apache.sling.testing.mock.caconfig.ContextPlugins;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextBuilder;

public class ComponentsConfigurationAdapterFactoryTest {

    @Rule
    public final AemContext context = new AemContextBuilder(ResourceResolverType.JCR_MOCK).plugin(ContextPlugins.CACONFIG)
        .beforeSetUp(context -> {
            ConfigurationAdmin configurationAdmin = context.getService(ConfigurationAdmin.class);
            Configuration serviceConfiguration = configurationAdmin.getConfiguration(
                "org.apache.sling.caconfig.resource.impl.def.DefaultContextPathStrategy");

            Dictionary<String, Object> props = new Hashtable<>();
            props.put("configRefResourceNames", new String[] { ".", "jcr:content" });
            props.put("configRefPropertyNames", "cq:conf");
            serviceConfiguration.update(props);

            serviceConfiguration = configurationAdmin.getConfiguration(
                "org.apache.sling.caconfig.resource.impl.def.DefaultConfigurationResourceResolvingStrategy");
            props = new Hashtable<>();
            props.put("configPath", "/conf");
            serviceConfiguration.update(props);

            serviceConfiguration = configurationAdmin.getConfiguration("org.apache.sling.caconfig.impl.ConfigurationResolverImpl");
            props = new Hashtable<>();
            props.put("configBucketNames", new String[] { "settings" });
            serviceConfiguration.update(props);
        }).build();

    @Before
    public void setup() {
        context.load().json("/context/jcr-conf.json", "/conf/testing");
        context.load().json("/context/jcr-content.json", "/content/my-site");
        ResourceResolverFactory resourceResolverFactory = Mockito.mock(ResourceResolverFactory.class);
        ServiceUserMapped serviceUserMapped = Mockito.mock(ServiceUserMapped.class);

        context.registerService(ServiceUserMapped.class, serviceUserMapped, ImmutableMap.of(ServiceUserMapped.SUBSERVICENAME,
            "cif-components-configuration"));

        ComponentsConfigurationAdapterFactory factory = new ComponentsConfigurationAdapterFactory();

        context.registerInjectActivateService(factory);
    }

    @Test
    public void testAdaptFromResource() {
        Resource resource = context.resourceResolver().getResource("/content/my-site/pageH");
        ComponentsConfiguration configuration = resource.adaptTo(ComponentsConfiguration.class);

        Assert.assertNotNull("Configuration is not null", configuration);
        Assert.assertTrue("The configuration has some data in it", configuration.size() > 0);

        String unrelatedProperty = configuration.get("aTotallyUnrelatedProperty", String.class);
        Assert.assertEquals("The configuration is correct", unrelatedProperty, "true");
    }

    @Test
    public void testAdaptFromResourceWithoutContextConfig() {
        Resource resource = context.resourceResolver().getResource("/content/my-site/pageD");

        ComponentsConfiguration configuration = resource.adaptTo(ComponentsConfiguration.class);

        Assert.assertNotNull("Configuration is not null", configuration);
        Assert.assertEquals("The configuration has no data in it", 0, configuration.size());
    }

    @Test
    public void testAdaptNullResource() {
        ComponentsConfiguration configuration = context.resourceResolver().adaptTo(ComponentsConfiguration.class);
        Assert.assertNull(configuration);
    }
}
