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
package com.adobe.cq.commerce.mcp.internal.tools;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.adobe.cq.commerce.magento.graphql.Attribute;
import com.adobe.cq.commerce.magento.graphql.AttributeInput;
import com.adobe.cq.commerce.magento.graphql.AttributeOption;
import com.adobe.cq.commerce.magento.graphql.CustomAttributeMetadata;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class GetCustomAttributeMetadataToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void mapsAttributesWithOptions() {
        AttributeOption red = new AttributeOption().setLabel("Red").setValue("1");
        AttributeOption blue = new AttributeOption().setLabel("Blue").setValue("2");
        Attribute colorAttribute = new Attribute().setAttributeCode("color")
            .setAttributeType("varchar")
            .setInputType("select")
            .setEntityType("catalog_product")
            .setAttributeOptions(Arrays.asList(red, blue));
        CustomAttributeMetadata metadata = new CustomAttributeMetadata().setItems(Collections.singletonList(colorAttribute));

        GetCustomAttributeMetadataTool tool = new GetCustomAttributeMetadataTool() {
            @Override
            protected CustomAttributeMetadata fetch(StoreContext ctx, List<AttributeInput> inputs) {
                assertEquals(1, inputs.size());
                assertEquals("color", inputs.get(0).getAttributeCode());
                assertEquals("catalog_product", inputs.get(0).getEntityType());
                return metadata;
            }
        };

        StoreContext ctx = mock(StoreContext.class);
        ObjectNode args = argsWithAttribute("color", null);

        JsonNode out = tool.call(ctx, args);

        assertEquals("get_custom_attribute_metadata", tool.name());
        JsonNode items = out.get("items");
        assertEquals(1, items.size());
        JsonNode item = items.get(0);
        assertEquals("color", item.get("code").asText());
        assertEquals("varchar", item.get("attributeType").asText());
        assertEquals("select", item.get("inputType").asText());
        assertEquals("catalog_product", item.get("entityType").asText());
        JsonNode options = item.get("options");
        assertEquals(2, options.size());
        assertEquals("Red", options.get(0).get("label").asText());
        assertEquals("1", options.get(0).get("value").asText());
        assertEquals("Blue", options.get(1).get("label").asText());
        assertEquals("2", options.get(1).get("value").asText());
    }

    @Test
    public void defaultsEntityTypeToCatalogProductWhenOmitted() {
        GetCustomAttributeMetadataTool tool = new GetCustomAttributeMetadataTool() {
            @Override
            protected CustomAttributeMetadata fetch(StoreContext ctx, List<AttributeInput> inputs) {
                assertEquals("catalog_product", inputs.get(0).getEntityType());
                return new CustomAttributeMetadata().setItems(Collections.emptyList());
            }
        };

        StoreContext ctx = mock(StoreContext.class);
        // no entityType supplied at all
        ObjectNode args = mapper.createObjectNode();
        args.putArray("attributes").addObject().put("code", "color");

        tool.call(ctx, args);
    }

    @Test
    public void honorsExplicitEntityType() {
        GetCustomAttributeMetadataTool tool = new GetCustomAttributeMetadataTool() {
            @Override
            protected CustomAttributeMetadata fetch(StoreContext ctx, List<AttributeInput> inputs) {
                assertEquals("customer", inputs.get(0).getEntityType());
                return new CustomAttributeMetadata().setItems(Collections.emptyList());
            }
        };

        StoreContext ctx = mock(StoreContext.class);
        ObjectNode args = argsWithAttribute("gender", "customer");

        tool.call(ctx, args);
    }

    @Test
    public void handlesAttributeWithNoOptionsGracefully() {
        Attribute noOptionsAttribute = new Attribute().setAttributeCode("sku")
            .setAttributeType("varchar")
            .setInputType("text")
            .setEntityType("catalog_product")
            .setAttributeOptions(null);
        CustomAttributeMetadata metadata = new CustomAttributeMetadata().setItems(Collections.singletonList(noOptionsAttribute));

        GetCustomAttributeMetadataTool tool = new GetCustomAttributeMetadataTool() {
            @Override
            protected CustomAttributeMetadata fetch(StoreContext ctx, List<AttributeInput> inputs) {
                return metadata;
            }
        };

        StoreContext ctx = mock(StoreContext.class);
        ObjectNode args = argsWithAttribute("sku", null);

        JsonNode out = tool.call(ctx, args);

        JsonNode options = out.get("items").get(0).get("options");
        assertTrue(options.isArray());
        assertEquals(0, options.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenAttributesMissing() {
        GetCustomAttributeMetadataTool tool = new GetCustomAttributeMetadataTool();
        StoreContext ctx = mock(StoreContext.class);
        tool.call(ctx, mapper.createObjectNode());
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenAttributesEmpty() {
        GetCustomAttributeMetadataTool tool = new GetCustomAttributeMetadataTool();
        StoreContext ctx = mock(StoreContext.class);
        ObjectNode args = mapper.createObjectNode();
        args.putArray("attributes");
        tool.call(ctx, args);
    }

    private ObjectNode argsWithAttribute(String code, String entityType) {
        ObjectNode args = mapper.createObjectNode();
        ObjectNode attribute = args.putArray("attributes").addObject();
        attribute.put("code", code);
        if (entityType != null) {
            attribute.put("entityType", entityType);
        }
        return args;
    }
}
