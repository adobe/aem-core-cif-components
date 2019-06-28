/*
 * Copyright 2019 Adobe.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adobe.cq.commerce.core.components.internal.models.v1.button;

import org.apache.sling.api.scripting.SlingBindings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.cq.commerce.core.components.models.button.Button;
import com.adobe.cq.sightly.WCMBindings;
import com.day.cq.wcm.api.Page;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
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
    public void testGetUrlForProduct() {

        final String expResult = "/content/sample-page.blast-mini-pump.html";
        context.currentResource("/content/linkTypeProduct");
        button = context.request().adaptTo(Button.class);

        String result = button.getUrl();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetUrlForCategory() {

        final String expResult = "/content/sample-page.11.html";
        context.currentResource("/content/linkTypeCategory");
        button = context.request().adaptTo(Button.class);

        String result = button.getUrl();
        assertEquals(expResult, result);

    }

    @Test
    public void testGetUrlForExternalLink() {

        final String expResult = "http://sample-link.com";
        context.currentResource("/content/linkTypeExternalLink");
        button = context.request().adaptTo(Button.class);

        String result = button.getUrl();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetUrlForLinkTo() {

        final String expResult = "/content/venia/language-masters/en.html";
        context.currentResource("/content/linkTypeLinkToPage");
        button = context.request().adaptTo(Button.class);
        String result = button.getUrl();
        assertEquals(expResult, result);

    }

    /**
     * Test of getLabel method, of class ButtonImpl.
     */
    @Test
    public void testDefaultLabel() {

        final String expResult = "Label";
        context.currentResource("/content/defaultLabel");
        button = context.request().adaptTo(Button.class);

        String result = button.getLabel();
        assertEquals(expResult, result);

    }

    @Test
    public void testDefaultUrl() {

        final String expResult = "#";
        context.currentResource("/content/defaultUrl");
        button = context.request().adaptTo(Button.class);

        String result = button.getUrl();
        assertEquals(expResult, result);

    }

    @Test
    public void testGetLabel() {

        final String expResult = "Demo Label";
        context.currentResource("/content/demoLabel");
        button = context.request().adaptTo(Button.class);

        String result = button.getLabel();
        assertEquals(expResult, result);

    }

}