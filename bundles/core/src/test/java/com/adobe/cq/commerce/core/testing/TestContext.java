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
package com.adobe.cq.commerce.core.testing;

import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.xss.XSSAPI;
import org.mockito.Mockito;

import com.adobe.cq.commerce.core.components.internal.services.SiteNavigationImpl;
import com.adobe.cq.commerce.core.components.internal.services.SpecificPageStrategy;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.wcm.core.components.internal.link.DefaultPathProcessor;
import com.day.cq.commons.Externalizer;
import com.day.cq.wcm.api.PageManagerFactory;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextBuilder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestContext {

    public static AemContext newAemContext() {
        return buildAemContext().build();
    }

    public static AemContext newAemContext(String loadContent) {
        return buildAemContext(loadContent).build();
    }

    /**
     * @deprecated Tests should not use the builder pattern of AemContext but use setup methods to initialize the test context further
     */
    @Deprecated
    public static AemContextBuilder buildAemContext(String loadContent) {
        return buildAemContext()
            .<AemContext>afterSetUp(context -> context.load(true).json(loadContent, "/content"));
    }

    /**
     * @deprecated Tests should not use the builder pattern of AemContext but use setup methods to initialize the test context further
     */
    @Deprecated
    public static AemContextBuilder buildAemContext() {
        return buildAemContextInternal();
    }

    private static AemContextBuilder buildAemContextInternal() {
        return new AemContextBuilder()
            .resourceResolverType(ResourceResolverType.JCR_MOCK)
            .<AemContext>afterSetUp(context -> {
                // register commonly required ootb services
                context.registerService(PageManagerFactory.class, rr -> context.pageManager());
                context.registerService(Externalizer.class, new MockExternalizer());
                context.registerInjectActivateService(new DefaultPathProcessor());

                XSSAPI xssApi = mock(XSSAPI.class);
                when(xssApi.filterHTML(Mockito.anyString())).then(i -> i.getArgumentAt(0, String.class));
                context.registerService(XSSAPI.class, xssApi);

                // register commonly used cif services
                context.registerInjectActivateService(new SpecificPageStrategy());
                context.registerInjectActivateService(new SiteNavigationImpl());
                context.registerInjectActivateService(new UrlProviderImpl());
            });
    }
}
