/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.granite.ui.components.rendercondition.RenderCondition;
import com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition;
import com.day.cq.wcm.api.policies.ContentPolicy;
import com.day.cq.wcm.api.policies.ContentPolicyManager;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextBuilder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class ProductListXfStylesRenderConditionServletTest {

    private ProductListXfStylesRenderConditionServlet renderConditionServlet;

    @Rule
    public final AemContext context = new AemContextBuilder(ResourceResolverType.JCR_MOCK).build();

    ContentPolicyManager policyManager;
    ContentPolicy policy;
    SlingHttpServletRequest request;

    @Before
    public void setUp() {
        renderConditionServlet = new ProductListXfStylesRenderConditionServlet();
        context.load().json("/context/jcr-conf.json", "/conf/testing");
        context.load().json("/context/jcr-content.json", "/content");

        request = spy(context.request());
        RequestPathInfo requestPathInfo = mock(RequestPathInfo.class);
        when(request.getRequestPathInfo()).thenReturn(requestPathInfo);
        when(requestPathInfo.getSuffixResource())
            .thenReturn(context.resourceResolver().getResource("/content/pageA/jcr:content/root/responsivegrid/productlist_with_xf"));

        policyManager = mock(ContentPolicyManager.class);
        policy = mock(ContentPolicy.class);

        context.registerAdapter(ResourceResolver.class, ContentPolicyManager.class, policyManager);
    }

    @Test
    public void testDataSource() throws ServletException, IOException {
        when(policy.getPath()).thenReturn("/conf/testing/settings/wcm/policies/testing");
        when(policyManager.getPolicy((Resource) any())).thenReturn(policy);
        // Call datasource servlet
        renderConditionServlet.doGet(request, context.response());
        SimpleRenderCondition condition = (SimpleRenderCondition) context.request().getAttribute(RenderCondition.class.getName());
        assertNotNull(condition);

        assertTrue(condition.check());
    }

    @Test
    public void testDataSourceWithNoPolicy() throws ServletException, IOException {
        when(policyManager.getPolicy((Resource) any())).thenReturn(null);
        // Call datasource servlet
        renderConditionServlet.doGet(request, context.response());
        SimpleRenderCondition condition = (SimpleRenderCondition) context.request().getAttribute(RenderCondition.class.getName());
        assertNotNull(condition);

        assertFalse(condition.check());
    }

    @Test
    public void testDataSourceWithNoValues() throws ServletException, IOException {
        when(policy.getPath()).thenReturn("/conf/testing/settings/wcm/policies/empty");
        when(policyManager.getPolicy((Resource) any())).thenReturn(policy);

        // Call datasource servlet
        renderConditionServlet.doGet(request, context.response());
        SimpleRenderCondition condition = (SimpleRenderCondition) context.request().getAttribute(RenderCondition.class.getName());
        assertNotNull(condition);

        assertFalse(condition.check());
    }
}
