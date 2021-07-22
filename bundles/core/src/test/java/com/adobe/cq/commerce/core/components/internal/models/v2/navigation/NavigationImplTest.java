/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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
package com.adobe.cq.commerce.core.components.internal.models.v2.navigation;

import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.xss.XSSAPI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.core.components.internal.services.MockUrlProviderConfiguration;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.core.components.testing.Utils;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.adobe.cq.wcm.core.components.models.Navigation;
import com.adobe.cq.wcm.core.components.models.NavigationItem;
import com.adobe.cq.wcm.core.components.testing.MockLanguageManager;
import com.day.cq.wcm.api.LanguageManager;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NavigationImplTest {
    private static final String PAGE_PATH = "/content/pageA";
    private static final String NAVIGATION_PATH = "/content/pageA/jcr:content/root/navigation";

    @Rule
    public final AemContext context = createContext("/context/jcr-content-navigation.json");

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                context.load().json(contentPath, "/content");

                ConfigurationBuilder mockConfigBuilder = Mockito.mock(ConfigurationBuilder.class);
                Utils.addDataLayerConfig(mockConfigBuilder, true);
                context.registerAdapter(Resource.class, ConfigurationBuilder.class, mockConfigBuilder);
            },
            ResourceResolverType.JCR_MOCK);
    }

    @Before
    public void setup() throws Exception {
        Page page = context.currentPage(PAGE_PATH);
        context.currentResource(NAVIGATION_PATH);
        context.request().setContextPath("");

        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(context.resourceResolver().getResource(NAVIGATION_PATH));
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.put(WCMBindingsConstants.NAME_PROPERTIES, context.currentResource().getValueMap());
        XSSAPI xssApi = mock(XSSAPI.class);
        when(xssApi.filterHTML(Mockito.anyString())).then(i -> i.getArgumentAt(0, String.class));
        slingBindings.put("xssApi", xssApi);
        Style style = mock(Style.class);
        when(style.get(Mockito.anyString(), Mockito.anyInt())).then(i -> i.getArgumentAt(1, Object.class));
        slingBindings.put("currentStyle", style);
        SightlyWCMMode wcmMode = mock(SightlyWCMMode.class);
        when(wcmMode.isDisabled()).thenReturn(false);
        slingBindings.put("wcmmode", wcmMode);

        context.registerService(LiveRelationshipManager.class, mock(LiveRelationshipManager.class));
        context.registerService(LanguageManager.class, new MockLanguageManager());
        UrlProviderImpl urlProvider = new UrlProviderImpl();
        urlProvider.activate(new MockUrlProviderConfiguration());
        context.registerService(UrlProvider.class, urlProvider);
    }

    @Test
    public void testNavigation() {
        Navigation navigation = context.request().adaptTo(Navigation.class);

        Assert.assertNotNull(navigation);
        Assert.assertEquals(NavigationImpl.RESOURCE_TYPE, navigation.getExportedType());
        Assert.assertNotNull(navigation.getData());
        Assert.assertEquals("navigationAccessLabel", navigation.getAccessibilityLabel());
        Assert.assertEquals("navigation-id", navigation.getId());

        List<NavigationItem> items = navigation.getItems();

        Assert.assertNotNull(items);
        Assert.assertEquals(1, items.size());

        NavigationItem item = items.get(0);

        Assert.assertNotNull(item);
        Assert.assertEquals("pageB", item.getName());
        Assert.assertEquals("pageB", item.getTitle());
        Assert.assertEquals("/content/pageA/pageB.html", item.getURL());
        Assert.assertEquals(0, item.getLevel());
        Assert.assertFalse(item.isActive());

        List<NavigationItem> children = item.getChildren();

        Assert.assertNotNull(children);
        Assert.assertEquals(2, children.size());
    }
}
