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
package com.adobe.cq.commerce.mcp.internal.dto;

import java.util.List;

import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Maps CIF domain objects to compact Jackson DTOs suitable for MCP tool responses.
 */
public final class DtoMapper {

    private DtoMapper() {}

    /**
     * Maps a {@link ProductListItem} to a compact DTO with the fields {@code sku}, {@code name}, {@code slug},
     * {@code imageUrl}, {@code imageAlt}, {@code price} and {@code currency}. The price fields are omitted if the item has no
     * price range.
     *
     * @param mapper the Jackson object mapper used to create the resulting node
     * @param item the product list item to map
     * @return the mapped DTO
     */
    public static ObjectNode product(ObjectMapper mapper, ProductListItem item) {
        ObjectNode node = mapper.createObjectNode();
        node.put("sku", item.getSKU());
        node.put("name", item.getTitle());
        node.put("slug", item.getSlug());
        node.put("imageUrl", item.getImageURL());
        node.put("imageAlt", item.getImageAlt());
        Price price = item.getPriceRange();
        if (price != null) {
            node.put("price", price.getFinalPrice());
            node.put("currency", price.getCurrency());
        }
        return node;
    }

    /**
     * Maps a {@link CategoryInterface} to a compact DTO with the fields {@code uid}, {@code name} and {@code urlPath}. When
     * {@code withChildren} is {@code true} and the category is a {@link CategoryTree} with children, a {@code children} array
     * of the same DTO shape (without nested children) is added.
     *
     * @param mapper the Jackson object mapper used to create the resulting node
     * @param category the category to map
     * @param withChildren whether to include the immediate children of the category
     * @return the mapped DTO
     */
    public static ObjectNode category(ObjectMapper mapper, CategoryInterface category, boolean withChildren) {
        ObjectNode node = mapper.createObjectNode();
        node.put("uid", category.getUid() != null ? category.getUid().toString() : null);
        node.put("name", category.getName());
        node.put("urlPath", category.getUrlPath());
        if (withChildren && category instanceof CategoryTree) {
            List<CategoryTree> children = ((CategoryTree) category).getChildren();
            if (children != null) {
                ArrayNode arr = node.putArray("children");
                for (CategoryTree child : children) {
                    arr.add(category(mapper, child, false));
                }
            }
        }
        return node;
    }
}
