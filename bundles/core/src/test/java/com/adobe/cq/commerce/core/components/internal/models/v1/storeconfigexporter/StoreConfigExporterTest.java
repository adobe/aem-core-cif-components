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

package com.adobe.cq.commerce.core.components.internal.models.v1.storeconfigexporter;

import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

public class StoreConfigExporterTest {

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
    private static final String PAGE_D = "/content/pageD";

    @Test
    public void testStoreView() {
        StoreConfigExporterImpl storeConfigExporter = new StoreConfigExporterImpl();
        Whitebox.setInternalState(storeConfigExporter, "currentPage", context.currentPage(PAGE_A));
        storeConfigExporter.initModel();

        Assert.assertEquals("my-store", storeConfigExporter.getStoreView());
    }

    @Test
    public void testStoreViewInherited() {
        StoreConfigExporterImpl storeConfigExporter = new StoreConfigExporterImpl();
        Whitebox.setInternalState(storeConfigExporter, "currentPage", context.currentPage(PAGE_C));
        storeConfigExporter.initModel();

        Assert.assertEquals("my-store", storeConfigExporter.getStoreView());
    }

    @Test
    public void testStoreViewDefault() {
        StoreConfigExporterImpl storeConfigExporter = new StoreConfigExporterImpl();
        Whitebox.setInternalState(storeConfigExporter, "currentPage", context.currentPage(PAGE_D));
        storeConfigExporter.initModel();

        Assert.assertEquals("default", storeConfigExporter.getStoreView());
    }

}
