# CIF Commerce MCP Server (`core-cif-components-mcp`)

An OSGi bundle that turns a CIF-backed AEM instance into a
[Model Context Protocol](https://modelcontextprotocol.io) (MCP) server, so LLM/agent
tools can work against commerce data through well-described tools. It reuses CIF Core
Components' existing GraphQL stack (`SearchResultsService`, `SearchFilterService`, the
public `Abstract*Retriever` classes, `MagentoGraphqlClient`, `UrlProvider`) rather than
reimplementing any of it.

The MCP layer is a hand-rolled JSON-RPC 2.0 subset served over Sling servlets — no
external MCP SDK, Jackson only. Protocol version: **`2025-06-18`**.

## Endpoints

The server is **mounted on the CIF nav-root (store-root) page** — no dedicated content
nodes. Two servlets bind `cq:Page` + `POST` + extension `json`, distinguished by selector,
and each gates on the nav-root via `SiteStructure` (a request to a non-nav-root page
returns `404`).

| | Selector / URL | Tools | Instances | Auth |
|---|---|---|---|---|
| **Shopper (read)** | `POST <navRoot>.mcp.json` | read kernel | author **+** publish | anonymous (storefront parity) |
| **Authoring (read+write)** | `POST <navRoot>.mcp-authoring.json` | read kernel **+** write tools | **author only** | AEM auth + JCR ACLs |

`<navRoot>` is the store-root page CIF marks `navRoot=true`, e.g.
`/content/venia/us/en`. Different nav-roots = different stores; the store/commerce context
is derived from the endpoint's own page.

The authoring servlet is `configurationPolicy = REQUIRE` and its OSGi config lives under
`config.author`, so it is **structurally absent on publish** (write tools are physically not
registered there, not merely filtered).

## Bundle structure

```
bundles/mcp/
├── pom.xml                     # mirrors bundles/core build (JDK 11, Java 8 target, bnd, formatter, impsort, macker)
├── macker-rules.xml            # enforces the exported-API vs internal package split
└── src/main/java/com/adobe/cq/commerce/mcp/
    ├── JsonRpc.java            # (exported API) JSON-RPC 2.0 envelope + error codes
    ├── McpTool.java            # (exported API) tool SPI: name/description/inputSchema/writesContent/call
    ├── McpCallContext.java     # (exported API) per-call context handed to tools
    └── internal/
        ├── ToolRegistry.java           # collects McpTool services; per-selector visibility
        ├── JsonRpcDispatcher.java      # initialize / tools.list / tools.call routing
        ├── StoreContext.java           # McpCallContext + MagentoGraphqlClient for the request
        ├── StoreContextResolver.java   # nav-root gate + store-context resolution
        ├── McpSearchOptions.java       # SearchOptions built from tool args
        ├── servlets/
        │   ├── AbstractMcpServlet.java # transport: body-limit, nav-root gate, JSON-RPC I/O
        │   ├── ShopperMcpServlet.java  # selector "mcp"           (OPTIONAL policy)
        │   └── AuthoringMcpServlet.java# selector "mcp-authoring" (REQUIRE policy → author-only)
        ├── dto/DtoMapper.java          # compact product/category/cart DTOs (incl. PDP/PLP links)
        └── tools/                      # the McpTool implementations (below), plus:
            ├── CartMutationClient.java         # shared cart-mutation executor + field selection (see "Cart tools" below)
            ├── ConfigurableOptionResolver.java  # resolves human-readable configurable options to option-value UIDs
            └── BundleOptionResolver.java        # resolves human-readable bundle selections to the same kind of UID
```

Related config outside this module:
- `ui.config/src/content/jcr_root/apps/core/cif/config.author/…AuthoringMcpServlet.cfg.json` — author-only activation.
- `all/pom.xml` embeds this bundle at `jcr_root/apps/core/cif/install/`, alongside `core-cif-components-core`.

## Tools

Read tools (both endpoints); each result is a compact JSON DTO with a matching
`structuredContent`, never raw GraphQL:

