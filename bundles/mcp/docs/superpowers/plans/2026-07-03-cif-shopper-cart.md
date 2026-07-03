# CIF Shopper Guest Cart Tools Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add four guest-cart MCP tools (`add_to_cart`, `view_cart`, `update_cart_item`, `clear_cart`) to the `cif-shopper` endpoint.

**Architecture:** Same flat `internal/tools/` placement and `McpTool` contract as every existing tool. `view_cart` is a plain GraphQL query, so it reuses `MagentoGraphqlClient.execute()` exactly like existing read tools. The other three are mutations, which need a small shared helper (`CartMutationClient`) that talks to the raw `GraphqlClient` directly, because `MagentoGraphqlClient.execute()` can only deserialize `Query`-shaped responses.

**Tech Stack:** Java 8, `magento-graphql` 9.1.0 query/mutation builders, `graphql-client` `GraphqlClient`, Jackson, JUnit 4 + Mockito.

## Global Constraints

- All new tools: `writesContent() == false` (they mutate the remote commerce backend, not AEM content) — required so they stay visible on the anonymous `cif-shopper` endpoint.
- All new classes live flat in `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/` — no new subpackages.
- DTO mapping goes on the existing shared `internal/dto/DtoMapper.java` — no new mapper class.
- Field names mirror Magento's own schema (`uid`, `sku`, `quantity`, `totalQuantity`) rather than invented synonyms.
- No changes to `bundles/core`.
- Exact field/response shapes are provisional — confirm and adjust against a running AEM+Magento instance during/after implementation; don't over-verify via bytecode inspection before writing code.

---

### Task 1: `CartMutationClient` — shared mutation execution helper

**Files:**
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/CartMutationClient.java`
- Test: `bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/CartMutationClientTest.java`
- Modify: `bundles/mcp/pom.xml` — add a test-scope `com.google.code.gson:gson:2.8.9` dependency (same version `bundles/core/pom.xml` already uses for its own tests). `magento-graphql`/`graphql-client` are `provided` scope, and Maven does not propagate transitive dependencies of `provided`-scope dependencies, so `gson` (needed at test runtime by `MutationDeserializer.getGson()`) isn't otherwise on the test classpath. Discovered by actually running the test (`NoClassDefFoundError: com/google/gson/internal/Excluder`), not by inspection.

**Interfaces:**
- Produces: `public class CartMutationClient { public Mutation execute(StoreContext ctx, MutationQueryDefinition definition) }` — throws `IllegalStateException` if the GraphQL response has errors, otherwise returns the typed `Mutation` result. Tasks 3–5 call this.

- [ ] **Step 1: Write the failing test**

```java
package com.adobe.cq.commerce.mcp.internal.tools;

import java.util.Collections;

