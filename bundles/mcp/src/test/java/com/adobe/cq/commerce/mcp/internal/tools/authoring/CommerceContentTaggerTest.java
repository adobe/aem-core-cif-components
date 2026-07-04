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
package com.adobe.cq.commerce.mcp.internal.tools.authoring;

import org.apache.sling.api.resource.Resource;
import org.junit.Rule;
import org.junit.Test;

import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CommerceContentTaggerTest {
    @Rule
    public final AemContext context = new AemContext();

    @Test
    public void removesProductTag() {
        context.create().resource("/content/dam/venia/sample.jpg", "jcr:primaryType", "dam:Asset");
        context.create().resource("/content/dam/venia/sample.jpg/jcr:content", "jcr:primaryType", "nt:unstructured");
        context.create().resource("/content/dam/venia/sample.jpg/jcr:content/metadata",
            "jcr:primaryType", "nt:unstructured",
            "cq:products", new String[] { "VD09", "VP11" });

        Resource metadata = context.resourceResolver()
            .getResource("/content/dam/venia/sample.jpg/jcr:content/metadata");
        CommerceContentTagger.apply(metadata, "VP11", null, CommerceContentTagger.Action.REMOVE);

        String[] products = metadata.getValueMap().get("cq:products", String[].class);
        assertEquals(1, products.length);
        assertEquals("VD09", products[0]);
        assertTrue(metadata.getValueMap().get(CommerceContentTagger.PN_CQ_PRODUCTS_TYPE, String.class) == null);
    }

    @Test
    public void upgradesSingleStringTagToArrayWhenMultiple() {
        context.create().resource("/content/dam/venia/sample.jpg", "jcr:primaryType", "dam:Asset");
        context.create().resource("/content/dam/venia/sample.jpg/jcr:content", "jcr:primaryType", "nt:unstructured");
        context.create().resource("/content/dam/venia/sample.jpg/jcr:content/metadata",
            "jcr:primaryType", "nt:unstructured",
            "cq:products", "VD09");

        Resource metadata = context.resourceResolver()
            .getResource("/content/dam/venia/sample.jpg/jcr:content/metadata");
        CommerceContentTagger.apply(metadata, "VA04", null, CommerceContentTagger.Action.ADD);

        String[] products = metadata.getValueMap().get("cq:products", String[].class);
        assertEquals(2, products.length);
        assertEquals("VD09", products[0]);
        assertEquals("VA04", products[1]);
        assertEquals(CommerceContentTagger.PRODUCT_TYPE_COMBINED_SKU,
            metadata.getValueMap().get(CommerceContentTagger.PN_CQ_PRODUCTS_TYPE, String.class));
    }

    @Test
    public void readsLegacyCommaSeparatedProductTags() {
        context.create().resource("/content/dam/venia/sample.jpg", "jcr:primaryType", "dam:Asset");
        context.create().resource("/content/dam/venia/sample.jpg/jcr:content", "jcr:primaryType", "nt:unstructured");
        context.create().resource("/content/dam/venia/sample.jpg/jcr:content/metadata",
            "jcr:primaryType", "nt:unstructured",
            "cq:products", "VD09,VA04");

        Resource metadata = context.resourceResolver()
            .getResource("/content/dam/venia/sample.jpg/jcr:content/metadata");
        CommerceContentTagger.apply(metadata, "VP11", null, CommerceContentTagger.Action.ADD);

        String[] products = metadata.getValueMap().get("cq:products", String[].class);
        assertEquals(3, products.length);
        assertEquals("VD09", products[0]);
        assertEquals("VA04", products[1]);
        assertEquals("VP11", products[2]);
    }

    @Test
    public void resolvesDamAssetMetadataTarget() {
        context.create().resource("/content/dam/venia/sample.jpg", "jcr:primaryType", "dam:Asset");
        context.create().resource("/content/dam/venia/sample.jpg/jcr:content", "jcr:primaryType", "nt:unstructured");
        context.create().resource("/content/dam/venia/sample.jpg/jcr:content/metadata", "jcr:primaryType", "nt:unstructured");

        Resource target = CommerceContentTagger.resolveTagTarget(context.resourceResolver(),
            "/content/dam/venia/sample.jpg");
        assertTrue(target.getPath().endsWith("/jcr:content/metadata"));
    }
}