| Tool | Args | Result |
|---|---|---|
| `search_products` | `{query?, page?, pageSize?, filters?}` | `{total, items:[{sku,name,slug,url,imageUrl,imageAlt,price,currency}]}` (`url` = PDP link) |
| `get_product` | `{sku}` | `{sku,name,urlKey}` |
| `get_product_variants` | `{sku}` | `{sku,name,urlKey,configurable,options:[…],variants:[…]}` |
| `get_product_associated_content` | `{sku, fragmentLocation?, contentFragmentModel?, linkElement?, limit?}` | `{sku, experienceFragments:[…], contentFragments:[…], contentPages:[…], assets:[…]}` |
| `get_category_associated_content` | `{categoryUid, fragmentLocation?, contentFragmentModel?, linkElement?, limit?}` | `{categoryUid, experienceFragments:[…], contentFragments:[…], contentPages:[…], assets:[…]}` |
| `browse_categories` | `{uid?}` | `{category:{uid,name,urlPath,url,children:[…]}}` (`url` = PLP link, on category + children) |
| `get_attributes` | `{}` | `{attributes:[{code,inputType}]}` |
| `resolve_picker_selection` | `{skus:[…]}` | `{items:[{sku,name}]}` (authoring picker helper; read-only) |
| `get_sort_options` | `{}` | `{default, options:[{value,label}]}` (store's product-listing sort fields) |
| `get_product_relationships` | `{sku, linkType?}` | `{sku, links:[{linkType,sku,linkedProductSku,linkedProductType,position}]}` (`linkType` filters to `related`/`upsell`/`crosssell`) |
| `get_category_breadcrumbs` | `{uid?, urlPath?}` (one required) | `{uid, breadcrumbs:[{uid,name,level,urlPath}]}` (top-level ancestor first) |
| `resolve_category_details` | `{uid, urlPath?}` | `{uid, name, urlPath, breadcrumbs:[…]}`, or `{uid, resolves:false}` if the category no longer resolves |
| `get_custom_attribute_metadata` | `{attributes:[{code, entityType?}]}` (`entityType` defaults to `catalog_product`) | `{items:[{code,attributeType,inputType,entityType,options:[{label,value}]}]}` |
| `resolve_url_to_entity` | `{url}` | `{url, type, id, uid, canonicalUrl, relativeUrl, redirectCode}`, or `{url, resolves:false}` if the URL doesn't resolve |
| `validate_content_bindings` | `{products?:[sku…], categories?:[uid…]}` (at least one non-empty) | `{products:[{sku,resolves}], categories:[{uid,resolves}]}` |
| `find_orphaned_commerce_content` | `{root?, limit?}` (`root` defaults to `/content`, must be `/content` or under it; `limit` defaults to `200`) | `{root, scanned, orphans:[{path,identifier,identifierType,property}]}` (reverse scan for `cq:products`/`cq:categories` tags whose SKU/UID no longer resolves) |
| `get_commerce_content_fragment` | `{identifier, type}` (`type` is `product`\|`category`) + optional `{contentFragmentModel?, linkElement?}` | `{identifier, type, modelPath?, fragmentPath?, fields}`, or `{identifier, type, resolves:false}` if no CF matches |
| `list_catalog_pages` | `{siteRoot?}` (any page under `/content`; defaults to the endpoint's nav root) | `{siteRoot, catalogPages:[{path,rootCategoryId,idType,genericFallback}]}` (routing order, generic fallback last) |
| `explain_catalog_page_routing` | `{urlPath, siteRoot?}` | `{identifier, winningPage, reason, candidates:[{path,matched,why}]}` (replays `getGenericPage`'s first-match walk) |
| `detect_catalog_page_conflicts` | `{siteRoot?}` | `{siteRoot, overlaps:[{pages:[…],scope,kind}]}` (`kind` = `duplicate-scope`\|`ancestor-descendant`; structural only, no live category-tree fetch) |
| `explain_page_resolution` | `{identifier, type, siteRoot?}` (`type` is `product`\|`category`) | `{identifier, type, winningPage, depth, candidates:[{path,depth,matched,why}]}` (replays the deepest-wins specific-page match) |
| `list_specific_pages` | `{siteRoot?}` | `{siteRoot, specificPages:[{path,pageType,selectorFilter?,selectorFilterType?,includesSubCategories?,useForCategories?}]}` |
| `detect_specific_page_conflicts` | `{siteRoot?}` | `{siteRoot, duplicates:[{scope,pages:[…]}], shadowing:[{broader,narrower,reason}]}` (structural only; url-path-scoped bindings only) |
| `validate_selector_filter_format` | `{path}` | `{path, selectorFilterType, entries:[{raw,valid,uid?,urlPath?,issue?}]}` (flags entries missing the `uid\|urlPath` separator) |
| `check_specific_page_capability` | `{path}` | `{path, pageType, componentVersion, fields:{selectorFilter,selectorFilterType,includesSubCategories,useForCategories}}` (booleans; `useForCategories`/product-page `includesSubCategories` are `false` pre-v2) |
| `suggest_template_for_page_type` | `{kind}` (`kind` is `product`\|`category`\|`catalog`) | `{kind, templates:[{path,title,signal}]}` (`signal` = `resourceSuperType`\|`title`) |

`get`/`list`-style tools above return empty collections for an identifier that no longer
resolves; `resolve_category_details`/`resolve_url_to_entity` instead return an explicit
`resolves:false`, and `validate_content_bindings` reports `resolves:false` per entry.
`list_catalog_pages`/`explain_catalog_page_routing`/`detect_catalog_page_conflicts`/
`explain_page_resolution`/`list_specific_pages`/`detect_specific_page_conflicts` all take an
optional `siteRoot` (any page under `/content`; defaults to the endpoint's own nav root), while
`validate_selector_filter_format`/`check_specific_page_capability` take a required `path` that
must resolve to a `cq:Page` under `/content`.

### Cart tools (shopper endpoint — guest cart, `writesContent() == false`)

These mutate the **remote Magento cart**, not AEM content, so they stay on the anonymous
shopper endpoint (same as an anonymous storefront visitor adding to cart) — do not confuse
them with the write tools below, which mutate JCR content and are authoring-only. Cart state
is client-threaded: `add_to_cart` returns a `cart_id`; pass it to every other cart tool.

| Tool | Args | Result |
|---|---|---|
| `add_to_cart` | `{sku, quantity, cart_id?, options?, bundle_options?}` | cart DTO (below). Creates a guest cart if `cart_id` omitted. `options`/`bundle_options` select a configurable/bundle product's variant — see below. |
| `view_cart` | `{cart_id}` | cart DTO |
| `update_cart_item` | `{cart_id, uid, quantity}` | cart DTO. `quantity: 0` removes the line item. |
| `clear_cart` | `{cart_id}` | cart DTO with `items: []` |

Cart DTO shape: `{cart_id, items:[{uid,sku,name,quantity,price,currency,rowTotal}], grandTotal, currency, totalQuantity}`.

**Configurable products** (size/color variants): pass human-readable option values in
`add_to_cart`'s `options` argument, keyed by attribute code or label (case-insensitive), e.g.
`{"fashion_color": "Peach", "fashion_size": "M"}` — not Magento's internal option-value IDs. If
an option is missing or invalid, the error names the real attribute and its available values
(e.g. `"Fashion Color must be one of: Peach, Khaki, Lilac, Rain"`), not Magento's generic "You
need to choose options for your item."

**Bundle products**: pass human-readable selections in `add_to_cart`'s `bundle_options`
argument, keyed by each bundle item's title, e.g. `{"Necklace": "Carmina Necklace"}`. Same
descriptive-error behavior as configurable options for missing/invalid selections.

### Checkout tools (shopper endpoint, guest checkout — `writesContent() == false`)

Mirror a real checkout wizard: each step returns what's valid for the next, so the agent never
has to guess a carrier/method/payment code.

**Confirm-before-commit:** `set_shipping_address`, `set_shipping_method` and `set_payment_method`
all take an optional `confirm` boolean (default `false`). Without `confirm: true`, the tool
commits nothing — it just echoes back what it *would* set (`pending_shipping_address` /
`pending_shipping_method` / `pending_payment_method`) so the caller can review the details with
the customer first. Call the same tool again with `confirm: true` to actually apply it. Every
result includes a `confirmed` boolean so the caller always knows which case it got.
`add_to_cart`/`view_cart`/`update_cart_item`/`clear_cart` don't need this — cart edits are freely
reversible, unlike an address/shipping/payment choice that's about to feed into an order.
`place_order` **also requires `confirm: true`** — since it creates a real, non-reversible order it
must not fire on a single unconfirmed call; without it the tool commits nothing and returns a
`pending_order` preview.

| Tool | Args | Result |
|---|---|---|
| `set_shipping_address` | `{cart_id, email, firstname, lastname, street, city, region, postcode, country_code, telephone, confirm?}` | Unconfirmed: `{cart_id, confirmed:false, pending_shipping_address:{...}, message}`. Confirmed: `{cart_id, confirmed:true, shipping_methods:[{carrier_code,carrier_title,method_code,method_title,price,currency}]}`. Also sets guest email and billing address (defaults to same-as-shipping) once confirmed. |
| `set_shipping_method` | `{cart_id, carrier_code, method_code, confirm?}` | Unconfirmed: `{cart_id, confirmed:false, pending_shipping_method:{carrier_code,method_code}, message}`. Confirmed: `{cart_id, confirmed:true, payment_methods:[{code,title}]}` |
| `set_payment_method` | `{cart_id, payment_method, confirm?}` | Unconfirmed: `{cart_id, confirmed:false, pending_payment_method, message}`. Confirmed: `{cart_id, confirmed:true, payment_method, ready_to_place_order:true}` |
| `place_order` | `{cart_id, confirm?}` | Unconfirmed: `{cart_id, confirmed:false, pending_order:true, message}` (nothing placed). Confirmed: `{cart_id, order_number, confirmed:true}`. **Not idempotent, not reversible** — creates a real order. |

**Not yet supported:** customer login (guest checkout only), a separate billing address, any
payment method beyond what the store already has configured — see "Known limitations" below.

Write tools (authoring endpoint only — `writesContent() == true`). They run under the
**caller's** `ResourceResolver` (ACLs enforced) and **fail closed** on a non-`/content`
path or a resource that is not a CIF component/page they understand:

| Tool | Args | Effect |
|---|---|---|
| `configure_product_component` | `{path, sku}` | pins a CIF product component to a SKU (`selection`/`selectionType`) |
| `configure_productlist_component` | `{path, categoryUid?, showTitle?, showImage?, pageSize?, defaultSortField?, defaultSortOrder?, fragments?}` | pins a CIF product list / carousel **component** to a category (its `category` manual selection) and, for product-list components (`ProductListImpl` extends `ProductCollectionImpl`), the rest of its dialog: `showTitle`/`showImage` (booleans), `pageSize` (integer), `defaultSortField`/`defaultSortOrder` (`asc`\|`desc`), and `fragments` (array of `{fragmentLocation?, fragmentPage?, fragmentCssClass?}`, written as the `fragments/item0…` composite multifield). The productlist-specific properties are no-ops on a product carousel — only `categoryUid` has any effect there |
| `configure_productcarousel_component` | `{path, selectionType?, product?, category?, productCount?}` | configures a CIF product carousel's selection: a manual list of product SKUs (`selectionType=product`, `product[]` combinedSku-normalized) or a category with an optional product count (`selectionType=category`, `category` + `productCount`) |
| `configure_featuredcategorylist_component` | `{path, items, title?, titleType?, linkTarget?}` | configures the `items` composite multifield (`categoryId` + optional `asset` override per item, written as `items/item0…`) shared by CIF's featured-category-list and category-carousel components (same `FeaturedCategoryListImpl` model backs both resource types), plus `jcr:title`/`titleType`/`linkTarget` (no-ops on category-carousel, which doesn't render a title) |
| `configure_catalog_page` | `{path, categoryUid, idType?, showMainCategories?}` | binds a catalog (PLP) **page's** root category (`magentoRootCategoryId` + `magentoRootCategoryIdType`, default `uid`, also accepts `urlPath` + `showMainCategories`, default `false`) |
| `bind_page_to_products` | `{path, skusOrUrlKeys[]}` | binds a CIF structure **product page** as the dedicated PDP for a specific set of products by writing `selectorFilter` (plain SKUs/URL keys/slugs, not pipe-encoded); an empty array clears an existing binding |
| `bind_page_to_category` | `{path, categoryUid, urlPath, includesSubCategories?}` | binds a CIF structure **category page** as the dedicated PLP for a specific category by writing `selectorFilter` (`categoryUid\|urlPath`, pipe-encoded), `selectorFilterType` (`uidAndUrlPath`), and `includesSubCategories` |
| `bind_product_page_to_category_tree` | `{path, categoryUid, urlPath, includesSubCategories?}` | binds a CIF structure **product page** as the dedicated PDP for every product under a whole category/tree by writing `useForCategories` (plain `urlPath`, v2+ only) and `includesSubCategories`; `categoryUid` is required for symmetry with `bind_page_to_category` but is validated only, not persisted |
| `unbind_specific_page` | `{path}` | clears a CIF structure page's specific-PDP/PLP binding by removing `selectorFilter`, `selectorFilterType`, `includesSubCategories`, and `useForCategories` from its `jcr:content`; idempotent |
| `configure_productteaser_component` | `{path, sku, cta?, ctaText?, linkTarget?, id?}` | binds a SKU (base or `base#variant` combinedSku) plus optional CTA/link/id properties to a product teaser component (`selection`, `cta`, `ctaText`, `linkTarget`, `id`) |
| `configure_relatedproducts_component` | `{path, relationType, product?}` | sets the relation type (`RELATED_PRODUCTS`\|`UPSELL_PRODUCTS`\|`CROSS_SELL_PRODUCTS`) and optional plain-SKU product override on a related-products component; omitting `product` removes it and falls back to the page-URL product |
| `configure_product_visible_sections` | `{path, visibleSections[]}` | sets which sections (`title`\|`price`\|`sku`\|`images`\|`options`\|`quantity`\|`actions`\|`description`\|`details`) a product component renders; an empty array clears the override (falls back to the style/policy default). Effective on v3 product components only — accepted but ignored on v1/v2 |
| `configure_page_commerce_links` | `{path, cifProductPage?, cifCategoryPage?, cifSearchResultsPage?}` | sets the nav-config pagefields on a CIF structure page's `jcr:content` so generic product/category/search-results links resolve to the given paths; pass `""` to clear a field. At least one of the three is required. Covers only these pagefields — `cq:products`/`cq:categories` markers are still `tag_content_with_commerce` |
| `tag_content_with_commerce` | `{path, sku?, categoryUid?, action?}` | sets `cq:products` / `cq:categories` on a DAM asset, page, or XF variation (`action`: `add` or `remove`) |
| `create_product_teasers` | `{parentPath, skus[], cta?, ctaText?, dryRun?}` | bulk-creates one product teaser component (`sling:resourceType=…/productteaser/v1/productteaser`) per SKU as a uniquely-named child of `parentPath`, sharing the optional `cta`/`ctaText`; `dryRun:true` previews the node paths/selections without creating anything |
| `create_product_carousels` | `{parentPath, carousels[{selectionType?, product?, category?, productCount?}], dryRun?}` | bulk-creates one product carousel component (`sling:resourceType=…/productcarousel/v1/productcarousel`) per carousel spec as a uniquely-named child of `parentPath`, each spec mirroring `configure_productcarousel_component`'s shape (manual `product[]` or `category`+`productCount`); `dryRun:true` previews the node paths without creating anything |
| `update_commerce_content_fragment_field` | `{fragmentPath, elementName, value, variation?}` | sets one element's value on an existing commerce content fragment under `/content/dam`; richtext elements are written as HTML (`text/html`), scalars by type, multi-value elements accept a JSON array (type-gated). Writes the **master** value by default; `variation` targets an already-existing named variation (unknown → error, never auto-created). Returns `{fragmentPath, elementName, variation, updated}` (`updated` from a real post-commit readback). Never auto-publishes |
| `create_commerce_content_fragment` | `{identifier, type, modelPath, linkElement, fields?, parentPath?, name?, title?, dryRun?}` | creates a new content fragment from `modelPath` (via `FragmentTemplate.createFragment`) under `parentPath` (default `/content/dam`), seeding `linkElement`=identifier + optional `fields` as **scalar strings only** (richtext/multi-value seeds are rejected — use `update_commerce_content_fragment_field` after). `dryRun:true` previews the fragment path + seeded fields without creating anything; never auto-publishes |

> Note the distinction: `category` (a **component** property, read by `ProductListImpl`) vs `magentoRootCategoryId` (a **page** property, read by `SiteStructure`/`NavigationImpl`). Binding a *component* to a category → `configure_productlist_component`; scoping a *catalog page* to a root category → `configure_catalog_page`.

`PDP`/`PLP` links are page-relative paths (as CIF's `UrlProvider` emits them); prepend
scheme/host if you need absolute URLs.

## Usage (raw JSON-RPC)

```bash
NAV=http://localhost:4502/content/venia/us/en

# initialize
curl -s -u admin:admin -X POST -H "Content-Type: application/json" \
  --data '{"jsonrpc":"2.0","id":1,"method":"initialize"}' "$NAV.mcp.json"

# list tools
curl -s -u admin:admin -X POST -H "Content-Type: application/json" \
  --data '{"jsonrpc":"2.0","id":2,"method":"tools/list"}' "$NAV.mcp.json"

# call a tool
curl -s -u admin:admin -X POST -H "Content-Type: application/json" \
  --data '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"search_products","arguments":{"query":"top","pageSize":5}}}' \
  "$NAV.mcp.json"
```

## Setting up in a coding agent

The server speaks the MCP **Streamable HTTP** shape (single JSON-RPC POST, `application/json`
response). Point your agent's HTTP MCP transport at the endpoint URL; add basic auth for the
authoring endpoint.

**Claude Code** (native HTTP MCP support):

```bash
claude mcp add --transport http cif-shopper \
  http://localhost:4502/content/venia/us/en.mcp.json

claude mcp add --transport http cif-authoring \
  http://localhost:4502/content/venia/us/en.mcp-authoring.json \
  --header "Authorization: Basic YWRtaW46YWRtaW4="      # base64("admin:admin")
```

or a project `.mcp.json`:

```json
{
  "mcpServers": {
    "cif-shopper":   { "type": "http", "url": "http://localhost:4502/content/venia/us/en.mcp.json" },
    "cif-authoring": { "type": "http", "url": "http://localhost:4502/content/venia/us/en.mcp-authoring.json",
                       "headers": { "Authorization": "Basic YWRtaW46YWRtaW4=" } }
  }
}
```

**Agents that only speak stdio** (Cursor, Claude Desktop, …): bridge with `mcp-remote`:

```json
{ "mcpServers": { "cif-authoring": {
  "command": "npx",
  "args": ["-y", "mcp-remote", "http://localhost:4502/content/venia/us/en.mcp-authoring.json",
           "--header", "Authorization: Basic YWRtaW46YWRtaW4="] } } }
```

Notes:
- Keep the `.mcp.json` / `.mcp-authoring.json` suffix — it is the Sling selector+extension, part of the endpoint.
- This is a **minimal** transport (no SSE, no session id, `GET` → 405). Spec-compliant clients should work for the tool flow; if a client insists on the SSE/session handshake, use the `mcp-remote` bridge.
- Only put the authoring endpoint (with credentials) in a local/trusted config; it exists on **author** only.

## Extending — add a tool

Implement `McpTool` and register it as an OSGi component; `ToolRegistry` picks it up
automatically:

```java
@Component(service = McpTool.class)
public class MyTool implements McpTool {
    public String name() { return "my_tool"; }
    public String description() { return "…"; }
    public ObjectNode inputSchema() { /* JSON Schema */ }
    // return true to make it an authoring-only WRITE tool (hidden from the shopper endpoint)
    // public boolean writesContent() { return true; }
    public JsonNode call(McpCallContext ctx, JsonNode args) { /* cast ctx to StoreContext */ }
}
```

Write tools **must** validate the target and fail closed (see the existing `Configure*Tool`s).

## Build & deploy

```bash
# build + test (from repo root)
mvn -pl bundles/mcp -am clean install

# deploy to a running author (Felix console) — this repo has no sling-maven deploy profile
curl -s -u admin:admin -F action=install -F bundlestart=start -F bundlestartlevel=20 -F refreshPackages=true \
  -F bundlefile=@bundles/mcp/target/core-cif-components-mcp-*.jar \
  http://localhost:4502/system/console/bundles
```

Confirm the bundle is `Active` at `/system/console/bundles.json` (symbolic name
`com.adobe.commerce.cif.core-cif-components-mcp`). Formatting note: use the repo's
`-Pformat-code` profile (`mvn -pl bundles/mcp -Pformat-code process-classes`) before
committing — the bare `formatter:format` goal ignores the project's Eclipse config.

## Known limitations

- No MCP `resources`/`prompts` surface — tools only.
- Cart tools support simple, configurable, **and bundle** products (see "Bundle products"
  above). Downloadable, virtual, and grouped products are untested.
- Checkout supports guest email/shipping/payment/order placement; no separate billing address
  input (always same-as-shipping) and no payment method beyond what the store already has
  configured.
- No customer login — carts and checkout are guest-only; there is no authenticated/
  customer-owned cart.
- Shopper **commerce-token pass-through** is not yet implemented: the endpoint operates as a
  guest (public catalog data). See `docs/superpowers/specs/2026-07-02-cif-commerce-mcp-design.md`
  (original design) and `project_mcp_cart_checkout_flow` for the phased roadmap: guest cart
  (shipped) → configurable products (shipped) → bundle products (shipped) → checkout/order
  placement (shipped, payment is cash/COD-style via the store's existing `checkmo` method — no
  payment gateway integration).
