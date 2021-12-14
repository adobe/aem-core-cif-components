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
package com.adobe.cq.commerce.core.components.internal.models.v1.account;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adobe.cq.commerce.core.components.models.account.MiniAccount;
import com.adobe.cq.commerce.core.testing.TestContext;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class MiniAccountImplTest {

    @Rule
    public final AemContext context = TestContext.newAemContext();
    @Mock
    private Style style;
    private Resource contentResource;
    private Page currentPage;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        currentPage = context.create().page("/content/page");
        context.currentPage(currentPage);

        contentResource = context.create().resource(currentPage.getPath() + "/root/mini-account",
            "sling:resourceType", MiniAccountImpl.RT_MINIACCOUNT_V2);
        context.currentResource(contentResource);

        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_STYLE, style);

        when(style.get(any(), anyBoolean())).then(i -> i.getArgumentAt(1, Boolean.class));
    }

    @Test
    public void testWishListDefault() {
        MiniAccount miniAccount = context.request().adaptTo(MiniAccount.class);

        assertNotNull(miniAccount);
        assertFalse(miniAccount.getWishListEnabled());
    }

    @Test
    public void testWishListDisabled() {
        when(style.get(eq(MiniAccountImpl.PN_STYLE_ENABLE_WISH_LIST), anyBoolean())).thenReturn(Boolean.FALSE);
        MiniAccount miniAccount = context.request().adaptTo(MiniAccount.class);

        assertNotNull(miniAccount);
        assertFalse(miniAccount.getWishListEnabled());
    }

    @Test
    public void testWishListEnabled() {
        when(style.get(eq(MiniAccountImpl.PN_STYLE_ENABLE_WISH_LIST), anyBoolean())).thenReturn(Boolean.TRUE);
        MiniAccount miniAccount = context.request().adaptTo(MiniAccount.class);

        assertNotNull(miniAccount);
        assertTrue(miniAccount.getWishListEnabled());
    }
}
