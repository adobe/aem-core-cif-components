# CIF Shopper MCP — Guest Cart Tools — Design

**Date:** 2026-07-03
**Status:** Draft (pending final field-name verification during implementation)
**Scope:** Add guest-cart tools to the existing `cif-shopper` MCP endpoint, filling in part of the "v2" cart/checkout scope the original design (`2026-07-02-cif-commerce-mcp-design.md`) deliberately deferred.

---

## 1. Goal

Let an MCP agent add products to a cart, view cart contents, change quantities/remove items, and clear a cart — as an anonymous guest, exactly like an unauthenticated storefront visitor.

### Non-goals (this phase)

- **No customer login.** No `generateCustomerToken`, no authenticated/customer-owned cart. Deferred to a later phase.
- **No shipping address, payment method, or order placement.** Deferred to a later phase, once guest cart is proven out.
- **No coupon/gift-card/reward-points application.** Not part of the requested flow.
- **No `cif-authoring` changes.** These tools are shopper-only.

---

## 2. Decisions locked

| Decision | Choice | Why |
|---|---|---|
| Endpoint | `cif-shopper` only (anonymous) | Matches real storefront guest-cart parity; the v1 spec already anticipated cart tools living here. |
| `writesContent()` | `false` for all 4 tools | This flag exists specifically to keep **JCR content** writes off the anonymous endpoint. Cart mutations write to the remote commerce backend, not AEM content — they are not what that guard protects against, and must stay visible on the shopper servlet. |
| Package placement | Flat, inside existing `internal/tools/` | Matches the bundle's existing convention — no per-feature subpackages. All current tools (read and write) live directly in `internal/tools/`, e.g. `BrowseCategoriesTool.java`, `ConfigureProductComponentTool.java`. Cart tools follow the same pattern. |
| DTO mapping | New methods on the existing `internal/dto/DtoMapper.java` | The bundle uses one shared mapper, not per-domain mappers. |
| Execution layer | Call `MagentoGraphqlClient.execute(...)` directly from each tool, using `magento-graphql` `Mutation`/`Query` builders | No `Abstract*Retriever`-equivalent exists for mutations (none of the existing reuse abstractions cover writes). Building one in `bundles/core` for a single consumer (MCP) would be premature abstraction — nothing else in this codebase does server-side cart operations; the real storefront's React/Peregrine cart talks to GraphQL directly. All new code stays inside `bundles/mcp`. |
| Cart-id threading | Client-threaded, as a plain JSON tool argument (`cart_id`), not a header | Matches the v1 spec's already-locked language ("cart state carried as a client-threaded cart-id token"). A guest cart ID is not a secret — Magento itself treats knowledge of the ID as sufficient to act on the cart, the same way an anonymous storefront session does. |
| Cart auto-creation | `add_to_cart` auto-creates a guest cart via `createEmptyCart` when no `cart_id` is supplied | One tool call to start shopping; avoids a mandatory extra round trip through a separate `create_cart` tool. |
| Field naming | Mirror Magento's own GraphQL schema field names wherever there's a 1:1 mapping (`uid`, `sku`, `quantity`, `totalQuantity`); flatten only where the existing `DtoMapper` already sets that precedent (e.g. `price`/`currency` flattened from a nested `Money` type, matching how `DtoMapper.product()` already flattens `Price`) | Avoids inventing synonyms (`cart_item_uid`, `subtotal`, `item_count`) that don't exist in Magento's schema and would create a translation-error surface. Verified against `magento-graphql` 9.1.0 via `javap` (see §4). |

---

## 3. New tools

All four added to `internal/tools/`, all `writesContent() == false`, all on `cif-shopper`.

### `AddToCartTool` (`add_to_cart`)

- **Input:** `{ sku: string (required), quantity: integer (required, ≥1), cart_id: string (optional) }`
- **Behavior:**
  1. If `cart_id` omitted → call `createEmptyCart` mutation first.
  2. Call `addSimpleProductsToCart` with `AddSimpleProductsToCartInput(cartId, [CartItemInput(quantity, sku)])`.
- **Output:** `{ cart_id, items: [{ uid, sku, name, quantity, price, currency, rowTotal }], grandTotal, currency, totalQuantity }`

### `ViewCartTool` (`view_cart`)

- **Input:** `{ cart_id: string (required) }`
- **Behavior:** `CartQuery` by id, requesting `items`, `prices`, `totalQuantity`.
- **Output:** same shape as `add_to_cart`'s output (full current cart state).

### `UpdateCartItemTool` (`update_cart_item`)

