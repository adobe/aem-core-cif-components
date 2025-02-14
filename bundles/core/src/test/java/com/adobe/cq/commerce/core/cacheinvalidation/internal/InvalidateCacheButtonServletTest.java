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
package com.adobe.cq.commerce.core.cacheinvalidation.internal;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adobe.granite.ui.components.rendercondition.RenderCondition;

import static org.mockito.Mockito.*;

public class InvalidateCacheButtonServletTest {

    @Mock
    private InvalidateCacheSupport invalidateCacheSupport;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private SlingHttpServletResponse response;

    @InjectMocks
    private InvalidateCacheButtonServlet servlet;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDoGetWithInvalidateCacheSupport() {
        invalidateCacheSupport = mock(InvalidateCacheSupport.class);
        servlet.doGet(request, response);
        verify(request).setAttribute(eq(RenderCondition.class.getName()), any());
    }

    @Test
    public void testDoGetWithoutInvalidateCacheSupport() {
        invalidateCacheSupport = null;
        servlet.doGet(request, response);
        verify(request).setAttribute(eq(RenderCondition.class.getName()), any());
    }
}
