# CIF Shopper MCP — Bundle Products + Checkout — Design, Plan & Live Verification

**Date:** 2026-07-03
**Status:** Shipped and fully verified live, including `place_order` (see "Live verification" below) — real order `000000029` was created on 2026-07-03 by explicit user request, after initially deferring that one step

## Goal

Two additions to the cif-shopper cart/checkout roadmap:
1. Bundle product support in `add_to_cart` (Phase 2b).
2. Full checkout: shipping address, shipping method, payment method, order placement (Phase 3).

## Decisions (including one reversed mid-implementation by live testing)

| Decision | Choice | Why |
|---|---|---|
| Bundle product mutation | **Reuse the same unified `addProductsToCart` mutation** already used for simple/configurable products — no separate `addBundleProductsToCart` call | Originally implemented with the dedicated `addBundleProductsToCart` mutation and `BundleOptionInput`, since it has an entirely different input shape. **Live testing overturned this**: a bundle choice's own `uid` field (e.g. `bundle/2/2/1`, base64-encoded) works exactly like a configurable option-value UID in `CartItemInput.selectedOptions`. Confirmed by directly calling `addProductsToCart` with a bundle SKU and its option UIDs — it succeeded with no errors. This removed an entire redundant code path (`addBundleItem`, `BundleProductCartItemInput`, `AddBundleProductsToCartInput`) that was implemented and unit-tested before this was caught. |
| Bundle option resolution | `BundleOptionResolver`, same shape/pattern as `ConfigurableOptionResolver` — human-readable `{"Necklace": "Carmina Necklace"}` matched against the bundle item's `title` (case-insensitive) and each choice's `label`, resolving to that choice's `uid` | Mirrors the already-approved configurable-product UX (agent supplies labels, not opaque IDs). Kept as a separate small class rather than merged into `ConfigurableOptionResolver` — the underlying `magento-graphql` types (`BundleItem`/`BundleItemOption` vs. `ConfigurableProductOptions`/`ConfigurableProductOptionsValues`) are unrelated Java classes with no shared interface, so unifying them into one class would need type-specific branching internally anyway, with no real simplification. Both resolvers now share the exact same *output* type (`List<ID>`), and `AddToCartTool.call()` just concatenates both resolvers' results into one `selectedOptions` list before the single `addItem()` call — no `instanceof BundleProduct` branching needed in the tool itself. |
| Checkout tool granularity | 4 tools mirroring the real checkout wizard: `set_shipping_address` → `set_shipping_method` → `set_payment_method` → `place_order`, each returning what's valid for the next step (shipping methods → payment methods → ready-to-place-order) | Matches how a real storefront checkout works and removes guesswork for the agent — it never has to know a `carrier_code`/`method_code`/payment `code` without first seeing it returned by the previous step. |
| Billing address | Always defaults to `sameAsShipping: true` (Magento's own `BillingAddressInput.sameAsShipping` flag) — no separate billing-address input in this phase | YAGNI: this is a guest-checkout MCP flow, not a full storefront; a separate billing address is an edge case not worth the extra tool argument surface until asked for. |
| Payment method | No new payment integration — this store has exactly one configured payment method, `checkmo` (Check / Money order), confirmed live via `available_payment_methods` | Matches the earlier decision that checkout payment should be cash/COD-style with no real gateway integration. Nothing needed to be built for this — it's just what's already configured. |
| `place_order` live testing | Initially deferred to unit tests only, then **executed live by explicit follow-up request** — order `000000029` was created | `place_order` creates a real, non-reversible order on the shared demo backend (`mcprod.catalogservice-commerce.fun`), unlike every other cart/checkout mutation which only touches an ephemeral guest cart — this is why it was gated behind a separate, explicit go-ahead rather than being run automatically with the rest of the live verification pass. |

## Why resolvers exist at all (not a DtoMapper concern)

`DtoMapper` maps an already-fetched GraphQL response into a compact output DTO — it runs *after* a query/mutation succeeds. `ConfigurableOptionResolver`/`BundleOptionResolver` solve a different, *input-side* problem: Magento's `selectedOptions` field requires opaque UIDs (e.g. `bundle/2/2/1`) that no agent could plausibly know or construct — they only exist by querying the specific product's options first. Something has to translate a human-supplied label into that UID *before* the mutation call can even be built. That translation is what the resolvers do; `DtoMapper` has no role here since there's no GraphQL response to format yet at that point in the flow.

## New tools

**`add_to_cart`** (extended, not new): now accepts `bundle_options` in addition to the existing `options`, e.g. `{"sku": "VA24", "quantity": 1, "bundle_options": {"Necklace": "Carmina Necklace", "Bangles": "Gold Omni Bangle Set"}}`. Missing/invalid bundle selections get the same descriptive-error treatment as configurable options (naming the item title and its real available values).

**`set_shipping_address`** — `{cart_id, email, firstname, lastname, street, city, region, postcode, country_code, telephone, confirm?}` → unconfirmed: `{cart_id, confirmed: false, pending_shipping_address: {...}, message}`; confirmed (`confirm: true`): `{cart_id, confirmed: true, shipping_methods: [{carrier_code, carrier_title, method_code, method_title, price, currency}]}`. Internally calls `setGuestEmailOnCart`, `setShippingAddressesOnCart`, and `setBillingAddressOnCart` (same-as-shipping) — only once confirmed.

**`set_shipping_method`** — `{cart_id, carrier_code, method_code, confirm?}` → unconfirmed: `{cart_id, confirmed: false, pending_shipping_method: {carrier_code, method_code}, message}`; confirmed: `{cart_id, confirmed: true, payment_methods: [{code, title}]}`.

**`set_payment_method`** — `{cart_id, payment_method, confirm?}` → unconfirmed: `{cart_id, confirmed: false, pending_payment_method, message}`; confirmed: `{cart_id, confirmed: true, payment_method, ready_to_place_order: true}`.

**`place_order`** — `{cart_id}` → `{order_number}`. Not idempotent, not reversible. No `confirm` argument — calling it *is* the final confirmation.

## Confirm-before-commit gate (added 2026-07-03, after initial checkout ship)

**Why:** the checkout tools initially committed immediately on every call, same as the cart-edit tools. User pointed out that address/shipping-method/payment-method choices feed into a real order and should require an explicit customer-facing confirmation step before committing — unlike cart edits (`add_to_cart`, `update_cart_item`, `clear_cart`), which are cheap to undo and don't need this.

**Design:** `set_shipping_address`, `set_shipping_method`, `set_payment_method` each gained an optional `confirm` boolean (default `false`). Required-field validation still runs unconditionally; only the actual `CartMutationClient` call is gated behind `confirm`. Without it, the tool returns a `pending_*` object echoing back exactly what was received, `confirmed: false`, and a `message` instructing the caller to re-call with `confirm: true`. This lets whatever is calling the tool (an LLM agent, or a human via curl) show the pending choice to the customer and only proceed once they've actually agreed — a real gate, not just documentation telling the caller to be careful.

`add_to_cart`/`view_cart`/`update_cart_item`/`clear_cart` were deliberately left unchanged — no `confirm` argument. `place_order` was also deliberately left unchanged — it's already the single, final, explicit action; adding a `confirm` flag to it would just add a redundant extra step in front of an already-explicit call.

**Live verification:** deployed and drove the full flow through all three preview/confirm pairs on a real cart — each `confirm`-omitted call returned a `pending_*` preview with `confirmed: false` and made no backend mutation (verified: the subsequent `confirm: true` call against the same cart still worked and returned the real shipping/payment methods, meaning nothing had been silently committed by the preview call). Each `confirm: true` call then committed correctly and returned the same real data as before this change. No regressions: full `bundles/mcp` suite at 74/74 passing.

## Live verification (2026-07-03)

Deployed to the running AEM instance and drove real GraphQL calls end-to-end:

- `add_to_cart` on `VA24` ("Night Out Collection" bundle) with `{"Necklace": "Carmina Necklace", "Bangles": "Gold Omni Bangle Set"}` → succeeded, correct cart DTO (`$156.00`, matching the real bundle price).
- `add_to_cart` on `VA24` with no `bundle_options` → `"Necklace is required. Available values: Carmina Necklace, Augusta Necklace"` — descriptive error, matches the configurable-product error UX exactly.
- `set_shipping_address` with a real US address → returned the actual configured shipping method (`flatrate`/`flatrate`, $5.00).
- `set_shipping_method` with that method → returned the actual configured payment method (`checkmo`, "Check / Money order").
- `set_payment_method` with `checkmo` → `ready_to_place_order: true`.
- `place_order` — **executed live by explicit follow-up request**, one step at a time (add_to_cart → set_shipping_address → set_shipping_method → set_payment_method → place_order), each result shown before proceeding to the next. Returned real order number `000000029`. Verified genuine (not a mocked/fake response) by calling `view_cart` on the same `cart_id` immediately after: it returned `"The cart isn't active."` — Magento's real behavior once a guest cart converts to a placed order, which cannot happen without an actual order being created.

Full `bundles/mcp` test suite: 71/71 passing after all changes (`ConfigurableOptionResolver`, `BundleOptionResolver`, `AddToCartTool` extension, 4 new checkout tools, and their tests).

## Post-implementation code review (2026-07-03, Phase 2b+3)

Ran a `--level high` code-review pass (8 finder angles, verified) across all uncommitted Phase 2b+3 work, per user request to match established conventions and catch issues before committing. Found and fixed:

- **Important — `AddToCartTool` fractional-quantity truncation.** Same bug class as the earlier `UpdateCartItemTool` fix (see AGENTS.md "Known pitfalls"), missed here: `{"quantity": 1.9}` truncated via `.asInt()` to `1`, passed the `>= 1` check, and silently added quantity `1` instead of being rejected. Fixed with the same `quantityNode.isIntegralNumber()` guard, plus a new test (`rejectsFractionalQuantityInsteadOfSilentlyTruncating`).
- **Minor — `SetPaymentMethodTool` cart_id inconsistency.** Re-derived `cart_id` from the mutation response (`cart.getId()`) instead of echoing the local `cartId` variable like `SetShippingAddressTool`/`SetShippingMethodTool` do. Normalized to `out.put("cart_id", cartId)` and dropped the now-unnecessary `.id()` field from the mutation's cart selection.

Both fixes verified live against the running AEM instance (fractional quantity rejected with a clear error; full checkout flow re-driven end-to-end through `set_shipping_address` → `set_shipping_method` → `set_payment_method`, confirming `cart_id` echoes correctly). Full `bundles/mcp` suite: 75/75 passing. Full green gate (`mvn -pl bundles/mcp clean install`, includes formatter/impsort/macker/apache-rat) clean.

## Explicitly deferred (not this round)

- **Customer login / authenticated (non-guest) checkout — declined on security grounds, not just postponed.** Magento's `generateCustomerToken` mutation takes a raw email+password. Exposed as an MCP tool, the password would flow through the calling LLM agent's conversation context and tool-call arguments — the same exposure surface every other tool argument has, but this is the first credential rather than commerce data. The standard mitigation (exchange the password for a token at login, thread only the token through subsequent calls, same pattern as `cart_id`) reduces blast radius (a token is scoped/revocable, a password isn't) but doesn't eliminate the exposure — the token still passes through the same conversation/tool-call surface. Fully avoiding this needs a different auth flow entirely (e.g. real OAuth/browser redirect where the LLM never sees the credential), which Magento's default customer auth doesn't support and is a materially bigger build. User weighed this trade-off and chose to keep everything guest-only rather than accept the risk.
- Separate billing address input.
- Any payment method beyond what's already configured (`checkmo`) — no gateway integration.
