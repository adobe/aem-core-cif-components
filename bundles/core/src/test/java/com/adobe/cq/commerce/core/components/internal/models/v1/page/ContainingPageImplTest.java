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
package com.adobe.cq.commerce.core.components.internal.models.v1.page;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.junit.Before;
import org.junit.Rule;

import com.adobe.cq.commerce.core.components.internal.models.v1.product.ProductImpl;
import com.adobe.cq.wcm.core.components.models.Page;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

public class ContainingPageImplTest extends AbstractPageDelegatorTest {

    @Rule
    public final AemContext context = new AemContext((AemContextCallback) context -> {
        context.load().json("/context/jcr-content.json", "/content");
    });

    protected final String pagePath = "/content/pageH";

    /**
     * Parameterizes the testDelegation() tests inherited from {@link AbstractPageDelegatorTest}.
     *
     * @param mock
     * @return
     */
    @Override
    protected Page testDelegationCreateSubject(Page mock) {
        Resource component = context.create().resource(pagePath + "/jcr:content/root/container/product",
            "sling:resourceType", ProductImpl.RESOURCE_TYPE);
        context.request().setAttribute(MockPage.class.getName(), mock);
        context.currentResource(component);
        return context.request().adaptTo(Page.class);
    }

    @Before
    public void setup() throws PersistenceException {
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, context.currentPage(pagePath));
        slingBindings.setResource(context.currentPage().getContentResource());

        // set a mock resourceType to inject the MockPage
        ModifiableValueMap pageContent = context.resourceResolver().getResource(pagePath + "/jcr:content")
            .adaptTo(ModifiableValueMap.class);
        pageContent.put("sling:resourceType", "mock");

        context.resourceResolver().commit();
        context.currentResource(context.currentPage().getContentResource());
        context.addModelsForClasses(MockPage.class);
    }
}
