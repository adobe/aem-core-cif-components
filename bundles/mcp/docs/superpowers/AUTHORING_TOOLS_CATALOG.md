# CIF Authoring Tools Catalog

**Purpose:** This document consolidates every MCP authoring-tool idea explored during the
2026-07-03 research pass across `aem-core-cif-components`, `cif-on-skyline`,
`commerce-cif-graphql-client`/`commerce-cif-magento-graphql`, and `aem-cif-guides-venia`.
It is the reference basis for writing follow-up implementation plans (in the same
TDD-task format as `plans/2026-07-02-cif-commerce-mcp.md`). It is not
itself an implementation plan — no code, no task breakdown — just a grounded, tiered
tool catalog with **enough technical detail (backing classes, exact JCR properties,
GraphQL binding methods, file paths, and gotchas) that an implementation agent can build
each tool without re-deriving the mechanics**.

Every tool below is traceable to real source discovered during research; nothing here is
speculative "AEM authoring in general" — everything is tied to a CIF-specific mechanism.

**Reconciled with shipped code — 2026-07-03.** This catalog was reconciled against the
tools actually implemented on the `mcp` branch. Several tools originally listed here as
*future work* are already shipped (under the code's own naming conventions), and two
described mechanisms were resolved differently than this catalog first proposed. Those
reconciliation facts are folded in below. **Where a shipped tool and this catalog
disagreed, the catalog adopts the shipped tool** (it is tested and live-verified; this doc
is a plan), *except* where the shipped tool is a deliberate subset — there the catalog
records the remaining scope as follow-up work, not as a catalog error. The authoritative,
always-current tool contract is `bundles/mcp/README.md` + `bundles/mcp/AGENTS.md`; this
catalog is the forward plan layered on top of that reality.

**Status legend** (used on every tool heading and in the §11 index):

- **✅ Shipped** — implemented and unit-tested on the `mcp` branch (has a `*Test.java`).
- **◐ Partial** — a subset of the catalog scope is shipped; the remaining delta is called out inline.
- **▢ Planned** — not yet implemented; this catalog is the spec.

**How to read the per-tool detail.** §0.1 describes the shared substrate (SPI, client
acquisition, read/write patterns) that *every* tool builds on — read it first. Each
group section then gives the mechanism once, followed by per-tool **Backing** (class FQN
+ repo-relative path), **Properties/Fields** (exact identifiers to read/write),
**Implementation** (the concrete calls), and **Gotchas**. Repo-relative paths use these
abbreviations:

| Abbrev | Repo | Prefix it stands for |
|---|---|---|
| `core/…` | `aem-core-cif-components` | `bundles/core/src/main/java/com/adobe/cq/commerce/core/components/` |
| `mcp/…` | `aem-core-cif-components` (branch `mcp`) | `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/` |
| `apps/…` | `aem-core-cif-components` | `ui.apps/src/main/content/jcr_root/apps/core/cif/components/` |
| `mgql.` | `commerce-cif-magento-graphql` | `src/main/java/com/adobe/cq/commerce/magento/graphql/` |
| `skyline/…` | `cif-on-skyline` | `bundles/…/src/main/java/com/adobe/cq/cif/` |
| `venia/…` | `aem-cif-guides-venia` | `ui.content`/`ui.apps` under `src/main/content/jcr_root/` |

Two shared picker value-format facts referenced throughout (from `cifproductfield` =
`commerce/gui/components/common/cifproductfield`, `cifcategoryfield` =
`…/cifcategoryfield`; the written value is governed by the picker's `selectionId`):

- **Product picker** — `selectionId="combinedSku"` writes `baseSku` or `baseSku#variantSku`
  (separator `#`, see `CombinedSku` — `core/.../models/common/CombinedSku.java:34`);
  `selectionId="sku"` writes the plain base SKU; `selectionId="slug"` writes the product slug.
- **Category picker** — default writes the category **UID**; `selectionId="urlPath"` writes the URL path.

---

## 0. Architecture recap (already built, `mcp` branch of `aem-core-cif-components`)

- **Module:** `bundles/mcp`, hand-rolled JSON-RPC 2.0 over two `SlingAllMethodsServlet`s bound
  to `cq:Page`, selectors `mcp` (shopper/read, anonymous) and `mcp-authoring` (read+write,
  author-only).
- **Gating:** `StoreContextResolver.isNavRoot()` — the endpoint only responds on the CIF
  nav-root page (`SiteStructure.getLandingPage()`), via `SiteStructure`.
- **SPI:** `McpTool` (`name()/description()/inputSchema()/writesContent()/call(ctx, args)`),
  collected by `ToolRegistry`, dispatched by `JsonRpcDispatcher`.
- **Constraint:** AEM 6.5 compatible — Java 8 source/target, no AEMaaCS-only APIs, no
  external MCP SDK, Jackson only.

### Already implemented tools (baseline — do not duplicate)

**This is the real shipped set as of 2026-07-03** (each has a matching `*Test.java`). The
selector split is driven purely by `McpTool.writesContent()`: `false` ⇒ visible on both the
shopper `mcp` and authoring `mcp-authoring` endpoints; `true` ⇒ authoring endpoint only.
The rightmost column ties each shipped tool back to the catalog ID it satisfies (so §2–§9
below don't re-plan something that already exists).

**Read tools** (`writesContent()==false` → both endpoints):

| Tool | Args | Does | Catalog ID |
|---|---|---|---|
| `search_products` | `{query?, page?, pageSize?, filters?}` | Keyword + attribute-filtered catalog search | baseline |
| `get_attributes` | `{}` | List filterable product attributes for the store | baseline |
| `get_product` | `{sku}` | Fetch one product by SKU (`{sku,name,urlKey}`) | baseline |
| `get_product_variants` | `{sku}` | Configurable options + variant SKUs for a product | **✅ T-10** |
| `browse_categories` | `{uid?}` | Browse category tree (UID, name, URL path, children) | baseline |
| `get_product_associated_content` | `{sku, fragmentLocation?, contentFragmentModel?, linkElement?, limit?}` | Pages/assets/XFs/CFs referencing a SKU | **✅ T-19** (product) |
| `get_category_associated_content` | `{categoryUid, …same…}` | Same, for a category UID | **✅ T-19** (category) |
| `resolve_picker_selection` | `{skus:[…]}` | Resolve display names for a set of SKUs (authoring picker helper) | baseline |

> **Correction to earlier draft:** `resolve_picker_selection` is `writesContent()==false`,
> so it is served on **both** endpoints, not "authoring-only" as previously noted.

**Write tools** (`writesContent()==true` → authoring endpoint only; run under the caller's
`ResourceResolver`, fail closed on non-`/content` paths and unrecognized resource types):

| Tool | Args | Does | Catalog ID |
|---|---|---|---|
| `configure_product_component` | `{path, sku}` | Bind a SKU to a `product` component (`selection` + `selectionType=combinedSku`; gated to product v1/v2/v3) | baseline |
| `configure_productlist_component` | `{path, categoryUid}` | Pin a **product-list / carousel component** to a category (writes `category`; gated to productlist v1/v2 + productcarousel v1) | **◐ T-03 / T-02** |
| `configure_catalog_page` | `{path, categoryUid, showMainCategories?}` | Scope a **catalog page's** root category (`magentoRootCategoryId` + `magentoRootCategoryIdType=uid` + `showMainCategories`, on `jcr:content`; gated to catalog page v1/v3) | **◐ T-28** |
| `tag_content_with_commerce` | `{path, sku?, categoryUid?, action?}` | Set/remove `cq:products`/`cq:categories` on a DAM asset, page, or XF variation (`action`: `add`\|`remove`) | **✅ T-20** |

> **Correction to earlier draft:** the baseline previously described `configure_catalog_page`
> as binding "a category UID to a catalog page (`category` on `jcr:content`)". Writing
> `category` on a catalog page is a **silent no-op** — that was the original bug (fixed in
> commit `fa999f97`). The shipped tool writes `magentoRootCategoryId`, which is what
> `SiteStructure`/`NavigationImpl` actually read. This makes it the shipped realization of
> §7's `configure_catalog_page_scope` (T-28); see §7. Component-level category pinning
> (`category`, read by `ProductListImpl`/`ProductCarouselImpl`) is the *separate*
> `configure_productlist_component`. Keep page-vs-component category binding distinct.

### Out of scope for this catalog: shipped shopper cart & checkout tools

The `mcp` branch also ships an **8-tool shopper cart + guest-checkout suite** —
`add_to_cart`, `view_cart`, `update_cart_item`, `clear_cart`, `set_shipping_address`,
`set_shipping_method`, `set_payment_method`, `place_order`. These mutate the **remote
Magento cart/order**, not AEM content, so they are `writesContent()==false` and live on the
anonymous shopper endpoint (the confirm-before-commit gate on the checkout tools is their
safeguard, not the servlet split). They are **not authoring tools** and are intentionally
**not planned or duplicated here** — this catalog stays authoring-focused. They are listed
only so a future authoring-tool implementer doesn't collide with them. Their full contract
is in `bundles/mcp/README.md` ("Cart tools" / "Checkout tools") and the design docs under
`docs/superpowers/specs/2026-07-03-cif-shopper-*`.

---

## 0.1 Implementation substrate (shared by every tool below)

**SPI.** Every tool is `@Component(service = McpTool.class)` implementing
`com.adobe.cq.commerce.mcp.McpTool` — `name()`, `description()`, `inputSchema()` (a Jackson
`ObjectNode`), `writesContent()` (return `true` for JCR-content-writing Tier 2/3 so it binds
to the `mcp-authoring` selector), and `call(McpCallContext ctx, JsonNode args)`. Registration
is discovery-only: `ToolRegistry` binds every `McpTool` service dynamically — no manual list.

**Call context.** `ctx` is always `mcp/internal/StoreContext.java` (implements
`McpCallContext`). It exposes: `getClient()` → `MagentoGraphqlClient`, `getResource()`,
`getRequest()` (`SlingHttpServletRequest`), `getLandingPage()`, `getProductPage()`
(`com.day.cq.wcm.api.Page`). It is built by `mcp/internal/StoreContextResolver.java`,
which acquires the GraphQL client via **`request.adaptTo(MagentoGraphqlClient.class)`** —
this is how store/config/headers context is injected. Any new tool that needs a
`ResourceResolver` for writes gets it from `ctx.getRequest().getResourceResolver()` — the
**caller's** resolver, so JCR ACLs enforce the write (never a service/admin resolver).

**Three read execution styles** (pick per tool — noted individually in §3):

1. **Retriever-based** — subclass `AbstractProductRetriever` / `AbstractCategoryRetriever`
   (`core/.../models/retriever/`), or reuse the existing minimal
   `mcp/…/tools/McpProductRetriever.java` / `McpCategoryRetriever.java` and call
   `extendProductQueryWith(...)` / `extendCategoryQueryWith(...)` to add fields. Base API:
   `setIdentifier(String)`, `setCategoryIdType(String)` (`"urlPath"` else UID),
   `fetchProduct()` / `fetchCategory()`, `getErrors()`. Construct with `ctx.getClient()`.
   `GetProductTool`/`BrowseCategoriesTool` are the copy-paste templates.
2. **Raw typed query** — build with `mgql.Operations.query(q -> q.<field>(...))` and run
   `ctx.getClient().execute(queryString)` → `GraphqlResponse<Query,Error>`. Use when no
   retriever fits (sort fields, custom-attribute metadata, url resolver). This is exactly
   how `ProductSortFieldsDataSourceServlet` works today.
3. **Service-based** — `@Reference SearchResultsService` / `SearchFilterService` (how
   `search_products` / `get_attributes` work). Only relevant if a tool needs faceted search.

**Write patterns** (JCR content — Tier 2/3):

- **Property write (Tier 2)** — `ctx.getResource()` → child at the component/`jcr:content`
  path → `resource.adaptTo(ModifiableValueMap.class)`, `put`/`remove` the exact property
  names from §2/§7/§8, then `resourceResolver.commit()`. Multi-valued fields are
  `String[]`. This is the shape `configure_product_component`/`configure_productlist_component`
  already use — including a post-write read-back to populate the `updated` flag.
- **Node creation (Tier 3, components)** — create an `nt:unstructured` child under the
  page's responsive grid (`…/jcr:content/root/responsivegrid/<name>` in Venia) with
  `sling:resourceType` + the minimum properties in §4, then `commit()`.
- **Page creation (Tier 3, pages)** — `com.day.cq.wcm.api.PageManager.create(parentPath,
  name, templatePath, title)` (from `resourceResolver.adaptTo(PageManager.class)`). AEM
  copies the template's `initial/jcr:content` (including any pre-placed commerce
  component) automatically. 6.5-safe; no CIF-specific creation hook exists (§9).
- **Content Fragment write (Tier 3)** — `com.adobe.cq.dam.cfm.ContentFragment` /
  `ContentElement` / `FragmentData` (§6). 6.5-safe.

> **Reuse-vs-reimplement decision (settled by shipped code):** for associated-content and
> commerce-CF *reads*, the shipped tools depend on the **AEM CIF SDK**
> `AssociatedContentService` / `AssociatedContentQuery` (`com.adobe.aem:aem-cif-sdk-api`,
> `provided` scope; wrapped by `mcp/internal/AssociatedContentSupport.java`) rather than
> reimplementing the JCR-SQL2 in `bundles/mcp`. This resolves the open question the §5/§6
> mechanism notes below originally raised — see those sections and §12 Open Q2.

---

## 1. Tiering model

Tools below are grouped thematically, but every tool also carries a **risk/complexity
tier**, used to sequence future implementation plans:

- **Tier 1 — Read/diagnostic.** No content mutation. Safe to build and ship first;
  several are prerequisites for using Tier 2/3 tools safely (they expose mechanisms that
  are otherwise invisible or silently fragile).
- **Tier 2 — Config write (existing resource).** Sets/clears properties on a resource
  that already exists — same shape as `configure_product_component`. Moderate risk,
  scoped to one `jcr:content` node.
- **Tier 3 — Content-creation write.** Creates new JCR content (pages, components,
  Content Fragment instances). Higher blast radius than Tier 2 — a bad input creates
  real content in the tree, not just a bad property value.
- **Tier 4 — Deferred / needs an architecture decision.** Either crosses a repo boundary
  that conflicts with the AEM 6.5 compatibility constraint, or requires context
  resolution beyond what the nav-root-gated servlet currently supports.

---

## 2. Component-dialog configuration tools (Tier 2)

Direct extensions of the existing `configure_product_component` pattern: bind real
`cq:dialog` fields to a component's `jcr:content`. **Implementation for all of §2 is the
"Property write (Tier 2)" pattern from §0.1** — adapt the target node to
`ModifiableValueMap`, set the exact property names below, `commit()`. All dialogs live
under `apps/commerce/<component>/<version>/<component>/_cq_dialog/.content.xml`.

**Naming convention (aligned with shipped code).** The shipped write tools use
`configure_<jcrComponentName>_component` (e.g. `configure_product_component`,
`configure_productlist_component`) for component writes and `configure_<x>_page` for page
writes. This catalog's §2 tool names are aligned to that convention; the original
research-draft names are kept in parentheses for traceability.

**Granular-per-component vs. operation-generic (a real divergence to be aware of).** This
catalog keeps **one tool per component dialog** (each covers that component's full field
set). The shipped code so far took an **operation-generic** shortcut: the single
`configure_productlist_component` writes just the `category` selection for *both* the
productlist and productcarousel components. So the shipped tool partially satisfies two
catalog entries (T-02 category mode, T-03) with one code path; the remaining per-component
dialog fields are the unshipped delta. When implementing the rest of §2, decide per tool
whether to extend the existing generic tool or add a dedicated one — but keep the
`configure_<component>_component` naming either way.

Cross-cutting gotchas: product-picker fields store **combinedSku** unless noted;
category-picker fields store **UIDs** unless a `urlPath` selectionId is set; several
"enable…" flags are **policy/style-driven and NOT on the instance dialog** (a write tool
cannot set them on the component node).

| Tool (aligned name) | Status | Component (resourceType) | Backing model |
|---|---|---|---|
| `configure_productteaser_component` | ▢ | `…/productteaser/v1/productteaser` | `core/…/internal/models/v1/productteaser/ProductTeaserImpl.java` |
| `configure_productcarousel_component` | ◐ (category mode shipped) | `…/productcarousel/v1/productcarousel` | `core/…/internal/models/v1/productcarousel/ProductCarouselImpl.java` |
| `configure_productlist_component` | ✅/◐ (category shipped) | `…/productcollection/v2` **/** `…/productlist/v2` | `core/…/internal/models/v1/productcollection/ProductCollectionImpl.java`, `…/v2/productlist/ProductListImpl.java` |
| `configure_relatedproducts_component` | ▢ | `…/relatedproducts/v1/relatedproducts` | `core/…/internal/models/v1/relatedproducts/RelatedProductsImpl.java` |
| `configure_categorycarousel_component` | ▢ | `…/categorycarousel/v1/categorycarousel` | `core/…/internal/models/v1/categorylist/FeaturedCategoryListImpl.java` |
| `configure_featuredcategorylist_component` | ▢ | `…/featuredcategorylist/v1/featuredcategorylist` | `FeaturedCategoryListImpl.java` (same model, both RTs) |
| `configure_product_visible_sections` | ▢ | `…/product/v3/product` | `core/…/internal/models/v3/product/ProductImpl.java` |
| `configure_page_commerce_links` | ◐ (markers shipped) | `…/structure/page/v3/page` | `core/…/internal/models/v3/page/PageImpl.java` |

### 2.1 `configure_productteaser_component` (T-01, was `configure_product_teaser`) — ▢ Planned
- **Properties:** `selection` (**combinedSku**, model `SELECTION_PROPERTY`, parsed via
  `CombinedSku.parse`), `cta` (`""`/`add-to-cart`/`details`), `ctaText`, `linkTarget`
  (`Link.PN_LINK_TARGET`), `id`.
- **Gotcha:** `enableAddToWishList` is **not** on this dialog (style/policy only —
  `PN_STYLE_ADD_TO_WISHLIST_ENABLED`); don't expose it as a settable arg.

### 2.2 `configure_productcarousel_component` (T-02, was `configure_product_carousel`) — ◐ Partial
- **Shipped:** category mode (`category` = single UID) is already writable via
  `configure_productlist_component` (gated to productcarousel v1). The fields below are the
  **unshipped delta** for a dedicated carousel tool.
- **Properties:** `selectionType` (`product`|`category`, defaults to `product` when blank),
  `product` (**flat multi-valued `String[]`** of combinedSkus — NOT child nodes),
  `category` (**single UID**), `productCount` (`Long`, category mode), `enableAddToCart`,
  `enableAddToWishList` (`"true"`/`"false"`), `id`.
- **Gotcha:** product mode reads `product[]`; category mode reads `category`+`productCount`.
  Model tolerates a path-prefixed SKU and strips to last segment.

### 2.3 `configure_productlist_component` (T-03, was `configure_product_collection`) — ◐ Partial (SHIPPED subset)
- **Shipped today:** `configure_productlist_component {path, categoryUid}` writes the
  `category` manual-selection property (gated to productlist v1/v2 + productcarousel v1),
  with post-write read-back verification. See `mcp/…/tools/ConfigureProductListComponentTool.java`.
- **Unshipped delta — properties differ by component, do not conflate:**
  - `productcollection/v2` dialog: `pageSize` (`PN_PAGE_SIZE`), `defaultSortField`
    (`PN_DEFAULT_SORT_FIELD`), `defaultSortOrder` (`asc`|`desc`, `PN_DEFAULT_SORT_ORDER`), `id`.
  - `productlist/v2` dialog: `category` (**single UID — SHIPPED**), `showTitle`, `showImage`
    (`"true"`/`"false"`, default true), `fragments` (**composite multifield** → child
    nodes `fragments/item0…` with unprefixed props `fragmentLocation`, `fragmentPage`,
    `fragmentCssClass`), `id`.
- **Gotcha:** `pageSize`/sort fields are **only** on `productcollection/v2`; `productlist/v2`'s
  own dialog has none of them. `defaultSortField` values are **datasource-driven**
  (`…/productcollection/sortfields`) and backend-specific — validate with `get_sort_options`
  (T-09), don't hardcode.

### 2.4 `configure_relatedproducts_component` (T-04, was `configure_related_products`) — ▢ Planned
- **Properties:** `product` (**plain base SKU**, `selectionId="sku"` — unique among §2;
  optional, falls back to page-URL product), `relationType`.
- **`relationType` values** (enum `RelatedProductsRetriever.RelationType`, stored as the
  enum name): `RELATED_PRODUCTS`, `UPSELL_PRODUCTS`, `CROSS_SELL_PRODUCTS`.

### 2.5 `configure_categorycarousel_component` (T-05) / 2.6 `configure_featuredcategorylist_component` (T-06) — ▢ Planned
- **Same Sling model** (`FeaturedCategoryListImpl`, registered for both RTs). Both use an
  `items` **composite multifield** → child nodes `items/item0…` with unprefixed props
  `categoryId` (**UID**) and `asset` (path under `/content/dam`).
- **featuredcategorylist adds:** `jcr:title` (default `"Shop by category"`, empty hides
  title), `titleType` (heading tag), `linkTarget`, `id`.
- **Gotcha:** writing these means creating/replacing child nodes under `items/`, not a
  flat property — heavier than a scalar set.

### 2.7 `configure_product_visible_sections` (T-07) — ▢ Planned
- **Note:** operates on the same `product` component as the shipped
  `configure_product_component`, but a *different* concern (section visibility, not SKU
  binding), so it keeps its own operation-scoped name rather than colliding with
  `configure_product_component`.
- **Property:** `visibleSections` — multi-valued `String[]`, **lowercase** values:
  `actions`, `description`, `details`, `images`, `options`, `price`, `quantity`, `sku`,
  `title` (model `PN_VISIBLE_SECTIONS`; maps lowercase→uppercase `Product.*_SECTION`).
- **Gotcha:** if empty/absent, sections fall back to the style/policy default (all
  sections). Write the **lowercase** dialog tokens, not the uppercase model constants.

### 2.8 `configure_page_commerce_links` (T-08) — ◐ Partial
- **Shipped:** the `cq:products`/`cq:categories` marker part is already writable via
  `tag_content_with_commerce {path, sku?/categoryUid?, action}` (it sets exactly these two
  properties on a page's `jcr:content`). The **unshipped delta** is the nav-config
  pagefields (`cq:cifProductPage`/`cq:cifCategoryPage`/`cq:cifSearchResultsPage`).
- **Properties (on the page's `jcr:content`):** `cq:products` (**combinedSku multi**),
  `cq:categories` (**UID multi**), `cq:cifProductPage`, `cq:cifCategoryPage`,
  `cq:cifSearchResultsPage` (content paths, pagefields).
- **Gotcha:** `cq:products`/`cq:categories` here are *associated-content markers* (read by
  the §5 mechanism), **not** the specific-page binding of §7/§8 — keep them distinct in
  any plan. Dialog fields are render-condition-gated by page type, but a write tool can
  set the property regardless of which type the page is.

---

## 3. Metadata & picker-support read tools (Tier 1)

Make the Tier 2 tools trustworthy instead of guess-driven, by exposing real backend data.
**Execution styles per §0.1 noted per tool.** GraphQL bindings are in `mgql.` (line
numbers are indicative). Tools 2/3/4 extend the existing `Mcp*Retriever`; tools 1/5/6
have no retriever and are raw typed queries.

| Tool | Status | Style | Backing |
|---|---|---|---|
| `get_sort_options` | ▢ | raw query + datasource | `ProductSortFieldsDataSourceServlet` + `mgql.SortFields`/`SortField` |
| `get_product_variants` (was `get_configurable_variants`) | ✅ | retriever (extend product) | `mgql.ConfigurableProductQuery.configurableOptions()/variants()` |
| `get_product_relationships` | ▢ | retriever (extend product) | `mgql.ProductInterfaceQuery.productLinks()` |
| `get_category_breadcrumbs` | ▢ | retriever (extend category) | `mgql.CategoryTreeQuery.breadcrumbs()` |
| `get_custom_attribute_metadata` | ▢ | raw query | `mgql.QueryQuery.customAttributeMetadata(List<AttributeInput>, …)` |
| `resolve_url_to_entity` | ▢ | raw query | `mgql.QueryQuery.urlResolver(String,…)` / `route(String,…)` |
| `validate_content_bindings` | ▢ | retriever | `AbstractProductRetriever`/`AbstractCategoryRetriever` |
| `resolve_category_details` | ▢ | retriever | `McpCategoryRetriever` + breadcrumbs |

### 3.1 `get_sort_options` (T-09) — ▢ Planned
- **Backing:** `core/…/internal/servlets/ProductSortFieldsDataSourceServlet.java` runs
  `{products(filter:{}){sort_fields{default options{label value}}}}` and reads
  `getProducts().getSortFields()` → `mgql.SortFields.getDefault()` /
  `getOptions()` → `mgql.SortField.getValue()`/`getLabel()`.
- **Shape:** returns `{default, options:[{value,label}]}`. Note `relevance` is injected by
  the servlet for searchresults and is **not** in the API response for plain listings.

### 3.2 `get_product_variants` (T-10, was `get_configurable_variants`) — ✅ Shipped
- **Shipped:** `get_product_variants {sku}` → `{sku,name,urlKey,configurable,options:[…],variants:[…]}`,
  backed by `mcp/…/tools/GetProductVariantsTool.java` + `McpProductVariantsRetriever.java`
  + `mcp/internal/dto/ProductVariantsDtoMapper.java`. Renamed from the catalog's
  `get_configurable_variants`; the catalog adopts the shipped name.
- **Query (as built):** inside `ProductInterfaceQuery.onConfigurableProduct(...)` select
  `configurable_options{attribute_code,label,uid,values{…}}` and
  `variants{attributes{…},product{sku,name,price_range}}`.
- **Variant SKU:** `mgql.ConfigurableVariant.getProduct().getSku()`.
- **Gotcha:** there is **no singular `.variant()` accessor** — only the `variants` list
  (narrow via `configurableProductOptionsSelection(configurableOptionValueUids:[…])`).

### 3.3 `get_product_relationships` (T-11) — ▢ Planned
- **Query:** `product_links{ link_type, linked_product_sku, linked_product_type,
  position, sku }` (`mgql.ProductInterface.getProductLinks()` → `ProductLinksInterface`).
- **Gotcha:** `link_type` is a **plain String** (`"related"`/`"upsell"`/`"crosssell"`), not
  an enum — group/filter client-side.

### 3.4 `get_category_breadcrumbs` (T-12) — ▢ Planned
- **Query:** extend the category retriever with
  `breadcrumbs{category_uid,category_name,category_level,category_url_path}`
  (`mgql.CategoryInterface.getBreadcrumbs()` → `mgql.Breadcrumb`). Order by `category_level`.

### 3.5 `get_custom_attribute_metadata` (T-13) — ▢ Planned
- **Query:** `customAttributeMetadata(attributes)` **requires** a `List<AttributeInput>`
  each `{attribute_code, entity_type}` (e.g. `catalog_product`). Read
  `CustomAttributeMetadata.getItems()` → `mgql.Attribute` (`attribute_code`,
  `attribute_type`, `input_type`, `entity_type`, `attribute_options{label,value}`).
- **Style:** raw query (`Operations.query(q -> q.customAttributeMetadata(inputs, def))`).

### 3.6 `resolve_url_to_entity` (T-14) — ▢ Planned
- **Two options:** `urlResolver(url)` → `mgql.EntityUrl` (`type`→`UrlRewriteEntityTypeEnum`,
  `id`, `entity_uid`, `canonical_url`, `relative_url`, `redirect_code`) for a lightweight
  type+id lookup; **or** `route(url)` → `mgql.RoutableInterface` with
  `onProductInterface(p -> p.sku().urlKey())` / `onCategoryInterface(...)` etc. when you
  need the entity's sku/uid/url_key in the same call.
- **Style:** raw query.

### 3.7 `validate_content_bindings` (T-15) / 3.8 `resolve_category_details` (T-16) — ▢ Planned
- **Backing:** the retriever pattern (§0.1 style 1). For each `(SKU|UID)` construct
  `McpProductRetriever`/`McpCategoryRetriever` (or the abstract base), `setIdentifier`,
  `fetchProduct()`/`fetchCategory()`, and treat a null/`getErrors()` result as
  "no longer resolves." `resolve_category_details` additionally extends the category
  query with `breadcrumbs` (reuse T-12's selection).
- **Gotcha:** batch calls should reuse one client and, where possible, one query — the
  retrievers cache after first fetch but are single-identifier; a bulk tool loops.

---

## 4. Bulk / scaffolding component tools (Tier 3)

New capability, not just dialog wrapping — "set up N similar components" in one call.
**Implementation = "Node creation" pattern from §0.1**: stamp `nt:unstructured` children
under `…/jcr:content/root/responsivegrid/<name>` with `sling:resourceType` + the minimum
properties below, then `commit()`. Neither component needs child nodes for the basic case.

| Tool | Status | Minimum viable node |
|---|---|---|
| `create_product_teasers(parentPage, skus[], ctaConfig)` (T-17) | ▢ | `sling:resourceType=…/productteaser/v1/productteaser` + `selection`=combinedSku (rest optional) |
| `create_product_carousels(parentPage, categoryUid, displayConfig, count)` (T-18) | ▢ | `sling:resourceType=…/productcarousel/v1/productcarousel` + either `product`=`String[]` (selectionType defaults to `product`) **or** `selectionType=category`+`category`=UID (+optional `productCount`) |

**Gotcha:** node name collisions under the grid — generate unique names
(`productteaser`, `productteaser_1`, …) like the AEM editor does. `componentGroup` is
`CIF Core Components`. (See §9 for page/catalog-page creation, a related Tier 3 group.)

---

## 5. Associated content tools (Tier 1 read / Tier 2 write)

**Mechanism** (confirmed from `skyline/common/.../associatedcontent/internal/AssociatedContentServiceImpl.java`
and the read-only `skyline/commerce-addon/.../authoring/AdminDataServlet.java`, GET at
`/bin/cifadmindata`): association between a SKU/category and other content is
**property-based**, matched via **JCR-SQL2** (`Query.JCR_SQL2`) built as a union of
per-identifier statements. The matched property paths (inlined string literals, **no named
Java constants**):

| Content type | Property path matched |
|---|---|
| Page | `jcr:content/cq:products` / `jcr:content/cq:categories` |
| DAM Asset | `jcr:content/metadata/cq:products` / `…/cq:categories` |
| Experience Fragment (variant node) | `jcr:content/cq:products` / `jcr:content/cq:categories` |
| Content Fragment | `jcr:content/data/master/<field>` gated by `jcr:content/data/cq:model=<modelPath>` and `jcr:content/contentFragment='true'` |

- **Variant-SKU matching** (products only; categories are exact-match): binds
  `identifierPrefix = "<SKU>#%"` and `identifierSuffix = "%#<SKU>"` and ORs
  `[prop] = $identifier OR [prop] LIKE $identifierPrefix OR [prop] LIKE $identifierSuffix`.
- **CF field discovery**: a JCR-SQL2 scan over `/conf` for nodes whose `sling:resourceType`
  is `cif/cfm/admin/components/productreference` / `…/categoryreference` (constants
  `RT_CIF_PRODUCT_FIELD` / `RT_CIF_CATEGORY_FIELD`; plus legacy
  `commerce/gui/components/common/cifcategoryfield`), deriving `Map<modelPath,Set<field>>`.
  These reference-field resource types are **defined in cif-on-skyline**
  (`apps/…/libs/settings/dam/cfm/models/formbuilderconfig/datatypes/{product,category}-reference/`),
  not in core.
- **Only the `master` variation is queried** (documented `TODO CIF-2109`) — non-master
  variation values are invisible to the lookup.

> **Reconciliation — read side is shipped by depending on the SDK service, not by
> reimplementing.** The catalog originally proposed reimplementing the JCR-SQL2 inside
> `bundles/mcp`. The shipped `get_product_associated_content` / `get_category_associated_content`
> tools instead call the **AEM CIF SDK** `AssociatedContentService` /
> `AssociatedContentQuery` (`com.adobe.aem:aem-cif-sdk-api`, `provided` scope), wrapped by
> `mcp/internal/AssociatedContentSupport.java`. So T-19 is **✅ shipped** (split into a
> product and a category tool), and the "reimplement JCR-SQL2" recommendation is
> superseded — see §12 Open Q2 (now resolved). A future plan targeting AEM 6.5 without the
> SDK on the classpath would need to revisit this, but the current shipped decision is
> "depend on the service."

| Tool | Status | Tier | Does |
|---|---|---|---|
| `get_product_associated_content(sku, …)` / `get_category_associated_content(categoryUid, …)` (T-19, was `get_associated_content`) | ✅ | 1 | Returns pages/assets/XFs/CFs referencing a SKU/category (one tool per identifier type) |
| `tag_content_with_commerce(path, sku?, categoryUid?, action?)` (T-20, was `link_content_to_entity`) | ✅ | 2 | Sets/removes `cq:products`/`cq:categories` on a page/asset/XF variation |
| `find_orphaned_commerce_content()` (T-21) | ▢ | 1 | Sweeps for content linked to a SKU/UID that no longer resolves (combine with `get_product`/`resolve_url_to_entity`) |

- **T-19 (shipped):** args `{sku|categoryUid, fragmentLocation?, contentFragmentModel?,
  linkElement?, limit?}`; result groups `experienceFragments`, `contentFragments`,
  `contentPages`, `assets`. Backed by `mcp/…/tools/GetProductAssociatedContentTool.java` /
  `GetCategoryAssociatedContentTool.java` over `AssociatedContentSupport`.
- **T-20 (shipped):** `tag_content_with_commerce` writes the **multi-valued** `cq:products`
  (combinedSku) / `cq:categories` (UID), plus `cq:productsType=combinedSku` for multi-SKU
  DAM metadata, via `mcp/internal/CommerceContentTagger.java`. `action` is `add` (default)
  or `remove`. **Fail-closed** (commit `f2a8639d`): `CommerceContentTagger.resolveTagTarget`
  accepts only DAM assets (`dam:Asset` → metadata node), pages/XF variations (`cq:Page` →
  `jcr:content`), or a `cq:PageContent` node directly; **any other resource type throws**
  rather than being silently tagged.
- **T-21 (planned):** run T-19's sweep, then resolve each identifier via the retrievers
  (T-15) and flag the misses.

**Implementation note (updated):** the shipped tools consume the SDK `AssociatedContentService`
directly (`aem-cif-sdk-api`). If a future 6.5-only packaging needs to drop that dependency,
the JCR-SQL2 above is the portable fallback (OOTB `damAssetLucene`/`experienceFragmentsIndex`/
node-scope indexes) — match the property paths and the prefix/suffix variant logic exactly.
Optionally keep an OSGi-optional `@Reference` to the real service and fall back to the local
query when it's absent.

---

## 6. Commerce Content Fragment editing tools (Tier 1 read / Tier 3 write)

**Mechanism** (confirmed from
`core/…/internal/models/v1/contentfragment/CommerceContentFragmentImpl.java` and interface
`core/…/models/contentfragment/CommerceContentFragment.java`): a CF is matched to a
SKU/category by **field value**, not folder/naming. The `contentfragment/v1` component's
dialog (`apps/commerce/contentfragment/v1/contentfragment/_cq_dialog/.content.xml`)
configures `modelPath` (CF Model — `PN_MODEL_PATH="modelPath"`), `linkElement` (which
model field holds the SKU/UID — `PN_LINK_ELEMENT="linkElement"`, `required`), and
`parentPath` (`PN_PARENT_PATH="parentPath"`, pathfield `rootPath="/content/dam"`, model
default `/content/dam` = `DamConstants.MOUNTPOINT_ASSETS`).

`findContentFragment()` does **not** run its own query — it resolves the current
SKU/category-UID from the URL via `UrlProvider`, then **delegates to the CIF SDK
`AssociatedContentService`** (injected `@OSGiService`) via `listProductContentFragments(...)`
/ `listCategoryContentFragments(...)` with `CfParams.of(id).model(modelPath).property(linkElement)`,
taking the first hit (`withLimit(1)`). The shipped MCP tools use the **same** SDK service
(§5) — so this is consistent, not a new dependency.

**Reading** uses the Core WCM `ContentFragment` model + `com.adobe.cq.dam.cfm.ContentFragment`
(`getElement`, `getElements`, `getValue`) — all **AEM-6.5-safe**. **Writing does not exist
anywhere in CIF today.** Full write surface (from `cq-dam-cfm-api`, 6.5-safe):

```java
ContentFragment cf = resource.adaptTo(ContentFragment.class);
ContentElement el   = cf.getElement(fieldName);
FragmentData data   = el.getValue();     // preserves DataType + contentType
data.setValue(newValue);                 // typed: String/String[]/Calendar/Long/Double/Boolean
el.setValue(data);                        // richtext: el.setContent(html, "text/html")
// per-variation: el.getVariation(variationName).setValue(data)
resourceResolver.commit();
```

| Tool | Status | Tier | Does |
|---|---|---|---|
| `get_commerce_content_fragment(identifier, type)` (T-22) | ◐ | 1 | Resolve a CF via `linkElement` match and return its fields |
| `update_commerce_content_fragment_field(fragmentPath, elementName, value, variation?)` (T-23) | ▢ | 3 | **The real editing capability** — sets a CF element's value. Draft-only; never auto-publishes |
| `create_commerce_content_fragment(identifier, type, modelPath, fields)` (T-24) | ▢ | 3 | Creates a new CF under `parentPath`, seeds `linkElement` with the SKU/UID + other fields |

- **T-22 (partial):** CF **discovery+read** is already covered by the shipped
  `get_product_associated_content` / `get_category_associated_content` tools — they return a
  `contentFragments:[…]` list and accept `contentFragmentModel?`/`linkElement?` to scope the
  match (same `linkElement` logic as `findContentFragment()`). A dedicated
  `get_commerce_content_fragment` returning `{modelPath, fragmentPath, fields}` for a single
  CF is the remaining delta — decide whether it's worth a separate tool or a richer shape on
  the associated-content result.
- **Per-field-type concerns (T-23):** text/richtext via `setContent(value, "text/plain"|"text/html")`;
  number/date/boolean via `FragmentData.setValue(Object)` with the matching Java type —
  check `getDataType()`/`isTypeSupported(Class)` first to avoid `ContentFragmentException`.
  product/category reference fields are multi-value (`String[]`).
- **Variation gotcha:** the lookup query (§5) only matches the **master** variation — a
  write to a non-master variation will be invisible to the read tools. Resolve the
  variation-handling policy (open question 3) before finalizing T-23's signature.

**Scope guardrail:** writes here behave like an author editing in the Assets UI — modify
draft/master content, leave activation to the normal publish flow.

---

## 7. Multi-catalog-page tools (Tier 1 read / Tier 2 write)

**Mechanism** (confirmed from
`core/…/internal/services/site/SiteStructureImpl.java` +
`core/…/models/common/SiteStructure.java`, and
`core/…/internal/services/SpecificPageStrategy.java`): a site can have more than one
catalog page. Constants:

- `PN_MAGENTO_ROOT_CATEGORY_IDENTIFIER = "magentoRootCategoryId"`
- `PN_MAGENTO_ROOT_CATEGORY_IDENTIFIER_TYPE = "magentoRootCategoryIdType"` (special-cased
  value `"urlPath"`; any other value ⇒ treated as a UID and cannot be path-matched)
- Catalog-page resource types: `RT_CATALOG_PAGE = "core/cif/components/structure/catalogpage/v1/catalogpage"`,
  `RT_CATALOG_PAGE_V3 = "…/catalogpage/v3/catalogpage"` — **v1 and v3 only, no v2 constant**.

`getCatalogPages()` collects **direct children of the nav root** (`navRoot=true`, found by
walking up) that pass `isCatalogPage` (resource-type check only), de-duped by name, with
the nav-root page itself **always appended last** as the generic fallback. Winning page for
a category = `SpecificPageStrategy.getGenericPage(...)`: iterate that ordered list, return
the first entry that is either generic (`magentoRootCategoryIdType != "urlPath"` or empty
id) **or** whose `urlPath` scope contains the category (`isSpecificCatalogPageFor`, which
always includes sub-categories). **First match wins; there is no explicit mapping table.**

| Tool | Status | Tier | Does |
|---|---|---|---|
| `list_catalog_pages(siteRoot)` (T-25) | ▢ | 1 | Every catalog page under a site + its root-category scope, in one call |
| `explain_catalog_page_routing(identifier)` (T-26) | ▢ | 1 | Replays `getGenericPage` for a category, reports which page wins and why |
| `detect_catalog_page_conflicts(siteRoot)` (T-27) | ▢ | 1 | Flags overlapping `urlPath` scopes (ambiguous) and categories with no matching page (dead links) |
| `configure_catalog_page` (T-28, was `configure_catalog_page_scope`) | ◐ | 2 | Property write of `magentoRootCategoryId` (+ `magentoRootCategoryIdType`, + `showMainCategories`) on the catalog page's `jcr:content` |

> **T-28 is the shipped baseline write tool** `configure_catalog_page {path, categoryUid,
> showMainCategories?}` (see §0). **Shipped subset/deltas vs. this catalog entry:** the
> shipped tool **hardcodes `magentoRootCategoryIdType=uid`** (it does not yet expose the
> `urlPath` idType option T-28 originally called for) and **adds `showMainCategories`**
> (default `false`), which the original T-28 signature didn't mention. Adding a `urlPath`
> idType option is the remaining delta; everything else is done, with post-write read-back
> verification.

- **T-25/26/27 implementation:** reuse `SiteStructure`/`SpecificPageStrategy` logic — read
  `getCatalogPages()` order and each page's two scope properties; for `explain`, mirror the
  `getGenericPage` first-match walk (generic-vs-`urlPath` branch).
- **T-28 gotcha:** `idType` must be `"urlPath"` for the page to participate in path
  matching; any other value (including the shipped hardcoded `uid`) makes it a generic
  fallback regardless of `rootCategoryId`.

(`create_catalog_page` and `scaffold_catalog_section` are covered in §9.)

---

## 8. Specific PDP/PLP binding tools (Tier 1 read / Tier 2 write / Tier 3 create)

**Status: ▢ Planned (none shipped).** The deepest mechanism investigated and the one with
the most confirmed real gotchas — prioritize the Tier 1 diagnostics alongside or before the
Tier 2 writes. (Do not confuse this with §7's `configure_catalog_page`, which scopes a whole
catalog page by root category; §8 binds *specific* descendant PDP/PLP pages.)

### 8.1 Mechanism (confirmed — `core/…/internal/services/SpecificPageStrategy.java`)

A page typed **product** or **category** (`core/cif/components/structure/page`) can be
bound as the dedicated PDP/PLP for a scoped subset of the catalog. Binding constants
(`SpecificPageStrategy.java:49-56`):

- `SELECTOR_FILTER_PROPERTY = "selectorFilter"`
- `SELECTOR_FILTER_TYPE_PROPERTY = "selectorFilterType"` — **default `"uidAndUrlPath"`**
  (read as `properties.get(..., "uidAndUrlPath")`). When `"uidAndUrlPath"`, each filter is
  split on `|` into `uid|urlPath`; any other value treats filters as plain url-paths.
- `INCLUDES_SUBCATEGORIES_PROPERTY = "includesSubCategories"`
- `PN_USE_FOR_CATEGORIES = "useForCategories"`
- `UID_AND_URL_PATH_SEPARATOR = "|"`

| Page type | Field | Binds to | Subtree option |
|---|---|---|---|
| Product page | `selectorFilter` (dialog `selectionId="slug"`) | Specific SKUs/URL keys → dedicated PDP | — |
| Category page | `selectorFilter` (+ `selectorFilterType`) | Specific category (`uid\|urlPath`) → dedicated PLP | `includesSubCategories` |
| **Product page** | `useForCategories` (+ `includesSubCategories`) | **A whole category/tree → every product under it routes to this custom PDP** | `includesSubCategories` |

**Version differences (correction):**
- `useForCategories` is **v2+ only** (absent in v1).
- `includesSubCategories` **exists in v1, v2, and v3** — but its render condition was
  broadened in v2+ from category-only to category-**or**-product, i.e. the *product-page*
  applicability of `includesSubCategories` is v2+.
- The category `selectorFilter` `selectionId` changed from `uidAndUrlPath` (v1) to
  `urlPath` (v2/v3). Product `selectorFilter` is always `slug`.

**Resolution algorithm** (`getSpecificPage`, depth-first): `traverse()` emits
`traverse(child)` **before** `Stream.of(child)`, so descendants precede parents, and
`findFirst()` means **the deepest matching page wins — by tree depth, not filter
specificity** (proven by `SpecificPageStrategyTest.testNestedSpecificProductPage` /
`testNestedSpecificCategory` in `core`'s test tree). Subtree predicate:

```java
categoryUrlPath.equals(givenUrlPath)
  || (includeSubCategories && StringUtils.startsWith(givenUrlPath, categoryUrlPath + "/"))
```

- Category `selectorFilter` format is `uid|urlPath` (literal pipe). Malformed (no pipe)
  degrades to treating the string as both uid and url-path — ambiguous.
- No conflict detection and no "unset binding" UI action exist.
- Independent of landing-page nav config (`cq:cifProductPage` etc.): those only pick the
  **search root** that `SpecificPageStrategy` then walks; the binding fields on descendant
  pages pick the **winner**. `UrlProviderImpl.getSpecificPageAndFormat()` orchestrates
  (root selection via `getGenericPage`, then `getSpecificPage` on that root).

### 8.2 Tools

| Tool | Status | Tier | Does |
|---|---|---|---|
| `bind_page_to_products(page, skusOrUrlKeys[])` (T-29) | ▢ | 2 | Sets `selectorFilter` on a product page |
| `bind_page_to_category(page, categoryUid, urlPath, includesSubCategories)` (T-30) | ▢ | 2 | Sets `selectorFilter`(+`selectorFilterType`) + `includesSubCategories` on a category page |
| `bind_product_page_to_category_tree(page, categoryUid, urlPath, includesSubCategories)` (T-31) | ▢ | 2 | Sets `useForCategories`+`includesSubCategories` on a product page (v2+) |
| `unbind_specific_page(page)` (T-32) | ▢ | 2 | Clears the binding fields — no such affordance in the dialog today |
| `explain_page_resolution(identifier, type)` (T-33) | ▢ | 1 | Replays the depth-first match, reports the winning page **and its tree depth** |
| `list_specific_pages(siteRoot)` (T-34) | ▢ | 1 | Every page under a site with a non-empty binding field, and what it binds |
| `detect_specific_page_conflicts(siteRoot)` (T-35) | ▢ | 1 | Flags identical-scope duplicates and structural shadowing risk (narrower binding at ≤ depth of a broader one) |
| `validate_selector_filter_format(page)` (T-36) | ▢ | 1 | Catches malformed `uid\|urlPath` (missing pipe → ambiguous fallback) |
| `check_specific_page_capability(page)` (T-37) | ▢ | 1 | Resolves component version; reports which binding fields exist (`useForCategories`/product-page `includesSubCategories` are v2+) |

- **Write tools (T-29–32):** Property write (§0.1), same fail-closed shape as the shipped
  `configure_*` tools. For category bindings, honor the `uid|urlPath` format and set
  `selectorFilterType` explicitly (don't rely on the default differing by version).
  `useForCategories` is multi-valued.
- **Diagnostic tools (T-33–37):** mirror `SpecificPageStrategy.traverse`/`isSpecificPage`
  (candidate = non-null `selectorFilter` **or** `useForCategories`) and the subtree
  predicate above. **Priority:** build `explain_page_resolution` + `detect_specific_page_conflicts`
  **before/with** the write tools, or an agent can construct the silent-shadowing bug.

---

## 9. Page / PDP / PLP creation tools (Tier 3)

**Status: ▢ Planned (none shipped).** **Mechanism** (confirmed via templates in
`aem-cif-guides-venia`): the product-page and category-page editable templates
**pre-populate `initial/jcr:content`** with a commerce component already placed inside the
responsive grid, so a page created from them is immediately functional. The pre-placed nodes
are **Venia proxy components**, not the core types directly:

| Template (`venia/…/conf/venia/settings/wcm/templates/…`) | `jcr:title` | Pre-placed in `initial` grid |
|---|---|---|
| `product-page/initial/.content.xml` | `Product page` | `sling:resourceType="venia/components/commerce/product"` → superType `core/cif/components/commerce/product/v3/product` |
| `category-page/initial/.content.xml` | `Category page` | `sling:resourceType="venia/components/commerce/productlist"` → superType `core/cif/components/commerce/productlist/v2/productlist` |
| `catalog-page/initial/.content.xml` | `Catalog Page` | **empty** responsiveGrid (structural only) |
| `landing-page/initial/.content.xml` | `Landing page` (`navRoot=true`) | empty |

- **`initial` vs `structure`:** the **`initial/jcr:content`** node seeds a newly created
  page's body; the pre-placed component lives under
  `initial/jcr:content/root/container/container` (responsiveGrid). `structure` holds locked
  chrome (header/footer XF refs, breadcrumb) + an editable empty slot — the commerce
  component is in `initial`, not `structure`.
- **Creation API:** plain `PageManager.create(parentPath, name, templatePath, title)` — AEM
  copies `initial` automatically; **no CIF-specific creation step**; 6.5-safe.
- **Template auto-discovery** by `jcr:title` (`"Product page"`/`"Category page"`/`"Catalog
  Page"` — note inconsistent casing) is an **inferred Venia convention, not a CIF
  standard** — a more robust signal is the `initial` content's `resourceSuperType`
  (`…/commerce/product/*` vs `…/productlist/*`). Treat title-matching as a fallback.

| Tool | Status | Tier | Does |
|---|---|---|---|
| `create_catalog_page(parent, name, title, rootCategoryId, idType?, template?)` (T-38) | ▢ | 3 | Creates the page, then applies §7 `configure_catalog_page` |
| `create_specific_pdp(parent, name, title, skusOrUrlKeys[], template?)` (T-39) | ▢ | 3 | Product-page template (component pre-placed) + §8 `bind_page_to_products` |
| `create_specific_plp(parent, name, title, categoryUid, urlPath, includesSubCategories, template?)` (T-40) | ▢ | 3 | Category-page template + `bind_page_to_category` |
| `create_specific_pdp_for_category_tree(parent, name, title, categoryUid, urlPath, includesSubCategories, template?)` (T-41) | ▢ | 3 | Product-page template + `bind_product_page_to_category_tree` |
| `scaffold_catalog_section(parent, name, rootCategoryId, template?)` (T-42) | ▢ | 3 | Creates a whole catalog section (catalog page + example product/category children) wired to a root category |
| `suggest_template_for_page_type(kind)` (T-43) | ▢ | 1 | Lists template candidates under `/conf/*/settings/wcm/templates/` matching a page-type signal |

**Guardrails (first tools that create content):**
- Validate the resolved template's `initial` content actually contains the expected
  `product`/`productlist` resource (via `resourceSuperType`) **before** `PageManager.create`
  — fail clearly rather than produce an empty-rendering page.
- Consider a dry-run/preview mode and stricter parent-path validation (must be under a
  recognized catalog/site structure) given the larger blast radius.

---

## 10. Deferred: cross-repo / global-config tools (Tier 4 — needs an architecture decision)

**Status: ▢ Planned / deferred (none shipped).** Found in `cif-on-skyline`. Real and
valuable for diagnosing *why a storefront isn't working*, but blocked from folding into
`bundles/mcp` as-is by:

1. **Context scope** — these operate at site/OSGi-config level, not per-nav-root, which
   `StoreContextResolver` (on-nav-root gating) doesn't naturally fit.
2. **Packaging (nuanced):** research found only **`GraphQLProxyServlet`** is
   *provably* Cloud-bound — it has two source copies, `commerce-addon-cs` (Jakarta servlet
   API) and `commerce-addon-65` (javax), and the split exists precisely because of the
   servlet-API difference. The **other** classes live in the shared `commerce-addon`
   module and use only `javax.servlet`/Sling/OSGi/`com.adobe.cq.commerce.graphql.client`
   APIs — **no AEMaaCS-only imports were found**. Their Cloud placement is a packaging
   decision, not a hard 6.5 incompatibility. (The `enableNewContentFragmentSupport`
   `extensionConfig`/`selectionType` scan does target the newer AEMaaCS CF-model UI.)

| Tool | Backing (cif-on-skyline) | FQN / path |
|---|---|---|
| `get_commerce_config` (T-44) | `ConfigurationInheritanceServlet` | `skyline/…/gui/components/configuration/servlets/ConfigurationInheritanceServlet.java` — GET `.cifconfig.json` on config pages |
| `validate_graphql_connectivity` (T-45) | `GraphQLProxyServlet` | `skyline/…/proxy/GraphQLProxyServlet.java` (cs + 65) — proxies `/api/graphql` (**Cloud-bound**) |
| `invalidate_cache` (T-46) | `InvalidateCacheServlet` | `skyline/…/cacheinvalidation/internal/InvalidateCacheServlet.java` — POST `/bin/cif/invalidate-cache` |
| `list_store_views` (T-47) | `Constants.PN_MAGENTO_STORE` | `skyline/…/cif/Constants.java:44` — value **`"magentoStore"`** |
| `get_catalog_service_config` (T-48) | `CatalogServiceConfigExporterServlet` | `skyline/…/components/internal/CatalogServiceConfigExporterServlet.java` — GET `/bin/cif/ccs-config` |
| `list_graphql_clients` (T-49) | `GraphQLProxyServlet` / OSGi service registry | (as T-45) |
| `get_cache_invalidation_status` (T-50) | `CacheInvalidationConfigService` | `skyline/…/cacheinvalidation/internal/CacheInvalidationConfigService.java` |
| `get_feature_activation_status` (T-51) | `CifFeatureActivationService` | `skyline/…/authoring/impl/CifFeatureActivationService.java` |
| `get_custom_headers_config` (T-52) | `HttpHeadersConfigProvider` | `skyline/…/http/HttpHeadersConfigProvider.java` (+ `…/internal/HttpHeadersConfigProviderImpl.java`) |

**Options, not yet decided:**
(a) extend `StoreContextResolver` to also resolve up to a site's config resource, still
gating on being under the site tree; or
(b) build these as a **separate MCP endpoint/module**, potentially living in
`cif-on-skyline` itself, explicitly Cloud-only. Given the packaging-vs-API finding above,
a third option is now visible: (c) reimplement the *config-agnostic* readers (T-44, T-47,
T-48, T-50, T-51, T-52) in `bundles/mcp` against the same underlying OSGi/JCR config, and
defer only the genuinely Cloud-bound proxy tools (T-45, T-46/T-49). This is a bigger
decision than "add a tool" and should be raised with the user before scoping.

---

## 11. Appendix — full tool ID index

Status: **✅ shipped** · **◐ partial** · **▢ planned**. Where the shipped tool differs in
name from the catalog ID, the shipped name is shown.

| ID | Tool (shipped name if different) | Group | Tier | Status |
|---|---|---|---|---|
| — | `configure_product_component` | baseline write | 2 | ✅ |
| — | `search_products` / `get_product` / `get_attributes` / `browse_categories` / `resolve_picker_selection` | baseline read | 1 | ✅ |
| T-01 | `configure_productteaser_component` | §2 Component config | 2 | ▢ |
| T-02 | `configure_productcarousel_component` | §2 | 2 | ◐ (category mode via `configure_productlist_component`) |
| T-03 | `configure_productlist_component` | §2 | 2 | ◐ (category shipped; pageSize/sort/fragments pending) |
| T-04 | `configure_relatedproducts_component` | §2 | 2 | ▢ |
| T-05 | `configure_categorycarousel_component` | §2 | 2 | ▢ |
| T-06 | `configure_featuredcategorylist_component` | §2 | 2 | ▢ |
| T-07 | `configure_product_visible_sections` | §2 | 2 | ▢ |
| T-08 | `configure_page_commerce_links` | §2 | 2 | ◐ (markers via `tag_content_with_commerce`; nav-config pending) |
| T-09 | `get_sort_options` | §3 Metadata reads | 1 | ▢ |
| T-10 | `get_product_variants` | §3 | 1 | ✅ |
| T-11 | `get_product_relationships` | §3 | 1 | ▢ |
| T-12 | `get_category_breadcrumbs` | §3 | 1 | ▢ |
| T-13 | `get_custom_attribute_metadata` | §3 | 1 | ▢ |
| T-14 | `resolve_url_to_entity` | §3 | 1 | ▢ |
| T-15 | `validate_content_bindings` | §3 | 1 | ▢ |
| T-16 | `resolve_category_details` | §3 | 1 | ▢ |
| T-17 | `create_product_teasers` | §4 Bulk components | 3 | ▢ |
| T-18 | `create_product_carousels` | §4 | 3 | ▢ |
| T-19 | `get_product_associated_content` / `get_category_associated_content` | §5 Associated content | 1 | ✅ |
| T-20 | `tag_content_with_commerce` | §5 | 2 | ✅ |
| T-21 | `find_orphaned_commerce_content` | §5 | 1 | ▢ |
| T-22 | `get_commerce_content_fragment` | §6 CF editing | 1 | ◐ (CF read folded into T-19 tools) |
| T-23 | `update_commerce_content_fragment_field` | §6 | 3 | ▢ |
| T-24 | `create_commerce_content_fragment` | §6 | 3 | ▢ |
| T-25 | `list_catalog_pages` | §7 Multi-catalog-page | 1 | ▢ |
| T-26 | `explain_catalog_page_routing` | §7 | 1 | ▢ |
| T-27 | `detect_catalog_page_conflicts` | §7 | 1 | ▢ |
| T-28 | `configure_catalog_page` | §7 | 2 | ◐ (idType hardcoded `uid`; adds `showMainCategories`) |
| T-29 | `bind_page_to_products` | §8 Specific PDP/PLP | 2 | ▢ |
| T-30 | `bind_page_to_category` | §8 | 2 | ▢ |
| T-31 | `bind_product_page_to_category_tree` | §8 | 2 | ▢ |
| T-32 | `unbind_specific_page` | §8 | 2 | ▢ |
| T-33 | `explain_page_resolution` | §8 | 1 | ▢ |
| T-34 | `list_specific_pages` | §8 | 1 | ▢ |
| T-35 | `detect_specific_page_conflicts` | §8 | 1 | ▢ |
| T-36 | `validate_selector_filter_format` | §8 | 1 | ▢ |
| T-37 | `check_specific_page_capability` | §8 | 1 | ▢ |
| T-38 | `create_catalog_page` | §9 Page creation | 3 | ▢ |
| T-39 | `create_specific_pdp` | §9 | 3 | ▢ |
| T-40 | `create_specific_plp` | §9 | 3 | ▢ |
| T-41 | `create_specific_pdp_for_category_tree` | §9 | 3 | ▢ |
| T-42 | `scaffold_catalog_section` | §9 | 3 | ▢ |
| T-43 | `suggest_template_for_page_type` | §9 | 1 | ▢ |
| T-44 | `get_commerce_config` | §10 Deferred | 4 | ▢ |
| T-45 | `validate_graphql_connectivity` | §10 | 4 | ▢ |
| T-46 | `invalidate_cache` | §10 | 4 | ▢ |
| T-47 | `list_store_views` | §10 | 4 | ▢ |
| T-48 | `get_catalog_service_config` | §10 | 4 | ▢ |
| T-49 | `list_graphql_clients` | §10 | 4 | ▢ |
| T-50 | `get_cache_invalidation_status` | §10 | 4 | ▢ |
| T-51 | `get_feature_activation_status` | §10 | 4 | ▢ |
| T-52 | `get_custom_headers_config` | §10 | 4 | ▢ |

**Shipped-but-not-in-this-catalog (shopper suite, see §0):** `add_to_cart`, `view_cart`,
`update_cart_item`, `clear_cart`, `set_shipping_address`, `set_shipping_method`,
`set_payment_method`, `place_order`.

---

## 12. Open questions to resolve before writing an implementation plan

1. **Sequencing:** does the next plan cover one group (e.g. §8 Specific PDP/PLP, the
   deepest and highest-value mechanism) or a cross-group Tier-1-first slice (all
   diagnostics across §5–§9 before any new writes)? Given the shipped baseline (§2/§5/§7
   already have a write tool each), a diagnostics-first slice (§7 T-25–27, §8 T-33–37) is
   now the highest-leverage next step — the writes exist but their resolution/conflict
   behavior is still invisible.
2. **§5 associated content — RESOLVED.** The shipped tools depend on the AEM CIF SDK
   `AssociatedContentService` (`aem-cif-sdk-api`, provided) rather than reimplementing the
   JCR-SQL2 in `bundles/mcp`. This also settles §6's CF read path (same service). Re-open
   only if a future 6.5-only packaging must drop the SDK dependency (then use the §5
   JCR-SQL2 fallback).
3. **§6 CF writes:** confirm variation-handling policy (master-only vs. caller-specified
   variation) before designing `update_commerce_content_fragment_field`'s signature —
   the lookup query only matches `master`, so writes elsewhere are invisible to it.
4. **§9 page creation:** decide whether a dry-run/preview mode or stricter parent-path
   validation is required given the higher blast radius versus Tier 2 tools.
5. **§10:** decide whether global/cross-repo config tools are in scope, and if so where
   they live — now a three-way choice given that only `GraphQLProxyServlet` is provably
   Cloud-bound: extend `StoreContextResolver`, a separate Cloud-only module, or
   reimplement the config-agnostic readers in `bundles/mcp` and defer only the proxy tools.
6. **§7 T-28 delta:** should `configure_catalog_page` grow a `urlPath` idType option (it
   currently hardcodes `magentoRootCategoryIdType=uid`), or is UID-only sufficient for the
   authoring flows in scope?
