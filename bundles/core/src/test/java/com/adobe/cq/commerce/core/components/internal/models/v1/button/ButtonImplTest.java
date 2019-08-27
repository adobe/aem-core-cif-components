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

package com.adobe.cq.commerce.core.components.internal.models.v1.button;

import org.apache.sling.api.scripting.SlingBindings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.sightly.WCMBindings;
import com.adobe.cq.wcm.core.components.models.Button;
import com.day.cq.wcm.api.Page;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ButtonImplTest {

    @Rule
    public AemContext context = new AemContext();

    private Button button;

    @Before
    public void setUp() throws Exception {
        Page currentPage = mock(Page.class);
        when(currentPage.getPath()).thenReturn("/content/sample-page");

        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindings.CURRENT_PAGE, currentPage);

        context.addModelsForClasses(ButtonImpl.class);
        context.load().json("/context/jcr-content-button.json", "/content");

    }

    @Test
    public void testGetLinkForProduct() {

        final String expResult = "/content/sample-page.blast-mini-pump.html";
        context.currentResource("/content/linkTypeProduct");
        button = context.request().adaptTo(Button.class);

        String result = button.getLink();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetLinkForCategory() {

        final String expResult = "/content/sample-page.11.html";
        context.currentResource("/content/linkTypeCategory");
        button = context.request().adaptTo(Button.class);

        String result = button.getLink();
        assertEquals(expResult, result);

    }

    @Test
    public void testGetLinkForExternalLink() {

        final String expResult = "http://sample-link.com";
        context.currentResource("/content/linkTypeExternalLink");
        button = context.request().adaptTo(Button.class);

        String result = button.getLink();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetLinkForLinkTo() {

        final String expResult = "/content/venia/language-masters/en.html";
        context.currentResource("/content/linkTypeLinkToPage");
        button = context.request().adaptTo(Button.class);
        String result = button.getLink();
        assertEquals(expResult, result);

    }

    @Test
    public void testDefaultLink() {

        final String expResult = "#";
        context.currentResource("/content/defaultUrl");
        button = context.request().adaptTo(Button.class);

        String result = button.getLink();
        assertEquals(expResult, result);

    }

}