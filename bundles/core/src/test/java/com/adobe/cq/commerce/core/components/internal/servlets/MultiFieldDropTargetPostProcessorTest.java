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

/*******************************************************************************
 * 
 *    Copyright (c) 2018 iDA MediaFoundry
 * 
 *    Permission is hereby granted, free of charge, to any person obtaining a
 *    copy of this software and associated documentation files (the "Software"),
 *    to deal in the Software without restriction, including without limitation
 *    the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *    and/or sell copies of the Software, and to permit persons to whom the Software
 *    is furnished to do so, subject to the following conditions:
 * 
 *    The above copyright notice and this permission notice shall be included in all
 *    copies or substantial portions of the Software.
 * 
 *    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *    WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *    CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *******************************************************************************/

package com.adobe.cq.commerce.core.components.internal.servlets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.servlets.post.Modification;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.wcm.testing.mock.aem.junit.AemContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

public class MultiFieldDropTargetPostProcessorTest {

    @Rule
    public final AemContext context = new AemContext();

    @Before
    public void setUp() throws Exception {
        context.load().json("/context/drop-target-post-processor.json", "/content/postprocessor");
    }

    @Test
    public void testAppendToExistingMultiValue() throws Exception {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(context.resourceResolver());
        request.setResource(context.resourceResolver().resolve("/content/postprocessor/jcr:content/existingMultiValue"));
        request.setParameterMap(Collections.singletonMap("./multiDropTarget->@items", "b"));

        MultiFieldDropTargetPostProcessor dropTargetPostProcessor = new MultiFieldDropTargetPostProcessor();
        List<Modification> modifications = new ArrayList<>();
        dropTargetPostProcessor.process(request, modifications);
        Resource resource = request.getResource();

        assertThat(modifications)
            .isNotEmpty()
            .hasSize(1);

        assertThat(resource).isNotNull();

        assertThat(resource.getValueMap())
            .isNotEmpty()
            .containsKeys("./items")
            .doesNotContainKeys("./multiDropTarget->@items");

        assertThat(resource.getValueMap().get("./items", String[].class))
            .isNotEmpty()
            .containsExactly("a", "b");
    }

    @Test
    public void testCreateSubnodeAndAppendValue() throws Exception {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(context.resourceResolver());
        request.setResource(context.resourceResolver().resolve("/content/postprocessor/jcr:content/noExistingMultiValue"));
        request.setParameterMap(Collections.singletonMap("./multiDropTarget->/subNode/@items", "b"));

        MultiFieldDropTargetPostProcessor dropTargetPostProcessor = new MultiFieldDropTargetPostProcessor();
        List<Modification> modifications = new ArrayList<>();
        dropTargetPostProcessor.process(request, modifications);
        Resource resource = request.getResource();

        assertThat(modifications)
            .isNotEmpty()
            .hasSize(1);

        assertThat(resource).isNotNull();
        assertThat(resource.getChild("multiDropTarget->")).isNull();
        assertThat(resource.getChild("subNode")).isNotNull();

        assertThat(resource.getValueMap())
            .isNotEmpty()
            .doesNotContainKeys("./multiDropTarget->/subNode/@items");

        assertThat(resource.getChild("subNode").getValueMap().get("./items", String[].class))
            .isNotEmpty()
            .containsExactly("b");
    }

    @Test
    public void testCreateNewMultiValue() throws Exception {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(context.resourceResolver());
        request.setResource(context.resourceResolver().resolve("/content/postprocessor/jcr:content/noExistingMultiValue"));
        request.setParameterMap(Collections.singletonMap("./multiDropTarget->@items", "b"));

        MultiFieldDropTargetPostProcessor dropTargetPostProcessor = new MultiFieldDropTargetPostProcessor();
        List<Modification> modifications = new ArrayList<>();
        dropTargetPostProcessor.process(request, modifications);
        Resource resource = request.getResource();

        assertThat(modifications).isNotEmpty().hasSize(1);
        assertThat(resource).isNotNull();

        assertThat(resource.getValueMap())
            .isNotEmpty()
            .containsKeys("./items")
            .doesNotContainKeys("./multiDropTarget->@items");

        assertThat(resource.getValueMap().get("./items", String[].class))
            .isNotEmpty()
            .containsExactly("b");
    }

    @Test
    public void testConvertExistingSingleValueToMultifield() throws Exception {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(context.resourceResolver());
        request.setResource(context.resourceResolver().resolve("/content/postprocessor/jcr:content/existingSingleValue"));
        request.setParameterMap(Collections.singletonMap("./multiDropTarget->@items", "b"));

        MultiFieldDropTargetPostProcessor dropTargetPostProcessor = new MultiFieldDropTargetPostProcessor();
        List<Modification> modifications = new ArrayList<>();
        dropTargetPostProcessor.process(request, modifications);
        Resource resource = request.getResource();

        assertThat(modifications)
            .isNotEmpty()
            .hasSize(1);

        assertThat(resource).isNotNull();

        assertThat(resource.getValueMap())
            .isNotEmpty()
            .containsKeys("./items")
            .doesNotContainKeys("./multiDropTarget->@items");

        assertThat(resource.getValueMap().get("./items", String[].class))
            .isNotEmpty()
            .containsExactly("a", "b");
    }

    @Test
    public void testCreateSubnodeWithUniqueNameAndAppendValue() throws Exception {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(context.resourceResolver());
        request.setResource(context.resourceResolver().resolve("/content/postprocessor/jcr:content/existingComposite"));
        request.setParameterMap(Collections.singletonMap("./multiDropTarget->/subNode/{{COMPOSITE}}/@link", "b"));

        MultiFieldDropTargetPostProcessor dropTargetPostProcessor = new MultiFieldDropTargetPostProcessor();
        List<Modification> modifications = new ArrayList<>();
        dropTargetPostProcessor.process(request, modifications);
        Resource resource = request.getResource();

        assertThat(modifications)
            .isNotEmpty()
            .hasSize(1);

        assertThat(resource).isNotNull();

        assertThat(resource.getValueMap())
            .isNotEmpty()
            .doesNotContainKeys("./items")
            .doesNotContainKeys("./multiDropTarget->/subNode/{{COMPOSITE}}/@link");

        assertThat(resource.getChild("multiDropTarget->")).isNull();
        assertThat(resource.getChild("subNode")).isNotNull();
        assertThat(resource.getChild("subNode").getChildren()).hasSize(3);
        assertThat(resource.getChild("subNode/item0")).isNotNull();
        assertThat(resource.getChild("subNode/item1")).isNotNull();
        assertThat(resource.getChild("subNode/item2")).isNotNull();

        assertThat(resource.getChild("subNode/item0").getValueMap())
            .contains(
                entry("jcr:primaryType", "nt:unstructured"),
                entry("link", "a"));

        assertThat(resource.getChild("subNode/item1").getValueMap())
            .contains(
                entry("jcr:primaryType", "nt:unstructured"),
                entry("link", "c"));

        assertThat(resource.getChild("subNode/item2").getValueMap())
            .contains(
                entry("link", "b"));
    }

}
