/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
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
package com.adobe.cq.commerce.core.components.internal.servlets;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletResponse;

import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CatalogPageErrorResponseFilterTest {

    @Rule
    public final AemContext context = new AemContext();

    private final CatalogPageErrorResponseFilter subject = new CatalogPageErrorResponseFilter();
    private MockSlingHttpServletRequest request;
    private MockSlingHttpServletResponse response;
    private final FilterChain chain = mock(FilterChain.class);

    @Before
    public void setup() {
        request = context.request();
        response = context.response();
    }

    @Test
    public void testMarkedResponse() throws Exception {
        request.setAttribute(CatalogPageErrorResponseFilter.RETRY_AFTER_ATTRIBUTE, 60);
        subject.doFilter(request, response, chain);
        assertEquals("60", response.getHeader("Retry-After"));
        verify(chain).doFilter(request, response);
    }

    @Test
    public void testUnmarkedResponse() throws Exception {
        subject.doFilter(request, response, chain);
        assertNull(response.getHeader("Retry-After"));
        verify(chain).doFilter(request, response);
    }

    @Test
    public void testErrorStatusInitializedByHandler() throws Exception {
        request.setAttribute(CatalogPageErrorResponseFilter.RETRY_AFTER_ATTRIBUTE, 60);
        subject.doFilter(request, response, (errorRequest, errorResponse) -> {
            errorRequest.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 503);
            response.setStatus(503);
            assertEquals("60", response.getHeader("Retry-After"));
        });
    }

    @Test
    public void testMarkerConsumedBeforeRendering() throws Exception {
        request.setAttribute(CatalogPageErrorResponseFilter.RETRY_AFTER_ATTRIBUTE, 60);
        subject.doFilter(request, response, (errorRequest, errorResponse) -> {
            assertNull(errorRequest.getAttribute(CatalogPageErrorResponseFilter.RETRY_AFTER_ATTRIBUTE));
            assertEquals("60", response.getHeader("Retry-After"));
        });
        response.reset();
        subject.doFilter(request, response, chain);
        assertNull(response.getHeader("Retry-After"));
    }

    @Test
    public void testNonpositiveRetryAfter() throws Exception {
        for (int seconds : new int[] { 0, -1 }) {
            request.setAttribute(CatalogPageErrorResponseFilter.RETRY_AFTER_ATTRIBUTE, seconds);
            subject.doFilter(request, response, chain);
            assertNull(response.getHeader("Retry-After"));
        }
    }

    @Test
    public void testInvalidRetryAfterAttribute() throws Exception {
        request.setAttribute(CatalogPageErrorResponseFilter.RETRY_AFTER_ATTRIBUTE, "60");
        subject.doFilter(request, response, chain);
        assertNull(response.getHeader("Retry-After"));
    }

    @Test
    public void testNonHttpResponse() throws Exception {
        ServletResponse nonHttpResponse = mock(ServletResponse.class);
        request.setAttribute(CatalogPageErrorResponseFilter.RETRY_AFTER_ATTRIBUTE, 60);
        subject.doFilter(request, nonHttpResponse, chain);
        verify(chain).doFilter(request, nonHttpResponse);
    }
}