- **Input:** `{ cart_id: string (required), uid: string (required), quantity: integer (required, ≥0) }`
  - `uid` is `CartItemInterface.uid` (the cart line identifier), obtained from a prior `add_to_cart`/`view_cart` response — not the SKU, since the same SKU could appear as more than one line.
- **Behavior:** `quantity ≥ 1` → `updateCartItems` mutation; `quantity == 0` → `removeItemFromCart` mutation.
- **Output:** same cart-state shape (post-mutation).

### `ClearCartTool` (`clear_cart`)

- **Input:** `{ cart_id: string (required) }`
- **Behavior:** No native "empty cart" mutation exists in the Magento schema (verified via `javap` against `MutationQuery` — no `EmptyCart`/`ClearCart` entry point). Composite operation: `CartQuery` to fetch current item `uid`s, then `removeItemFromCart` once per line item. Stops and reports on the first failed removal — no silent partial clears.
- **Output:** same shape, `items: []`, `totalQuantity: 0`.

---

## 4. Field-name verification (magento-graphql 9.1.0)

Verified via `javap` against `magento-graphql-9.1.0-magento242ee.jar` (the version this repo's `all`/`core` build resolves):

- Mutations confirmed present: `createEmptyCart`, `addSimpleProductsToCart`, `updateCartItems`, `removeItemFromCart` (all exposed as `MutationQuery$*Arguments` inner classes).
- `CartQuery` fields: `id()`, `items(...)`, `prices(...)`, `totalQuantity()`.
- `CartItemInterfaceQuery` fields: `uid()`, `product(...)`, `quantity()`, `prices(...)`.
- `CartItemPricesQuery` fields: `price(...)`, `rowTotal(...)` (both `MoneyQueryDefinition`).
- `CartPricesQuery` fields: `grandTotal(...)`, `subtotalExcludingTax(...)`, `subtotalIncludingTax(...)`.
- `MoneyQuery` fields: `value()`, `currency()`.
- Mutation input types (`AddSimpleProductsToCartInput`, `UpdateCartItemsInput`) confirm the argument name is `cartId` (Java) / `cart_id` (wire) and `CartItemInput` confirms `sku`/`quantity` — matching the tool input names chosen in §3.

No native cart-clearing mutation exists — confirmed by absence of any `EmptyCart`/`ClearCart`/`RemoveAllItems`-style entry on `MutationQuery`.

---

## 5. Error handling

- Any GraphQL-level error or populated `user_errors` on a cart mutation payload → mapped to a JSON-RPC tool error (existing `-32000`-range application error convention), passing Magento's own `message` through as-is (these are already customer-safe, since Magento surfaces them directly on the real storefront).
- Invalid/expired `cart_id` → tool error, not a crash. Recovery path: call `add_to_cart` again without `cart_id` to get a fresh guest cart.
- Input schema validation (missing `sku`, non-positive `quantity` on `add_to_cart`, etc.) rejected before dispatch, same as existing tools.
- `clear_cart`'s removal loop stops and reports on the first failed `removeItemFromCart` call rather than continuing and silently leaving a partially-cleared cart.

---

## 6. Testing

Following the existing per-tool `aem-mock` unit test convention (mock `MagentoGraphqlClient`, assert query construction + response mapping):

- Each of the 4 tools: happy path (schema in → correct mutation/query built → correct compact DTO out) and a simulated `user_errors` path → correct tool error surfaced.
- `add_to_cart`: both branches — no `cart_id` supplied (asserts `createEmptyCart` is called first) vs. `cart_id` supplied (asserts it's skipped).
- `update_cart_item`: both branches — `quantity ≥ 1` routes to `updateCartItems`; `quantity == 0` routes to `removeItemFromCart`.
- `tools/list` on the shopper servlet includes all 4 new tools; explicitly assert they are **not** excluded by the `writesContent()` filter despite being mutations (regression guard against a future implementer flipping that flag by habit, since every other mutating tool in the bundle today is authoring-only).

---

## 7. Open items to resolve during implementation

- Exact final DTO field names/shapes are provisional pending a live test against a running Magento instance (per user: "some what look right, after testing we can make it more correct").
- Whether `rowTotal`/`grandTotal` money fields should be flattened to bare numbers (as `price`/`currency` already are in `DtoMapper.product()`) or kept as nested `{value, currency}` — decide once real response shapes are seen in testing.
- Out-of-stock / saleable-quantity error message wording from Magento — confirm it reads sensibly when passed through verbatim to an agent.

---

## 8. Explicitly deferred (future phases, not this spec)

- Customer login (`generateCustomerToken`) and authenticated/customer-owned carts.
- Shipping address, shipping method, payment method, `placeOrder`.
- Coupon / gift card / reward points application to cart.
