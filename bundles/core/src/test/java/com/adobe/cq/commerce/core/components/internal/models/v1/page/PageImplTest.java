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

package com.adobe.cq.commerce.core.components.internal.models.v1.page;

import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

public class PageImplTest {

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                context.load().json(contentPath, "/content");
            },
            ResourceResolverType.JCR_MOCK);
    }

    private static final String PAGE_A = "/content/pageA";
    private static final String PAGE_C = "/content/pageB/pageC";

    @Test
    public void testStoreCode() {
        PageImpl pageModel = new PageImpl();
        Whitebox.setInternalState(pageModel, "currentPage", context.currentPage(PAGE_A));
        pageModel.initModel();

        Assert.assertEquals("my-store", pageModel.getStoreCode());
    }

    @Test
    public void testStoreCodeInherited() {
        PageImpl pageModel = new PageImpl();
        Whitebox.setInternalState(pageModel, "currentPage", context.currentPage(PAGE_C));
        pageModel.initModel();

        Assert.assertEquals("my-store", pageModel.getStoreCode());
    }

}
