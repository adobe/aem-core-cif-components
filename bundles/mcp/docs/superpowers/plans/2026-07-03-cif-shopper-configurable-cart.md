# CIF Shopper Configurable Product Cart Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let `add_to_cart` add configurable products (size/color/etc.) by resolving human-readable option values to Magento option-value IDs server-side, in addition to the simple products it already supports.

**Architecture:** A new standalone `ConfigurableOptionResolver` matches a supplied `{attribute: value}` map against a product's `configurable_options` (fetched via an extended `McpProductRetriever` query) to produce Magento option-value UIDs. `AddToCartTool` switches from `addSimpleProductsToCart` to the unified `addProductsToCart` mutation, which accepts those UIDs via `CartItemInput.selectedOptions` and works for both simple and configurable products through one code path.

**Tech Stack:** Java 8, `magento-graphql` 9.1.0 (`addProductsToCart`, `ConfigurableProduct`/`ConfigurableProductOptions`/`ConfigurableProductOptionsValues`), JUnit 4 + Mockito.

## Global Constraints

- Bundle products and order creation are out of scope for this plan (separate later phases per `project_mcp_cart_checkout_flow` memory).
- No `bundles/core` changes.
- All new/changed classes live flat in `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/` — no new subpackages (established convention).
- Exact attribute-code/label matching behavior and error wording are provisional — confirm and adjust via live testing against the running AEM instance (established workflow for this roadmap), then update this plan/spec with what testing found, same as Phase 1.

---

### Task 1: `ConfigurableOptionResolver`

