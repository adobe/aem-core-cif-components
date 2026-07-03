# CIF Shopper MCP — Configurable Product Cart Support — Design

**Date:** 2026-07-03
**Status:** Shipped and verified live 2026-07-03 (see `bundles/mcp/docs/superpowers/plans/2026-07-03-cif-shopper-configurable-cart.md` for live verification results — no bugs found, everything worked as designed on the first try)
**Scope:** Extend `add_to_cart` to support configurable products (size/color/etc.), in addition to the simple products already supported. Phase 2 of the roadmap tracked in project memory `project_mcp_cart_checkout_flow`.

---

## 1. Goal

Today, `add_to_cart` only works for simple products — a configurable product SKU (e.g. a skirt with size/color) fails with Magento's generic "You need to choose options for your item." This phase lets an agent add a configurable product by specifying human-readable option values (e.g. `{"color": "Blue", "size": "M"}`), without needing to know Magento's internal option-value IDs.

### Non-goals (this phase)

- Bundle products — separate later phase.
- Order creation/checkout — separate later phase.
- Listing all available option combinations up front (e.g. a "get product options" tool) — the error message on a missing/invalid option is enough for an agent to retry correctly; a dedicated discovery tool can be added later if needed.

---

## 2. Decisions

| Decision | Choice | Why |
|---|---|---|
| Mutation | Switch from `addSimpleProductsToCart` to the unified `addProductsToCart` mutation for **all** products (simple and configurable use the same code path) | `addProductsToCart` (confirmed present in `magento-graphql` 9.1.0 via `MutationQuery.addProductsToCart(cartId, List<CartItemInput>, outputDef)`) already accepts `CartItemInput.selectedOptions` (a `List<ID>` of option-value UIDs). One mutation instead of branching by product type — simpler, and simple products just pass an empty/omitted `selectedOptions`. |
| Option input shape | `options` argument on `add_to_cart`: a JSON object mapping attribute code/label to a human-readable value, e.g. `{"color": "Blue", "size": "M"}` | Caller is an LLM agent, not a human UI — human-readable labels are what an agent naturally has (from search results, product descriptions), not Magento's opaque base64 option-value IDs. |
| Option resolution | Server-side: fetch the product's `configurable_options` (via `ConfigurableProduct.configurableOptions` — attribute code, label, and each value's label + UID) and match case-insensitively against the supplied `options` map | Keeps the opaque-ID problem entirely server-side; the agent never sees or needs to handle Magento's internal IDs. |
| Error handling | If a configurable product is added with no/incomplete/invalid `options`, throw a clear error listing the actual attribute names and available values (e.g. `"color must be one of: Blue, Red, Black"`) instead of passing through Magento's generic message | Direct improvement over today's behavior (confirmed live: generic "You need to choose options for your item." is unhelpful for an agent to act on). |

---

## 3. Tool change

**`add_to_cart`** (modified, not a new tool):
- Input: `{ sku, quantity, cart_id? (existing), options? (new, object of string→string) }`
- Behavior: fetch the product's configurable options first (needed regardless, to know if the product requires options and what they are); if `options` were supplied, resolve to UIDs and validate completeness; call `addProductsToCart` with `CartItemInput{ sku, quantity, selectedOptions }`.
- Output: unchanged shape (`DtoMapper.cart(...)`).

Reuses the existing `McpProductRetriever` (already used elsewhere for product lookups) — extend its query-customization hook to also request `configurable_options` when needed, rather than adding a second retriever class.

---

## 4. Testing

- Unit tests (mocked): simple product (no options needed) still works unchanged; configurable product with correct options resolves UIDs and adds successfully; configurable product with missing/wrong options throws the descriptive error.
- Live test against the running AEM+Magento instance (same pattern as Phase 1): find a real configurable product (the Agatha Skirt, `VSK05`, already confirmed configurable), discover its actual option attribute codes/values, and drive `add_to_cart` with correct options end-to-end. Adjust this spec/the implementation afterward if real option attribute codes or value matching don't behave exactly as assumed (e.g. exact casing, whether Magento's attribute code is literally `"color"`/`"size"` or something else like `"fashion_color"`).

---

## 5. Open items — resolved via live testing (2026-07-03)

- Exact attribute codes: confirmed `fashion_color` (label "Fashion Color") and `fashion_size` (label "Fashion Size") for `VSK05`, matching the earlier guess.
- Option-key matching: implemented to accept either the attribute code (`fashion_color`) or the label (`Fashion Color`), case-insensitively; live testing used the attribute code and it worked correctly. See the implementation plan's "Live verification results" section for full detail.
