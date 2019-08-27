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

package com.adobe.cq.commerce.core.components.utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Assert;
import org.junit.Test;

import com.day.cq.wcm.api.Page;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SiteNavigationTest {

    @Test
    public void testGetNavigationRootPage() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(SiteNavigation.PN_NAV_ROOT, true);
        Page navRootPage = mockPage(properties);
        Page page0 = mockPage(null);
        Page page1 = mockPage(null);
        when(page0.getParent()).thenReturn(page1);
        when(page1.getParent()).thenReturn(navRootPage);
        Page page2 = mockPage(null);
        Page page3 = mockPage(null);
        when(page2.getParent()).thenReturn(page3);

        // returns null for null
        Assert.assertNull(SiteNavigation.getNavigationRootPage(null));

        // returns navigation root for navigation root
        Assert.assertSame(navRootPage, SiteNavigation.getNavigationRootPage(navRootPage));

        // returns navigation root for navigation root child
        Assert.assertSame(navRootPage, SiteNavigation.getNavigationRootPage(page1));

        // returns navigation root for child of navigation root child
        Assert.assertSame(navRootPage, SiteNavigation.getNavigationRootPage(page0));

        // returns null for page with no parent
        Assert.assertNull(SiteNavigation.getNavigationRootPage(page3));

        // returns null for page with no navigation root parent
        Assert.assertNull(SiteNavigation.getNavigationRootPage(page2));
    }

    private static Page mockPage(Map<String, Object> contentProperties) {
        Page page = mock(Page.class);
        Resource contentResource = mock(Resource.class);
        ValueMap properties = new ValueMapDecorator(contentProperties == null ? new HashMap<>() : contentProperties);
        when(contentResource.getValueMap()).thenReturn(properties);
        when(page.getContentResource()).thenReturn(contentResource);
        return page;
    }
}