**Files:**
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/ConfigurableOptionResolver.java`
- Test: `bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/ConfigurableOptionResolverTest.java`

**Interfaces:**
- Produces: `public class ConfigurableOptionResolver { public List<ID> resolve(ProductInterface product, Map<String, String> suppliedOptions) }` — returns an empty list for non-configurable products; throws `IllegalArgumentException` (with a message naming the option and its available values) when a configurable product's required option is missing or doesn't match any available value. Task 2 calls this.

- [ ] **Step 1: Write the failing test**

```java
package com.adobe.cq.commerce.mcp.internal.tools;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptions;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptionsValues;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.shopify.graphql.support.ID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigurableOptionResolverTest {
    private final ConfigurableOptionResolver resolver = new ConfigurableOptionResolver();

    @Test
    public void simpleProductNeedsNoOptions() {
        ProductInterface simple = mock(ProductInterface.class);
        List<ID> result = resolver.resolve(simple, new HashMap<>());
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void resolvesMatchingOptionsToUids() {
        ConfigurableProductOptionsValues blue = mock(ConfigurableProductOptionsValues.class);
        when(blue.getLabel()).thenReturn("Blue");
        when(blue.getUid()).thenReturn(new ID("value-blue"));

        ConfigurableProductOptions color = mock(ConfigurableProductOptions.class);
        when(color.getAttributeCode()).thenReturn("fashion_color");
        when(color.getLabel()).thenReturn("Color");
        when(color.getValues()).thenReturn(Collections.singletonList(blue));

        ConfigurableProduct product = mock(ConfigurableProduct.class);
        when(product.getConfigurableOptions()).thenReturn(Collections.singletonList(color));

        Map<String, String> supplied = new HashMap<>();
        supplied.put("fashion_color", "Blue");

        List<ID> result = resolver.resolve(product, supplied);
        assertEquals(Collections.singletonList(new ID("value-blue")), result);
    }

    @Test
    public void matchesBySuppliedLabelToo() {
        ConfigurableProductOptionsValues blue = mock(ConfigurableProductOptionsValues.class);
        when(blue.getLabel()).thenReturn("Blue");
        when(blue.getUid()).thenReturn(new ID("value-blue"));

        ConfigurableProductOptions color = mock(ConfigurableProductOptions.class);
        when(color.getAttributeCode()).thenReturn("fashion_color");
        when(color.getLabel()).thenReturn("Color");
        when(color.getValues()).thenReturn(Collections.singletonList(blue));

        ConfigurableProduct product = mock(ConfigurableProduct.class);
        when(product.getConfigurableOptions()).thenReturn(Collections.singletonList(color));

        Map<String, String> supplied = new HashMap<>();
        supplied.put("color", "blue");

        List<ID> result = resolver.resolve(product, supplied);
        assertEquals(Collections.singletonList(new ID("value-blue")), result);
    }

    @Test
    public void throwsWithAvailableValuesWhenOptionMissing() {
        ConfigurableProductOptionsValues blue = mock(ConfigurableProductOptionsValues.class);
        when(blue.getLabel()).thenReturn("Blue");
        ConfigurableProductOptionsValues red = mock(ConfigurableProductOptionsValues.class);
        when(red.getLabel()).thenReturn("Red");

        ConfigurableProductOptions color = mock(ConfigurableProductOptions.class);
        when(color.getAttributeCode()).thenReturn("fashion_color");
        when(color.getLabel()).thenReturn("Color");
        when(color.getValues()).thenReturn(Arrays.asList(blue, red));

        ConfigurableProduct product = mock(ConfigurableProduct.class);
        when(product.getConfigurableOptions()).thenReturn(Collections.singletonList(color));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> resolver.resolve(product, new HashMap<>()));
        assertTrue(ex.getMessage().contains("Color"));
        assertTrue(ex.getMessage().contains("Blue"));
        assertTrue(ex.getMessage().contains("Red"));
    }

    @Test
    public void throwsWithAvailableValuesWhenOptionValueInvalid() {
        ConfigurableProductOptionsValues blue = mock(ConfigurableProductOptionsValues.class);
        when(blue.getLabel()).thenReturn("Blue");

        ConfigurableProductOptions color = mock(ConfigurableProductOptions.class);
        when(color.getAttributeCode()).thenReturn("fashion_color");
        when(color.getLabel()).thenReturn("Color");
        when(color.getValues()).thenReturn(Collections.singletonList(blue));

        ConfigurableProduct product = mock(ConfigurableProduct.class);
        when(product.getConfigurableOptions()).thenReturn(Collections.singletonList(color));

        Map<String, String> supplied = new HashMap<>();
        supplied.put("fashion_color", "Purple");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> resolver.resolve(product, supplied));
        assertTrue(ex.getMessage().contains("must be one of"));
        assertTrue(ex.getMessage().contains("Blue"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd bundles/mcp && mvn -Dtest=ConfigurableOptionResolverTest test`
Expected: FAIL — compile error, `ConfigurableOptionResolver` does not exist yet.

- [ ] **Step 3: Write minimal implementation**

```java
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptions;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptionsValues;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.shopify.graphql.support.ID;

/**
 * Resolves human-readable option values (e.g. {@code {"color": "Blue"}}) supplied to {@code add_to_cart} into the
 * Magento option-value UIDs a configurable product's {@code addProductsToCart} mutation needs. Simple products
 * (no configurable options) resolve to an empty list. Matching is case-insensitive against either the option's
 * attribute code or its label, since an agent may naturally supply either.
 */
public class ConfigurableOptionResolver {

    public List<ID> resolve(ProductInterface product, Map<String, String> suppliedOptions) {
        List<ID> resolved = new ArrayList<>();
        if (!(product instanceof ConfigurableProduct)) {
            return resolved;
        }
        List<ConfigurableProductOptions> configurableOptions = ((ConfigurableProduct) product).getConfigurableOptions();
        if (configurableOptions == null) {
            return resolved;
        }
        for (ConfigurableProductOptions option : configurableOptions) {
            String suppliedValue = findSuppliedValue(suppliedOptions, option);
            if (suppliedValue == null) {
                throw new IllegalArgumentException(optionName(option) + " is required. Available values: " + availableValues(option));
            }
            ConfigurableProductOptionsValues match = findMatchingValue(option, suppliedValue);
            if (match == null) {
                throw new IllegalArgumentException(optionName(option) + " must be one of: " + availableValues(option));
            }
            resolved.add(match.getUid());
        }
        return resolved;
    }

    private String findSuppliedValue(Map<String, String> suppliedOptions, ConfigurableProductOptions option) {
        for (Map.Entry<String, String> entry : suppliedOptions.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(option.getAttributeCode()) || entry.getKey().equalsIgnoreCase(option.getLabel())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private ConfigurableProductOptionsValues findMatchingValue(ConfigurableProductOptions option, String suppliedValue) {
        if (option.getValues() == null) {
            return null;
        }
        for (ConfigurableProductOptionsValues value : option.getValues()) {
            if (suppliedValue.equalsIgnoreCase(value.getLabel())) {
                return value;
            }
        }
        return null;
    }

    private String optionName(ConfigurableProductOptions option) {
        return option.getLabel() != null ? option.getLabel() : option.getAttributeCode();
    }

    private String availableValues(ConfigurableProductOptions option) {
        if (option.getValues() == null) {
            return "";
        }
        return option.getValues().stream().map(ConfigurableProductOptionsValues::getLabel).collect(Collectors.joining(", "));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd bundles/mcp && mvn -Dtest=ConfigurableOptionResolverTest test`
Expected: PASS (5 tests)

- [ ] **Step 5: Commit**

```bash
git add bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/ConfigurableOptionResolver.java bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/ConfigurableOptionResolverTest.java
git commit -m "feat(mcp): add ConfigurableOptionResolver for add_to_cart"
```

---

### Task 2: Wire configurable option support into `AddToCartTool`

**Files:**
- Modify: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/AddToCartTool.java` (full replacement below)
- Modify: `bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/AddToCartToolTest.java` (full replacement below — the `addItem` override signature changes, so the existing two tests must be updated, not just added to)

**Interfaces:**
- Consumes: `ConfigurableOptionResolver.resolve(ProductInterface, Map<String,String>)` (Task 1); existing `McpProductRetriever` (extended with a query hook, same class, no signature change); existing `CartMutationClient.execute(StoreContext, MutationQueryDefinition)`, `DtoMapper.cart(...)`.
- Produces: `add_to_cart` now accepts an optional `options` object argument. `AddToCartTool.addItem(StoreContext, String, String, double, List<ID>)` replaces the old 4-arg signature (breaking change to this protected method, contained entirely within this task).

- [ ] **Step 1: Write the failing tests**

Replace `AddToCartToolTest.java` entirely:

```java
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

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.adobe.cq.commerce.magento.graphql.Cart;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptions;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptionsValues;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopify.graphql.support.ID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AddToCartToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void createsCartWhenNoCartIdSupplied() throws Exception {
        Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn(new ID("new-cart"));
        ProductInterface simpleProduct = mock(ProductInterface.class);

        AddToCartTool tool = new AddToCartTool() {
            @Override
            protected ProductInterface fetchProduct(StoreContext ctx, String sku) {
                return simpleProduct;
            }

            @Override
            protected String createEmptyCart(StoreContext ctx) {
                return "new-cart";
            }

            @Override
            protected Cart addItem(StoreContext ctx, String cartId, String sku, double quantity, List<ID> selectedOptions) {
                assertEquals("new-cart", cartId);
                assertEquals("VSK05", sku);
                assertEquals(1.0, quantity, 0.001);
                assertEquals(Collections.emptyList(), selectedOptions);
                return cart;
            }
        };

        JsonNode out = tool.call(mock(StoreContext.class), mapper.createObjectNode().put("sku", "VSK05").put("quantity", 1));
        assertEquals("new-cart", out.get("cart_id").asText());
        assertEquals("add_to_cart", tool.name());
        assertFalse(tool.writesContent());
    }

    @Test
    public void reusesSuppliedCartId() throws Exception {
        Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn(new ID("existing-cart"));
        ProductInterface simpleProduct = mock(ProductInterface.class);

        AddToCartTool tool = new AddToCartTool() {
            @Override
            protected ProductInterface fetchProduct(StoreContext ctx, String sku) {
                return simpleProduct;
            }

            @Override
            protected String createEmptyCart(StoreContext ctx) {
                throw new AssertionError("should not create a new cart when cart_id is supplied");
            }

            @Override
            protected Cart addItem(StoreContext ctx, String cartId, String sku, double quantity, List<ID> selectedOptions) {
                assertEquals("existing-cart", cartId);
                return cart;
            }
        };

        JsonNode out = tool.call(mock(StoreContext.class),
            mapper.createObjectNode().put("sku", "VSK05").put("quantity", 1).put("cart_id", "existing-cart"));
        assertEquals("existing-cart", out.get("cart_id").asText());
    }

    @Test
    public void resolvesConfigurableOptionsBeforeAddingItem() throws Exception {
        Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn(new ID("cart-1"));

        ConfigurableProductOptionsValues blue = mock(ConfigurableProductOptionsValues.class);
        when(blue.getLabel()).thenReturn("Blue");
        when(blue.getUid()).thenReturn(new ID("value-blue"));
        ConfigurableProductOptions color = mock(ConfigurableProductOptions.class);
        when(color.getAttributeCode()).thenReturn("fashion_color");
        when(color.getLabel()).thenReturn("Color");
        when(color.getValues()).thenReturn(Collections.singletonList(blue));
        ConfigurableProduct configurableProduct = mock(ConfigurableProduct.class);
        when(configurableProduct.getConfigurableOptions()).thenReturn(Collections.singletonList(color));

        AddToCartTool tool = new AddToCartTool() {
            @Override
            protected ProductInterface fetchProduct(StoreContext ctx, String sku) {
                return configurableProduct;
            }

            @Override
            protected Cart addItem(StoreContext ctx, String cartId, String sku, double quantity, List<ID> selectedOptions) {
                assertEquals(Collections.singletonList(new ID("value-blue")), selectedOptions);
                return cart;
            }
        };

        com.fasterxml.jackson.databind.node.ObjectNode args = mapper.createObjectNode().put("sku", "VSK05").put("quantity", 1)
            .put("cart_id", "cart-1");
        args.putObject("options").put("fashion_color", "Blue");
        JsonNode out = tool.call(mock(StoreContext.class), args);
        assertEquals("cart-1", out.get("cart_id").asText());
    }

    @Test
    public void throwsDescriptiveErrorWhenConfigurableOptionMissing() {
        ConfigurableProductOptionsValues blue = mock(ConfigurableProductOptionsValues.class);
        when(blue.getLabel()).thenReturn("Blue");
        ConfigurableProductOptions color = mock(ConfigurableProductOptions.class);
        when(color.getAttributeCode()).thenReturn("fashion_color");
        when(color.getLabel()).thenReturn("Color");
        when(color.getValues()).thenReturn(Collections.singletonList(blue));
        ConfigurableProduct configurableProduct = mock(ConfigurableProduct.class);
        when(configurableProduct.getConfigurableOptions()).thenReturn(Collections.singletonList(color));

        AddToCartTool tool = new AddToCartTool() {
            @Override
            protected ProductInterface fetchProduct(StoreContext ctx, String sku) {
                return configurableProduct;
            }

            @Override
            protected Cart addItem(StoreContext ctx, String cartId, String sku, double quantity, List<ID> selectedOptions) {
                throw new AssertionError("should not add to cart when a required option is missing");
            }
        };

        JsonNode args = mapper.createObjectNode().put("sku", "VSK05").put("quantity", 1).put("cart_id", "cart-1");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> tool.call(mock(StoreContext.class), args));
        assertEquals("Color is required. Available values: Blue", ex.getMessage());
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd bundles/mcp && mvn -Dtest=AddToCartToolTest test`
Expected: FAIL — compile error (`fetchProduct` not defined, `addItem` signature mismatch).

- [ ] **Step 3: Write the implementation**

Replace `AddToCartTool.java` entirely:

```java
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.magento.graphql.AddProductsToCartOutput;
import com.adobe.cq.commerce.magento.graphql.Cart;
import com.adobe.cq.commerce.magento.graphql.CartItemInput;
import com.adobe.cq.commerce.magento.graphql.CartUserInputError;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.adobe.cq.commerce.mcp.internal.dto.DtoMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.shopify.graphql.support.ID;

/**
 * MCP tool adding a SKU/quantity to a guest cart, creating the cart first if no cart id is supplied. Supports
 * configurable products via an optional {@code options} argument (e.g. {@code {"color": "Blue"}}), resolved to
 * Magento option-value UIDs by {@link ConfigurableOptionResolver}.
 */
@Component(service = McpTool.class)
public class AddToCartTool implements McpTool {
    private final ObjectMapper mapper = new ObjectMapper();
    private final CartMutationClient mutationClient = new CartMutationClient();
    private final ConfigurableOptionResolver optionResolver = new ConfigurableOptionResolver();

    @Override
    public String name() {
        return "add_to_cart";
    }

    @Override
    public String description() {
        return "Add a product to a guest cart, creating the cart first if no cart_id is supplied. For configurable "
            + "products (e.g. size/color variants), supply an 'options' object such as {\"color\": \"Blue\"}.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("sku").put("type", "string");
        properties.putObject("quantity").put("type", "integer");
        properties.putObject("cart_id").put("type", "string");
        properties.putObject("options").put("type", "object");
        schema.putArray("required").add("sku").add("quantity");
        return schema;
    }

    protected ProductInterface fetchProduct(StoreContext ctx, String sku) {
        McpProductRetriever retriever = new McpProductRetriever(ctx.getClient());
        retriever.setIdentifier(sku);
        retriever.extendProductQueryWith(q -> q.onConfigurableProduct(cp -> cp.configurableOptions(co -> co
            .attributeCode()
            .label()
            .values(v -> v.label().uid()))));
        return retriever.fetchProduct();
    }

    protected String createEmptyCart(StoreContext ctx) {
        return mutationClient.execute(ctx, m -> m.createEmptyCart()).getCreateEmptyCart();
    }

    protected Cart addItem(StoreContext ctx, String cartId, String sku, double quantity, List<ID> selectedOptions) {
        CartItemInput cartItem = new CartItemInput(quantity, sku);
        if (selectedOptions != null && !selectedOptions.isEmpty()) {
            cartItem.setSelectedOptions(selectedOptions);
        }
        AddProductsToCartOutput output = mutationClient
            .execute(ctx, m -> m.addProductsToCart(cartId, Collections.singletonList(cartItem), out -> out
                .cart(c -> c
                    .id()
                    .totalQuantity()
                    .items(i -> i
                        .uid()
                        .quantity()
                        .product(p -> p.sku().name())
                        .prices(pr -> pr.price(mo -> mo.value().currency()).rowTotal(mo -> mo.value().currency())))
                    .prices(cp -> cp.grandTotal(mo -> mo.value().currency())))
                .userErrors(e -> e.code().message())))
            .getAddProductsToCart();

        if (output.getUserErrors() != null && !output.getUserErrors().isEmpty()) {
            throw new IllegalArgumentException(
                output.getUserErrors().stream().map(CartUserInputError::getMessage).collect(Collectors.joining("; ")));
        }
        return output.getCart();
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String sku = args.path("sku").asText(null);
        JsonNode quantityNode = args.get("quantity");
        if (sku == null || quantityNode == null || quantityNode.asInt() < 1) {
            throw new IllegalArgumentException("sku and a positive quantity are required");
        }

        Map<String, String> suppliedOptions = new HashMap<>();
        JsonNode optionsNode = args.get("options");
        if (optionsNode != null && optionsNode.isObject()) {
            optionsNode.fields().forEachRemaining(entry -> suppliedOptions.put(entry.getKey(), entry.getValue().asText()));
        }

        ProductInterface product = fetchProduct(ctx, sku);
        List<ID> selectedOptions = optionResolver.resolve(product, suppliedOptions);

        String cartId = args.path("cart_id").asText(null);
        if (cartId == null) {
            cartId = createEmptyCart(ctx);
        }
        Cart cart = addItem(ctx, cartId, sku, quantityNode.asInt(), selectedOptions);
        return DtoMapper.cart(mapper, cart);
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd bundles/mcp && mvn -Dtest=AddToCartToolTest,ConfigurableOptionResolverTest test`
Expected: PASS (4 + 5 = 9 tests)

- [ ] **Step 5: Run the full module suite**

Run: `cd bundles/mcp && mvn test`
Expected: all tests pass (no regressions in other tools)

- [ ] **Step 6: Commit**

```bash
git add bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/AddToCartTool.java bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/AddToCartToolTest.java
git commit -m "feat(mcp): support configurable products in add_to_cart"
```

---

### Task 3: Live verification against the running AEM instance

Not a code task — this is the verification step this roadmap has consistently required before considering a phase done (Phase 1 caught a real bug this way).

- [ ] **Step 1: Build and redeploy**

```bash
cd /Users/aljoseph/Documents/projects/aem/aem-core-cif-components
mvn clean install -pl bundles/mcp -am -DskipTests -q
curl -s -u admin:admin -F "action=install" -F "bundlefile=@bundles/mcp/target/core-cif-components-mcp-2.18.5-SNAPSHOT.jar" -F "bundlestart=start" "http://localhost:4502/system/console/bundles" -o /dev/null -w "HTTP %{http_code}\n"
```

Expected: `HTTP 302`, and the bundle (id 658, confirm via `/system/console/bundles/658.json`) shows `"state": "Active"`.

- [ ] **Step 2: Find the real configurable options for a known configurable product**

`VSK05` (Agatha Skirt) was confirmed configurable in Phase 1 testing (errored on `addSimpleProductsToCart`). Query it directly against Magento to find its actual attribute codes/labels — the `get_product`/MCP tools don't expose `configurable_options` today, so query Magento's GraphQL endpoint directly for this one-off discovery step, e.g.:

```bash
curl -s "https://mcprod.catalogservice-commerce.fun/graphql" -H "Content-Type: application/json" -d '{"query":"{ products(filter:{sku:{eq:\"VSK05\"}}) { items { ... on ConfigurableProduct { configurable_options { attribute_code label values { label } } } } } }"}'
```

(Adjust the endpoint if this doesn't match — confirm the real GraphQL endpoint from the OSGi commerce cloud configuration if this guess is wrong.)

- [ ] **Step 3: Drive add_to_cart with the real options**

```bash
curl -s -u admin:admin -X POST "http://localhost:4502/content/venia/us/en.mcp.json" \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"add_to_cart","arguments":{"sku":"VSK05","quantity":1,"options":{"<attribute_code_from_step_2>":"<value_from_step_2>"}}}}'
```

Expected: a successful cart response (same DTO shape as Phase 1), not an error.

- [ ] **Step 4: Drive add_to_cart with a missing/wrong option to confirm the descriptive error**

```bash
curl -s -u admin:admin -X POST "http://localhost:4502/content/venia/us/en.mcp.json" \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"add_to_cart","arguments":{"sku":"VSK05","quantity":1}}}'
```

Expected: a tool error naming the real attribute label and its available values (not Magento's generic "You need to choose options for your item.").

- [ ] **Step 5: Update the spec and this plan with what testing found**

Add a "live verification results" section (same pattern as `2026-07-03-cif-shopper-cart.md`'s) documenting: the real attribute codes/labels this catalog uses, whether matching against attribute code vs. label worked as expected, and any field-name or behavior corrections needed. Fix `ConfigurableOptionResolver`/`AddToCartTool` if reality differs from what was assumed, re-run the full test suite, and commit the fix the same way Phase 1's `CartMutationClient` bug fix was committed.

## Live verification results (2026-07-03)

Deployed to the running local AEM author instance and drove the full flow against the real Magento backend. **No bugs found this time** — everything worked exactly as designed on the first try.

- Real attribute codes for `VSK05` (Agatha Skirt), confirmed via a direct GraphQL query against `https://mcprod.catalogservice-commerce.fun/graphql`: `fashion_color` (label "Fashion Color"; values Peach, Khaki, Lilac, Rain) and `fashion_size` (label "Fashion Size"; values L, M, S, XS) — matches the `fashion_color`/`fashion_size` attribute codes already seen in `get_attributes` output during earlier phases, confirming the earlier guess in the spec's "open items" was correct.
- `add_to_cart` with `{"sku":"VSK05","quantity":1,"options":{"fashion_color":"Peach","fashion_size":"M"}}` → succeeded, correct cart DTO returned (previously failed in Phase 1 with Magento's generic error).
- `add_to_cart` with no `options` on the same SKU → `"Fashion Color is required. Available values: Peach, Khaki, Lilac, Rain"` — the descriptive error works exactly as designed, naming the option's human label (not the raw attribute code) and its real values.
- `add_to_cart` with an invalid option value (`"fashion_color":"Purple"`) → `"Fashion Color must be one of: Peach, Khaki, Lilac, Rain"`.
- `add_to_cart` on a simple product (`VA13-GO-NA`, no options) → still works unchanged, confirming the switch to the unified `addProductsToCart` mutation didn't regress simple-product support.

Matching by attribute code (`fashion_color`) worked as the primary/expected input; matching by label was not separately exercised live in this pass (only unit-tested), but no issue is expected since it's the same code path.

## Post-implementation code review (2026-07-03)

A `--level high` code review (run across this plan + the base cart-tools plan together) found one real bug specific to this phase, fixed with a new test:

- **`add_to_cart`'s `options` handling treated a JSON `null` value as the literal string `"null"`, not as an absent option.** `{"options": {"fashion_color": null}}` would previously fail with the misleading `"Fashion Color must be one of: Peach, Khaki, Lilac, Rain"` (as if `"null"` were an attempted, invalid value) instead of the correct `"Fashion Color is required. Available values: ..."`. Root cause: Jackson's `NullNode.asText()` returns the string `"null"`, not Java `null` — a JSON-parsing gotcha, not a logic error in `ConfigurableOptionResolver` itself. Fixed in `AddToCartTool.call()` by skipping any options-object entry where `entry.getValue().isNull()` before adding it to the supplied-options map, so it's treated the same as the key being absent entirely. Covered by `AddToCartToolTest.treatsJsonNullOptionValueAsMissingNotAsTheStringNull`.

See the base cart-tools plan's own "Post-implementation code review" section for the other fixes from this same review pass (shared `CartMutationClient.cartFields()`, null-guard, non-cached cart reads, and the fractional-quantity fix in `UpdateCartItemTool`, which is base-plan scope even though it surfaced in the same review run).