import org.apache.sling.api.resource.Resource;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.graphql.client.RequestOptions;
import com.adobe.cq.commerce.magento.graphql.Mutation;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.mcp.internal.StoreContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CartMutationClientTest {

    @Test
    public void returnsMutationDataOnSuccess() {
        Mutation mutation = mock(Mutation.class);
        GraphqlResponse<Mutation, Error> response = new GraphqlResponse<>();
        response.setData(mutation);

        Resource resource = mock(Resource.class);
        GraphqlClient graphqlClient = mock(GraphqlClient.class);
        when(resource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);
        when(graphqlClient.<Mutation, Error>execute(any(GraphqlRequest.class), eq(Mutation.class), eq(Error.class),
            any(RequestOptions.class))).thenReturn(response);

        MagentoGraphqlClient magentoClient = mock(MagentoGraphqlClient.class);
        when(magentoClient.getHttpHeaderMap()).thenReturn(Collections.emptyMap());

        StoreContext ctx = mock(StoreContext.class);
        when(ctx.getResource()).thenReturn(resource);
        when(ctx.getClient()).thenReturn(magentoClient);

        CartMutationClient client = new CartMutationClient();
        Mutation result = client.execute(ctx, m -> m.createEmptyCart());

        assertEquals(mutation, result);
    }

    @Test
    public void throwsWithMagentoErrorMessageOnFailure() {
        Error error = new Error();
        error.setMessage("The cart isn't active.");
        GraphqlResponse<Mutation, Error> response = new GraphqlResponse<>();
        response.setErrors(Collections.singletonList(error));

        Resource resource = mock(Resource.class);
        GraphqlClient graphqlClient = mock(GraphqlClient.class);
        when(resource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);
        when(graphqlClient.<Mutation, Error>execute(any(GraphqlRequest.class), eq(Mutation.class), eq(Error.class),
            any(RequestOptions.class))).thenReturn(response);

        MagentoGraphqlClient magentoClient = mock(MagentoGraphqlClient.class);
        when(magentoClient.getHttpHeaderMap()).thenReturn(Collections.emptyMap());

        StoreContext ctx = mock(StoreContext.class);
        when(ctx.getResource()).thenReturn(resource);
        when(ctx.getClient()).thenReturn(magentoClient);

        CartMutationClient client = new CartMutationClient();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> client.execute(ctx, m -> m.createEmptyCart()));
        assertEquals("The cart isn't active.", ex.getMessage());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd bundles/mcp && mvn -q -Dtest=CartMutationClientTest test`
Expected: FAIL — compile error, `CartMutationClient` does not exist yet.

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

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.commerce.graphql.client.RequestOptions;
import com.adobe.cq.commerce.magento.graphql.Mutation;
import com.adobe.cq.commerce.magento.graphql.MutationQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.magento.graphql.gson.MutationDeserializer;
import com.adobe.cq.commerce.mcp.internal.StoreContext;

/**
 * Executes Magento GraphQL mutations for cart tools. {@code MagentoGraphqlClient.execute} cannot be used for mutations:
 * its implementation hardcodes response deserialization to {@code Query.class}. This adapts the endpoint's own
 * resource to the raw {@link GraphqlClient} instead, using {@link MutationDeserializer#getGson()}.
 */
public class CartMutationClient {

    public Mutation execute(StoreContext ctx, MutationQueryDefinition definition) {
        GraphqlClient graphqlClient = ctx.getResource().adaptTo(GraphqlClient.class);
        if (graphqlClient == null) {
            throw new IllegalStateException("GraphQL client not available for resource " + ctx.getResource().getPath());
        }

        String mutation = Operations.mutation(definition).toString();
        RequestOptions options = new RequestOptions()
            .withGson(MutationDeserializer.getGson())
            .withHeaders(toHeaders(ctx.getClient().getHttpHeaderMap()))
            .withHttpMethod(HttpMethod.POST);

        GraphqlResponse<Mutation, Error> response = graphqlClient.execute(new GraphqlRequest(mutation), Mutation.class, Error.class,
            options);

        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            StringBuilder message = new StringBuilder();
            for (Error error : response.getErrors()) {
                if (message.length() > 0) {
                    message.append("; ");
                }
                message.append(error.getMessage());
            }
            throw new IllegalStateException(message.toString());
        }

        return response.getData();
    }

    private List<Header> toHeaders(Map<String, String[]> headerMap) {
        List<Header> headers = new ArrayList<>();
        if (headerMap != null) {
            for (Map.Entry<String, String[]> entry : headerMap.entrySet()) {
                for (String value : entry.getValue()) {
                    headers.add(new BasicHeader(entry.getKey(), value));
                }
            }
        }
        return headers.isEmpty() ? null : headers;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd bundles/mcp && mvn -q -Dtest=CartMutationClientTest test`
Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

```bash
git add bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/CartMutationClient.java bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/CartMutationClientTest.java
git commit -m "feat(mcp): add CartMutationClient for executing cart mutations"
```

---

### Task 2: `DtoMapper.cart()` — map a Magento `Cart` to a compact DTO

**Files:**
- Modify: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/dto/DtoMapper.java`
- Modify: `bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/dto/DtoMapperTest.java`

**Interfaces:**
- Consumes: `com.adobe.cq.commerce.magento.graphql.Cart` and its nested `CartItemInterface`/`CartItemPrices`/`CartPrices`/`Money` getters.
- Produces: `public static ObjectNode cart(ObjectMapper mapper, Cart cart)` → `{ cart_id, items: [{ uid, sku, name, quantity, price, currency, rowTotal }], grandTotal, currency, totalQuantity }`. Tasks 3–6 call this.

- [ ] **Step 1: Write the failing test**

Add to `DtoMapperTest.java`:

```java
    @Test
    public void mapsCart() {
        com.adobe.cq.commerce.magento.graphql.ProductInterface product = mock(com.adobe.cq.commerce.magento.graphql.ProductInterface.class);
        when(product.getSku()).thenReturn("VSK05");
        when(product.getName()).thenReturn("Agatha Skirt");

        com.adobe.cq.commerce.magento.graphql.Money price = mock(com.adobe.cq.commerce.magento.graphql.Money.class);
        when(price.getValue()).thenReturn(78.0);
        when(price.getCurrency()).thenReturn(com.adobe.cq.commerce.magento.graphql.CurrencyEnum.USD);

        com.adobe.cq.commerce.magento.graphql.CartItemPrices itemPrices = mock(com.adobe.cq.commerce.magento.graphql.CartItemPrices.class);
        when(itemPrices.getPrice()).thenReturn(price);
        when(itemPrices.getRowTotal()).thenReturn(price);

        com.adobe.cq.commerce.magento.graphql.CartItemInterface item = mock(com.adobe.cq.commerce.magento.graphql.CartItemInterface.class);
        when(item.getUid()).thenReturn(new ID("item-1"));
        when(item.getProduct()).thenReturn(product);
        when(item.getQuantity()).thenReturn(2.0);
        when(item.getPrices()).thenReturn(itemPrices);

        com.adobe.cq.commerce.magento.graphql.CartPrices cartPrices = mock(com.adobe.cq.commerce.magento.graphql.CartPrices.class);
        when(cartPrices.getGrandTotal()).thenReturn(price);

        com.adobe.cq.commerce.magento.graphql.Cart cart = mock(com.adobe.cq.commerce.magento.graphql.Cart.class);
        when(cart.getId()).thenReturn(new ID("cart-1"));
        when(cart.getItems()).thenReturn(Collections.singletonList(item));
        when(cart.getPrices()).thenReturn(cartPrices);
        when(cart.getTotalQuantity()).thenReturn(2.0);

        ObjectNode dto = DtoMapper.cart(mapper, cart);
        assertEquals("cart-1", dto.get("cart_id").asText());
        assertEquals(1, dto.get("items").size());
        JsonNode itemNode = dto.get("items").get(0);
        assertEquals("item-1", itemNode.get("uid").asText());
        assertEquals("VSK05", itemNode.get("sku").asText());
        assertEquals("Agatha Skirt", itemNode.get("name").asText());
        assertEquals(2.0, itemNode.get("quantity").asDouble(), 0.001);
        assertEquals(78.0, itemNode.get("price").asDouble(), 0.001);
        assertEquals("USD", itemNode.get("currency").asText());
        assertEquals(78.0, dto.get("grandTotal").asDouble(), 0.001);
        assertEquals("USD", dto.get("currency").asText());
        assertEquals(2.0, dto.get("totalQuantity").asDouble(), 0.001);
    }
```

Add `import com.fasterxml.jackson.databind.JsonNode;` to the test file's imports if not already present.

- [ ] **Step 2: Run test to verify it fails**

Run: `cd bundles/mcp && mvn -q -Dtest=DtoMapperTest test`
Expected: FAIL — compile error, `DtoMapper.cart(...)` does not exist yet.

- [ ] **Step 3: Write minimal implementation**

Add imports to `DtoMapper.java`: `Cart`, `CartItemInterface`, `Money`, `ProductInterface` (all `com.adobe.cq.commerce.magento.graphql.*`). Add the method:

```java
    public static ObjectNode cart(ObjectMapper mapper, Cart cart) {
        ObjectNode node = mapper.createObjectNode();
        node.put("cart_id", cart.getId() != null ? cart.getId().toString() : null);

        ArrayNode items = node.putArray("items");
        if (cart.getItems() != null) {
            for (CartItemInterface item : cart.getItems()) {
                ObjectNode itemNode = items.addObject();
                itemNode.put("uid", item.getUid() != null ? item.getUid().toString() : null);
                ProductInterface product = item.getProduct();
                itemNode.put("sku", product != null ? product.getSku() : null);
                itemNode.put("name", product != null ? product.getName() : null);
                itemNode.put("quantity", item.getQuantity());
                if (item.getPrices() != null) {
                    Money price = item.getPrices().getPrice();
                    if (price != null) {
                        itemNode.put("price", price.getValue());
                        itemNode.put("currency", price.getCurrency() != null ? price.getCurrency().toString() : null);
                    }
                    Money rowTotal = item.getPrices().getRowTotal();
                    if (rowTotal != null) {
                        itemNode.put("rowTotal", rowTotal.getValue());
                    }
                }
            }
        }

        if (cart.getPrices() != null && cart.getPrices().getGrandTotal() != null) {
            Money grandTotal = cart.getPrices().getGrandTotal();
            node.put("grandTotal", grandTotal.getValue());
            node.put("currency", grandTotal.getCurrency() != null ? grandTotal.getCurrency().toString() : null);
        }
        node.put("totalQuantity", cart.getTotalQuantity());
        return node;
    }
```

Verified against a real build: `mvn -Dtest=DtoMapperTest test` → 6/6 pass.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd bundles/mcp && mvn -q -Dtest=DtoMapperTest test`
Expected: PASS (all DtoMapperTest tests, including the new one)

- [ ] **Step 5: Commit**

```bash
git add bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/dto/DtoMapper.java bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/dto/DtoMapperTest.java
git commit -m "feat(mcp): add DtoMapper.cart() for compact cart DTOs"
```

---

### Task 3: `ViewCartTool` (`view_cart`)

**Files:**
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/ViewCartTool.java`
- Test: `bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/ViewCartToolTest.java`

**Interfaces:**
- Consumes: `DtoMapper.cart(mapper, cart)` (Task 2).
- Produces: MCP tool `view_cart`, input `{ cart_id }`, output = `DtoMapper.cart(...)` shape.

- [ ] **Step 1: Write the failing test**

```java
package com.adobe.cq.commerce.mcp.internal.tools;

import org.junit.Test;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.magento.graphql.Cart;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopify.graphql.support.ID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ViewCartToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void returnsCartForCartId() throws Exception {
        Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn(new ID("cart-1"));

        StoreContext ctx = mock(StoreContext.class);
        when(ctx.getClient()).thenReturn(mock(MagentoGraphqlClient.class));

        ViewCartTool tool = new ViewCartTool() {
            @Override
            protected Cart fetch(StoreContext c, String cartId) {
                return cart;
            }
        };
        JsonNode out = tool.call(ctx, mapper.createObjectNode().put("cart_id", "cart-1"));
        assertEquals("cart-1", out.get("cart_id").asText());
        assertEquals("view_cart", tool.name());
        assertFalse(tool.writesContent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void requiresCartId() throws Exception {
        StoreContext ctx = mock(StoreContext.class);
        new ViewCartTool().call(ctx, mapper.createObjectNode());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd bundles/mcp && mvn -q -Dtest=ViewCartToolTest test`
Expected: FAIL — compile error, `ViewCartTool` does not exist yet.

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

import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.Cart;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.adobe.cq.commerce.mcp.internal.dto.DtoMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP tool fetching the current contents of a guest cart by cart id.
 */
@Component(service = McpTool.class)
public class ViewCartTool implements McpTool {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "view_cart";
    }

    @Override
    public String description() {
        return "Fetch the current contents of a guest cart by cart id.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        schema.putObject("properties").putObject("cart_id").put("type", "string");
        schema.putArray("required").add("cart_id");
        return schema;
    }

    protected Cart fetch(StoreContext ctx, String cartId) {
        String query = Operations.query(q -> q.cart(cartId, c -> c
            .id()
            .totalQuantity()
            .items(i -> i
                .uid()
                .quantity()
                .product(p -> p.sku().name())
                .prices(pr -> pr.price(m -> m.value().currency()).rowTotal(m -> m.value().currency())))
            .prices(cp -> cp.grandTotal(m -> m.value().currency())))).toString();
        GraphqlResponse<Query, Error> response = ctx.getClient().execute(query);
        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            throw new IllegalArgumentException(response.getErrors().get(0).getMessage());
        }
        return response.getData().getCart();
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String cartId = args.path("cart_id").asText(null);
        if (cartId == null) {
            throw new IllegalArgumentException("cart_id is required");
        }
        Cart cart = fetch(ctx, cartId);
        return DtoMapper.cart(mapper, cart);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd bundles/mcp && mvn -q -Dtest=ViewCartToolTest test`
Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

```bash
git add bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/ViewCartTool.java bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/ViewCartToolTest.java
git commit -m "feat(mcp): add view_cart tool"
```

---

### Task 4: `AddToCartTool` (`add_to_cart`)

**Files:**
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/AddToCartTool.java`
- Test: `bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/AddToCartToolTest.java`

**Interfaces:**
- Consumes: `CartMutationClient.execute(ctx, definition)` (Task 1), `DtoMapper.cart(mapper, cart)` (Task 2).
- Produces: MCP tool `add_to_cart`, input `{ sku, quantity, cart_id? }`, output = `DtoMapper.cart(...)` shape.

- [ ] **Step 1: Write the failing test**

```java
package com.adobe.cq.commerce.mcp.internal.tools;

import org.junit.Test;

import com.adobe.cq.commerce.magento.graphql.Cart;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopify.graphql.support.ID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AddToCartToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void createsCartWhenNoCartIdSupplied() throws Exception {
        Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn(new ID("new-cart"));

        AddToCartTool tool = new AddToCartTool() {
            @Override
            protected String createEmptyCart(StoreContext ctx) {
                return "new-cart";
            }

            @Override
            protected Cart addItem(StoreContext ctx, String cartId, String sku, double quantity) {
                assertEquals("new-cart", cartId);
                assertEquals("VSK05", sku);
                assertEquals(1.0, quantity, 0.001);
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

        AddToCartTool tool = new AddToCartTool() {
            @Override
            protected String createEmptyCart(StoreContext ctx) {
                throw new AssertionError("should not create a new cart when cart_id is supplied");
            }

            @Override
            protected Cart addItem(StoreContext ctx, String cartId, String sku, double quantity) {
                assertEquals("existing-cart", cartId);
                return cart;
            }
        };

        JsonNode out = tool.call(mock(StoreContext.class),
            mapper.createObjectNode().put("sku", "VSK05").put("quantity", 1).put("cart_id", "existing-cart"));
        assertEquals("existing-cart", out.get("cart_id").asText());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd bundles/mcp && mvn -q -Dtest=AddToCartToolTest test`
Expected: FAIL — compile error, `AddToCartTool` does not exist yet.

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

import java.util.Collections;

import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.magento.graphql.AddSimpleProductsToCartInput;
import com.adobe.cq.commerce.magento.graphql.Cart;
import com.adobe.cq.commerce.magento.graphql.CartItemInput;
import com.adobe.cq.commerce.magento.graphql.SimpleProductCartItemInput;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.adobe.cq.commerce.mcp.internal.dto.DtoMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP tool adding a SKU/quantity to a guest cart, creating the cart first if no cart id is supplied.
 */
@Component(service = McpTool.class)
public class AddToCartTool implements McpTool {
    private final ObjectMapper mapper = new ObjectMapper();
    private final CartMutationClient mutationClient = new CartMutationClient();

    @Override
    public String name() {
        return "add_to_cart";
    }

    @Override
    public String description() {
        return "Add a product to a guest cart, creating the cart first if no cart_id is supplied.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("sku").put("type", "string");
        properties.putObject("quantity").put("type", "integer");
        properties.putObject("cart_id").put("type", "string");
        schema.putArray("required").add("sku").add("quantity");
        return schema;
    }

    protected String createEmptyCart(StoreContext ctx) {
        return mutationClient.execute(ctx, m -> m.createEmptyCart()).getCreateEmptyCart();
    }

    protected Cart addItem(StoreContext ctx, String cartId, String sku, double quantity) {
        SimpleProductCartItemInput cartItem = new SimpleProductCartItemInput(new CartItemInput(quantity, sku));
        AddSimpleProductsToCartInput input = new AddSimpleProductsToCartInput(cartId, Collections.singletonList(cartItem));
        return mutationClient
            .execute(ctx, m -> m.addSimpleProductsToCart(args -> args.input(input), out -> out.cart(c -> c
                .id()
                .totalQuantity()
                .items(i -> i
                    .uid()
                    .quantity()
                    .product(p -> p.sku().name())
                    .prices(pr -> pr.price(mo -> mo.value().currency()).rowTotal(mo -> mo.value().currency())))
                .prices(cp -> cp.grandTotal(mo -> mo.value().currency())))))
            .getAddSimpleProductsToCart()
            .getCart();
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String sku = args.path("sku").asText(null);
        JsonNode quantityNode = args.get("quantity");
        if (sku == null || quantityNode == null || quantityNode.asInt() < 1) {
            throw new IllegalArgumentException("sku and a positive quantity are required");
        }
        String cartId = args.path("cart_id").asText(null);
        if (cartId == null) {
            cartId = createEmptyCart(ctx);
        }
        Cart cart = addItem(ctx, cartId, sku, quantityNode.asInt());
        return DtoMapper.cart(mapper, cart);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd bundles/mcp && mvn -q -Dtest=AddToCartToolTest test`
Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

```bash
git add bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/AddToCartTool.java bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/AddToCartToolTest.java
git commit -m "feat(mcp): add add_to_cart tool"
```

---

### Task 5: `UpdateCartItemTool` (`update_cart_item`)

**Files:**
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/UpdateCartItemTool.java`
- Test: `bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/UpdateCartItemToolTest.java`

**Interfaces:**
- Consumes: `CartMutationClient.execute(ctx, definition)` (Task 1), `DtoMapper.cart(mapper, cart)` (Task 2).
- Produces: MCP tool `update_cart_item`, input `{ cart_id, uid, quantity }`. `quantity >= 1` updates; `quantity == 0` removes.

- [ ] **Step 1: Write the failing test**

```java
package com.adobe.cq.commerce.mcp.internal.tools;

import org.junit.Test;

import com.adobe.cq.commerce.magento.graphql.Cart;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopify.graphql.support.ID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UpdateCartItemToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void positiveQuantityUpdatesItem() throws Exception {
        Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn(new ID("cart-1"));

        UpdateCartItemTool tool = new UpdateCartItemTool() {
            @Override
            protected Cart updateQuantity(StoreContext ctx, String cartId, String uid, double quantity) {
                assertEquals("cart-1", cartId);
                assertEquals("item-1", uid);
                assertEquals(3.0, quantity, 0.001);
                return cart;
            }

            @Override
            protected Cart removeItem(StoreContext ctx, String cartId, String uid) {
                throw new AssertionError("should not remove when quantity >= 1");
            }
        };

        JsonNode out = tool.call(mock(StoreContext.class),
            mapper.createObjectNode().put("cart_id", "cart-1").put("uid", "item-1").put("quantity", 3));
        assertEquals("cart-1", out.get("cart_id").asText());
        assertEquals("update_cart_item", tool.name());
        assertFalse(tool.writesContent());
    }

    @Test
    public void zeroQuantityRemovesItem() throws Exception {
        Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn(new ID("cart-1"));

        UpdateCartItemTool tool = new UpdateCartItemTool() {
            @Override
            protected Cart updateQuantity(StoreContext ctx, String cartId, String uid, double quantity) {
                throw new AssertionError("should not update when quantity == 0");
            }

            @Override
            protected Cart removeItem(StoreContext ctx, String cartId, String uid) {
                assertEquals("cart-1", cartId);
                assertEquals("item-1", uid);
                return cart;
            }
        };

        JsonNode out = tool.call(mock(StoreContext.class),
            mapper.createObjectNode().put("cart_id", "cart-1").put("uid", "item-1").put("quantity", 0));
        assertEquals("cart-1", out.get("cart_id").asText());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd bundles/mcp && mvn -q -Dtest=UpdateCartItemToolTest test`
Expected: FAIL — compile error, `UpdateCartItemTool` does not exist yet.

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

import java.util.Collections;

import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.magento.graphql.Cart;
import com.adobe.cq.commerce.magento.graphql.CartItemUpdateInput;
import com.adobe.cq.commerce.magento.graphql.RemoveItemFromCartInput;
import com.adobe.cq.commerce.magento.graphql.UpdateCartItemsInput;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.adobe.cq.commerce.mcp.internal.dto.DtoMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.shopify.graphql.support.ID;

/**
 * MCP tool changing a cart line item's quantity, or removing it when quantity is 0.
 */
@Component(service = McpTool.class)
public class UpdateCartItemTool implements McpTool {
    private final ObjectMapper mapper = new ObjectMapper();
    private final CartMutationClient mutationClient = new CartMutationClient();

    @Override
    public String name() {
        return "update_cart_item";
    }

    @Override
    public String description() {
        return "Update a cart line item's quantity (quantity 0 removes the item).";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("cart_id").put("type", "string");
        properties.putObject("uid").put("type", "string");
        properties.putObject("quantity").put("type", "integer");
        schema.putArray("required").add("cart_id").add("uid").add("quantity");
        return schema;
    }

    private com.adobe.cq.commerce.magento.graphql.CartQueryDefinition cartFields() {
        return c -> c
            .id()
            .totalQuantity()
            .items(i -> i
                .uid()
                .quantity()
                .product(p -> p.sku().name())
                .prices(pr -> pr.price(m -> m.value().currency()).rowTotal(m -> m.value().currency())))
            .prices(cp -> cp.grandTotal(m -> m.value().currency()));
    }

    protected Cart updateQuantity(StoreContext ctx, String cartId, String uid, double quantity) {
        CartItemUpdateInput itemInput = new CartItemUpdateInput().setCartItemUid(new ID(uid)).setQuantity(quantity);
        UpdateCartItemsInput input = new UpdateCartItemsInput(cartId, Collections.singletonList(itemInput));
        return mutationClient
            .execute(ctx, m -> m.updateCartItems(args -> args.input(input), out -> out.cart(cartFields())))
            .getUpdateCartItems()
            .getCart();
    }

    protected Cart removeItem(StoreContext ctx, String cartId, String uid) {
        RemoveItemFromCartInput input = new RemoveItemFromCartInput(cartId).setCartItemUid(new ID(uid));
        return mutationClient
            .execute(ctx, m -> m.removeItemFromCart(args -> args.input(input), out -> out.cart(cartFields())))
            .getRemoveItemFromCart()
            .getCart();
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String cartId = args.path("cart_id").asText(null);
        String uid = args.path("uid").asText(null);
        JsonNode quantityNode = args.get("quantity");
        if (cartId == null || uid == null || quantityNode == null || quantityNode.asInt() < 0) {
            throw new IllegalArgumentException("cart_id, uid and a non-negative quantity are required");
        }
        int quantity = quantityNode.asInt();
        Cart cart = quantity == 0 ? removeItem(ctx, cartId, uid) : updateQuantity(ctx, cartId, uid, quantity);
        return DtoMapper.cart(mapper, cart);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd bundles/mcp && mvn -q -Dtest=UpdateCartItemToolTest test`
Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

```bash
git add bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/UpdateCartItemTool.java bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/UpdateCartItemToolTest.java
git commit -m "feat(mcp): add update_cart_item tool"
```

---

### Task 6: `ClearCartTool` (`clear_cart`)

**Files:**
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/ClearCartTool.java`
- Test: `bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/ClearCartToolTest.java`

**Interfaces:**
- Consumes: `DtoMapper.cart(mapper, cart)` (Task 2). Reuses the same remove-by-uid mutation `UpdateCartItemTool` uses, but doesn't depend on that class directly — it builds its own `RemoveItemFromCartInput` calls via `CartMutationClient`, one per existing item, fetched first via the same query `ViewCartTool` uses.
- Produces: MCP tool `clear_cart`, input `{ cart_id }`, output = empty-cart `DtoMapper.cart(...)` shape.

- [ ] **Step 1: Write the failing test**

```java
package com.adobe.cq.commerce.mcp.internal.tools;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.adobe.cq.commerce.magento.graphql.Cart;
import com.adobe.cq.commerce.magento.graphql.CartItemInterface;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopify.graphql.support.ID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClearCartToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void removesEveryItemThenReturnsEmptyCart() throws Exception {
        CartItemInterface item1 = mock(CartItemInterface.class);
        when(item1.getUid()).thenReturn(new ID("item-1"));
        CartItemInterface item2 = mock(CartItemInterface.class);
        when(item2.getUid()).thenReturn(new ID("item-2"));

        Cart cartWithItems = mock(Cart.class);
        when(cartWithItems.getItems()).thenReturn(Arrays.asList(item1, item2));

        Cart emptyCart = mock(Cart.class);
        when(emptyCart.getId()).thenReturn(new ID("cart-1"));

        List<String> removedUids = new java.util.ArrayList<>();
        ClearCartTool tool = new ClearCartTool() {
            @Override
            protected Cart fetch(StoreContext ctx, String cartId) {
                return cartWithItems;
            }

            @Override
            protected Cart removeItem(StoreContext ctx, String cartId, String uid) {
                removedUids.add(uid);
                return emptyCart;
            }
        };

        JsonNode out = tool.call(mock(StoreContext.class), mapper.createObjectNode().put("cart_id", "cart-1"));
        assertEquals(Arrays.asList("item-1", "item-2"), removedUids);
        assertEquals("cart-1", out.get("cart_id").asText());
        assertEquals("clear_cart", tool.name());
        assertFalse(tool.writesContent());
    }

    @Test
    public void returnsCartUnchangedWhenAlreadyEmpty() throws Exception {
        Cart emptyCart = mock(Cart.class);
        when(emptyCart.getId()).thenReturn(new ID("cart-1"));
        when(emptyCart.getItems()).thenReturn(java.util.Collections.emptyList());

        ClearCartTool tool = new ClearCartTool() {
            @Override
            protected Cart fetch(StoreContext ctx, String cartId) {
                return emptyCart;
            }

            @Override
            protected Cart removeItem(StoreContext ctx, String cartId, String uid) {
                throw new AssertionError("should not remove anything from an already-empty cart");
            }
        };

        JsonNode out = tool.call(mock(StoreContext.class), mapper.createObjectNode().put("cart_id", "cart-1"));
        assertEquals("cart-1", out.get("cart_id").asText());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd bundles/mcp && mvn -q -Dtest=ClearCartToolTest test`
Expected: FAIL — compile error, `ClearCartTool` does not exist yet.

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

import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.magento.graphql.Cart;
import com.adobe.cq.commerce.magento.graphql.CartItemInterface;
import com.adobe.cq.commerce.magento.graphql.RemoveItemFromCartInput;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.adobe.cq.commerce.mcp.internal.dto.DtoMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.shopify.graphql.support.ID;

/**
 * MCP tool removing every item from a guest cart. Magento has no single "empty cart" mutation, so this fetches the
 * current items and removes them one at a time, stopping on the first failure (no silent partial clears).
 */
@Component(service = McpTool.class)
public class ClearCartTool implements McpTool {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ViewCartTool viewCartTool = new ViewCartTool();
    private final CartMutationClient mutationClient = new CartMutationClient();

    @Override
    public String name() {
        return "clear_cart";
    }

    @Override
    public String description() {
        return "Remove every item from a guest cart.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        schema.putObject("properties").putObject("cart_id").put("type", "string");
        schema.putArray("required").add("cart_id");
        return schema;
    }

    protected Cart fetch(StoreContext ctx, String cartId) {
        return viewCartTool.fetch(ctx, cartId);
    }

    protected Cart removeItem(StoreContext ctx, String cartId, String uid) {
        RemoveItemFromCartInput input = new RemoveItemFromCartInput(cartId).setCartItemUid(new ID(uid));
        return mutationClient
            .execute(ctx, m -> m.removeItemFromCart(args -> args.input(input), out -> out.cart(c -> c
                .id()
                .totalQuantity()
                .items(i -> i
                    .uid()
                    .quantity()
                    .product(p -> p.sku().name())
                    .prices(pr -> pr.price(mo -> mo.value().currency()).rowTotal(mo -> mo.value().currency())))
                .prices(cp -> cp.grandTotal(mo -> mo.value().currency())))))
            .getRemoveItemFromCart()
            .getCart();
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;
        String cartId = args.path("cart_id").asText(null);
        if (cartId == null) {
            throw new IllegalArgumentException("cart_id is required");
        }
        Cart cart = fetch(ctx, cartId);
        if (cart.getItems() != null) {
            for (CartItemInterface item : cart.getItems()) {
                cart = removeItem(ctx, cartId, item.getUid().toString());
            }
        }
        return DtoMapper.cart(mapper, cart);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd bundles/mcp && mvn -q -Dtest=ClearCartToolTest test`
Expected: PASS (2 tests)

- [ ] **Step 5: Format, run the full module test suite, and commit**

```bash
cd bundles/mcp && mvn -Pformat-code clean compile
mvn test
git add bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/ClearCartTool.java bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/ClearCartToolTest.java
git commit -m "feat(mcp): add clear_cart tool"
```

Expected: full `bundles/mcp` test suite passes, formatter makes no further code changes beyond import ordering.

---

## After this plan

Per the design spec's open items — once these tools run against a real AEM + Magento instance:
- Verify actual field names/shapes match what's written here (money flattening, error message wording for out-of-stock/invalid-cart cases) and adjust `DtoMapper.cart()`/tool code as needed. This is expected and fine — the spec explicitly deferred exact verification to real testing.
- Confirm the `Store` header and any other context headers survive the `CartMutationClient` header-copying path correctly (test against a multi-store setup if available).
