/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.components.internal.postprocessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Rule;
import org.junit.Test;

import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.assertj.core.api.Assertions.assertThat;

public class SelectorFilterPostProcessorTest {

    @Rule
    public final AemContext context = createContext("/context/selector-filter-post-processor.json");

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                // Load page structure
                context.load().json(contentPath, "/content");
            },
            ResourceResolverType.JCR_MOCK);
    }

    @Test
    public void testMissingRequestParameter() throws Exception {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(context.resourceResolver());
        request.setResource(context.resourceResolver().resolve("/content/empty/jcr:content"));

        SelectorFilterPostProcessor postProcessor = new SelectorFilterPostProcessor();
        List<Modification> modifications = new ArrayList<>();
        postProcessor.process(request, modifications);

        assertThat(modifications).isEmpty();
    }

    @Test
    public void testMissingSelectorFilterSeparator() throws Exception {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(context.resourceResolver());
        request.setResource(context.resourceResolver().resolve("/content/empty/jcr:content"));
        request.setParameterMap(Collections.singletonMap("./selectorFilter", "1"));

        SelectorFilterPostProcessor postProcessor = new SelectorFilterPostProcessor();
        List<Modification> modifications = new ArrayList<>();
        postProcessor.process(request, modifications);

        assertThat(modifications).isEmpty();
    }

    @Test
    public void testPostToEmptyResource() throws Exception {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(context.resourceResolver());
        request.setResource(context.resourceResolver().resolve("/content/empty/jcr:content"));
        request.setParameterMap(Collections.singletonMap("./selectorFilter", "1|men"));

        SelectorFilterPostProcessor postProcessor = new SelectorFilterPostProcessor();
        List<Modification> modifications = new ArrayList<>();
        postProcessor.process(request, modifications);
        Resource resource = request.getResource();

        assertThat(modifications)
            .isNotEmpty()
            .hasSize(1);

        ValueMap vm = resource.getValueMap();
        assertThat(vm)
            .isNotEmpty()
            .containsKeys("selectorFilter", "urlPath");

        assertThat(vm.get("selectorFilter", String.class)).isEqualTo("1");
        assertThat(vm.get("urlPath", String.class)).isEqualTo("men");
    }

    @Test
    public void testChangeSingleValueToMultiValues() throws Exception {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(context.resourceResolver());
        request.setResource(context.resourceResolver().resolve("/content/singlevalue/jcr:content"));
        request.setParameterMap(Collections.singletonMap("./selectorFilter", new String[] { "1|men", "2|women", "invalid" }));

        SelectorFilterPostProcessor postProcessor = new SelectorFilterPostProcessor();
        List<Modification> modifications = new ArrayList<>();
        postProcessor.process(request, modifications);
        Resource resource = request.getResource();

        assertThat(modifications)
            .isNotEmpty()
            .hasSize(1);

        ValueMap vm = resource.getValueMap();
        assertThat(vm)
            .isNotEmpty()
            .containsKeys("selectorFilter", "urlPath");

        assertThat(vm.get("selectorFilter", String[].class)).contains("1", "2");
        assertThat(vm.get("urlPath", String[].class)).contains("men", "women");
    }

    @Test
    public void testChangeMultiValuesToSingleValue() throws Exception {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(context.resourceResolver());
        request.setResource(context.resourceResolver().resolve("/content/singlevalue/jcr:content"));
        request.setParameterMap(Collections.singletonMap("./selectorFilter", "0|root"));

        SelectorFilterPostProcessor postProcessor = new SelectorFilterPostProcessor();
        List<Modification> modifications = new ArrayList<>();
        postProcessor.process(request, modifications);
        Resource resource = request.getResource();

        assertThat(modifications)
            .isNotEmpty()
            .hasSize(1);

        ValueMap vm = resource.getValueMap();
        assertThat(vm)
            .isNotEmpty()
            .containsKeys("selectorFilter", "urlPath");

        assertThat(vm.get("selectorFilter", String.class)).isEqualTo("0");
        assertThat(vm.get("urlPath", String.class)).isEqualTo("root");
    }
}
