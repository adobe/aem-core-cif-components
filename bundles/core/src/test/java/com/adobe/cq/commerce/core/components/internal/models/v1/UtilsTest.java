/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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
package com.adobe.cq.commerce.core.components.internal.models.v1;

import java.util.Collections;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import com.day.cq.wcm.api.designer.Designer;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.api.policies.ContentPolicy;
import com.day.cq.wcm.api.policies.ContentPolicyManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UtilsTest {

    @Test
    public void testStylePropertiesReturnsEmpty() {
        BundleContext bundleContext = mock(BundleContext.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Resource resource = mock(Resource.class);
        when(resource.getResourceResolver()).thenReturn(resourceResolver);

        ValueMap styleProps = Utils.getStyleProperties(new MockSlingHttpServletRequest(resourceResolver, bundleContext), resource);

        assertNotNull(styleProps);
        assertEquals(0, styleProps.size());
    }

    @Test
    public void testStylePropertiesReturnedEmptyFallbackNoPolicy() {
        BundleContext bundleContext = mock(BundleContext.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Resource resource = mock(Resource.class);
        SlingHttpServletRequest request = new MockSlingHttpServletRequest(resourceResolver, bundleContext);
        ContentPolicyManager policyManager = mock(ContentPolicyManager.class);

        when(resource.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.adaptTo(ContentPolicyManager.class)).thenReturn(policyManager);

        ValueMap styleProps = Utils.getStyleProperties(request, resource);

        assertNotNull(styleProps);
        assertEquals(0, styleProps.size());
    }

    @Test
    public void testStylePropertiesReturnedEmptyFallbackNoStyle() {
        BundleContext bundleContext = mock(BundleContext.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Resource resource = mock(Resource.class);
        SlingHttpServletRequest request = new MockSlingHttpServletRequest(resourceResolver, bundleContext);
        Designer designer = mock(Designer.class);

        when(resource.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.adaptTo(Designer.class)).thenReturn(designer);

        ValueMap styleProps = Utils.getStyleProperties(request, resource);

        assertNotNull(styleProps);
        assertEquals(0, styleProps.size());
    }

    @Test
    public void testStylePropertiesReturnedFromPolicy() {
        BundleContext bundleContext = mock(BundleContext.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Resource resource = mock(Resource.class);
        SlingHttpServletRequest request = new MockSlingHttpServletRequest(resourceResolver, bundleContext);
        ContentPolicyManager policyManager = mock(ContentPolicyManager.class);
        ContentPolicy contentPolicy = mock(ContentPolicy.class);
        ValueMap props = new ValueMapDecorator(Collections.singletonMap("foo", "bar"));

        when(resource.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.adaptTo(ContentPolicyManager.class)).thenReturn(policyManager);
        when(policyManager.getPolicy(any(), eq(request))).thenReturn(contentPolicy);
        when(contentPolicy.getProperties()).thenReturn(props);

        ValueMap styleProps = Utils.getStyleProperties(request, resource);

        assertNotNull(styleProps);
        assertEquals("bar", styleProps.get("foo", String.class));
    }

    @Test
    public void testStylePropertiesReturnedFromStyle() {
        BundleContext bundleContext = mock(BundleContext.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Resource resource = mock(Resource.class);
        SlingHttpServletRequest request = new MockSlingHttpServletRequest(resourceResolver, bundleContext);
        Designer designer = mock(Designer.class);
        Style style = mock(Style.class);

        when(resource.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.adaptTo(Designer.class)).thenReturn(designer);
        when(designer.getStyle(resource)).thenReturn(style);
        when(style.get("foo", String.class)).thenReturn("bar");

        ValueMap styleProps = Utils.getStyleProperties(request, resource);

        assertNotNull(styleProps);
        assertEquals("bar", styleProps.get("foo", String.class));
    }
}
