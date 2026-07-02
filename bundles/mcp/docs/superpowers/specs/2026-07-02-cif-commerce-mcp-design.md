# CIF Commerce MCP — Design

**Date:** 2026-07-02
**Status:** Approved design (pre-implementation)
**Scope:** A single OSGi bundle in `aem-core-cif-components` that exposes a Model Context Protocol (MCP) server from AEM, over plain (non-Celadon) CIF as shipped to customers.

---

## 1. Goal

Turn a CIF-backed AEM instance into an MCP server so LLM agents can work against commerce data through well-described tools, on both tiers:

- **Publish tier** → shopper-facing *read* tools (catalog search, product detail, category browse, attribute/facet metadata).
- **Author tier** → the same read tools **plus** CIF-specific authoring tools, including **content-config writes** (bind a SKU/category to a component, configure PLP/PDP).

This occupies the *commerce* niche that Adobe's existing generic AEM MCP servers do not cover, and reuses CIF's battle-tested GraphQL stack rather than reimplementing it.

### Non-goals (v1)

- No Celadon / catalog-import functionality (this is plain CIF).
- No cart/checkout tools (v2). The endpoint model already accommodates them: anonymous by storefront parity, honoring a supplied shopper token, with cart state carried as a client-threaded cart-id token.
- No MCP `resources` or `prompts` surface — **tools only** in v1.
- No SSE streaming — plain JSON responses (Streamable HTTP shape, single JSON response).
- No standalone (non-AEM) host — A is hosted in AEM publish, unifying A and B in one bundle.

---

## 2. Decisions locked

| Decision | Choice |
|---|---|
| Hosting of shopper MCP (A) | **In AEM publish**, unified with the authoring MCP (B) as one bundle. |
| MCP protocol layer | **Hand-rolled** JSON-RPC 2.0 subset in a Sling servlet (no external MCP SDK). |
| v1 authoring scope | **Includes content-config writes** (SKU/category binding, PLP/PDP config) under the caller's Sling session. |
| Bundle location | **New module `bundles/mcp`** inside `aem-core-cif-components`, depending on the exported `core` services. |
| Endpoint topology | **Two servlets** (shopper `.mcp.json` / authoring `.mcp-authoring.json`) over a shared `AbstractMcpServlet`, bound to **`cq:Page`** and **gated on the CIF nav-root** (`SiteStructure`) — endpoint is the store-root page, no extra content nodes, version-independent; authoring servlet author-only via `config.author`. |
| Store context | Derived from the **nav-root page** the endpoint is mounted on (no `store` argument); optional `path` override for cross-store lookups. |
| Shopper endpoint auth | **Anonymous** (parity with the public storefront); protection = rate/cost limits; **honors a supplied shopper commerce token** via pass-through. |

---

## 3. Architecture

One OSGi bundle, deployed to author and publish. It exposes **two resource-type-bound servlets** mounted **in the context of a site/store root** (so the endpoint's own resource supplies the CIF commerce context), sharing one protocol/dispatch core:

```
  ┌──────────────────────────────┐        ┌──────────────────────────────┐
  │ Shopper (read) endpoint       │        │ Authoring endpoint             │
  │ POST <navRoot>.mcp.json       │        │ POST <navRoot>.mcp-authoring   │
  │ bind cq:Page + selector mcp   │        │ bind cq:Page + sel mcp-authoring│
  │ gate: SiteStructure navRoot   │        │ gate: SiteStructure navRoot    │
  │ author + PUBLISH · anonymous  │        │ AUTHOR ONLY (config.author)    │
  │ + shopper-token pass-through  │        │ AEM auth + JCR ACLs            │
  └───────────────┬──────────────┘        └───────────────┬──────────────┘
                  │      both extend AbstractMcpServlet     │
                  └───────────────────┬────────────────────┘
                                      ▼
                       ┌──────────────────────────┐
                       │ JsonRpcDispatcher          │ initialize / tools.list / tools.call
                       └──────────────┬────────────┘
                       ┌──────────────▼────────────┐
                       │ ToolRegistry               │ each servlet mounts a fixed tool set
                       └──────────────┬────────────┘
        ┌───────────────────────────┼───────────────────────────┐
 ┌──────▼───────┐          ┌─────────▼─────────┐        ┌────────▼─────────┐
 │ Read kernel   │          │ Authoring tools    │        │ Store context    │
 │ (both endpts) │          │ (author endpt only)│        │ = endpoint's own │
 │ search_products│         │ resolve_picker_…   │        │ Resource/request │
 │ get_product    │         │ configure_product  │        │ (+ optional path │
 │ browse_categs  │         │ configure_plp/pdp  │        │  override)       │
 │ get_attributes │         └─────────┬──────────┘        └────────┬─────────┘
 └──────┬────────┘                    │ writes JCR via caller      │ resolves ctx-aware
        │ reads                       │ Sling session (ACLs)       │ commerce config
        ▼                             ▼                            ▼
 SearchResultsService /       ResourceResolver /           MagentoGraphqlClient /
 SearchFilterService /        ModifiableValueMap           GraphqlClient (live OSGi
 retrievers (core-cif)        (core-cif page/component)     service, caching + CB)
```

### 3.1 Transport — two servlets bound to `cq:Page`, gated on the CIF nav-root

The endpoint is mounted **on the store root page itself**, identified by CIF's own navigation-root concept, so no extra content nodes and no coupling to versioned page component types.

**Registration** (both servlets extend `AbstractMcpServlet`, differing only by selector):

```
sling.servlet.resourceTypes = cq:Page        # node type → version-independent (not …/page/v3/page)
sling.servlet.extensions    = json
sling.servlet.methods       = POST
sling.servlet.selectors     = mcp            # shopper;  authoring servlet uses: mcp-authoring
```

**Nav-root gate:** Sling registration cannot filter by a property, so binding is broad (`cq:Page` + selector) and the servlet gates in code using CIF's public API — `SiteStructure.getLandingPage()` (equality with the requested page) / `SiteNavigation` `navRoot`. A request to a non-nav-root page returns `404`. This is the same navRoot-gating pattern CIF already uses (`ShowNavRootRenderConditionServlet`, `SiteStructureImpl`), so it is idiomatic and self-rejects cheaply. Verified on the running instance: `navRoot=true` is present on the store roots (`/content/venia/us/en`, `language-masters/en`) and absent on sub-pages.

| | Shopper (read) servlet | Authoring servlet |
|---|---|---|
| Binding | `cq:Page` + selector `mcp` | `cq:Page` + selector `mcp-authoring` |
| Client URL | `POST <navRoot>.mcp.json` (e.g. `/content/venia/us/en.mcp.json`) | `POST <navRoot>.mcp-authoring.json` |
| Instances | author **+** publish | **author only** (component config under `config.author`, the existing run-mode pattern) |
| Tools | read kernel only | read kernel + authoring/write tools |
| Auth | anonymous (storefront parity) + shopper-token pass-through | AEM auth + JCR ACLs |

The distinct **selectors** let both servlets bind `cq:Page`+`POST` without a resolution conflict, and keep the endpoints off Sling's default JSON renderer.

`AbstractMcpServlet` owns the protocol:
- Minimal MCP method set: `initialize` (protocol/version + capabilities = `{ tools: {} }`), `tools/list`, `tools/call`. Unknown methods → `-32601`.
- Single JSON response per request (no SSE in v1); batch requests out of scope.
- `application/json`; rejects `GET`; enforces max request size.

### 3.2 JSON-RPC layer (hand-rolled)

- `JsonRpcDispatcher` parses the envelope, validates `jsonrpc`/`method`/`id`, routes to a handler, and maps exceptions to JSON-RPC errors (`-32700` parse, `-32600` invalid request, `-32601` method not found, `-32602` invalid params, `-32603` internal, plus a reserved application range for tool errors).
- Jackson (already on the AEM classpath) for (de)serialization. No external MCP SDK.

### 3.3 Tool registry + per-servlet tool sets

- Each tool implements a small `McpTool` SPI: `name()`, `title()/description()`, `inputSchema()` (JSON Schema as a Jackson tree), `writesContent()` (marker), and `call(McpCallContext, args)`.
- Tools are OSGi services; `ToolRegistry` collects them.
- **The tool set is defined by which servlet you hit**, not by a runtime profile filter:
  - Shopper servlet → read kernel only. It must expose **only public-storefront-equivalent operations** (see §5); this is the constraint that makes anonymous exposure safe.
  - Authoring servlet → read kernel + authoring/write tools.
- **Structural safety, defense in depth:**
  - The authoring servlet's OSGi component config lives under `config.author`, so it is **not registered on publish** at all (write tools are physically absent, not merely filtered).
  - The `writesContent()` marker is a belt-and-suspenders guard: a write tool can never be mounted on the shopper servlet, and `tools/call` for a tool not in the hit servlet's set returns `-32601`.
- `tools/list` returns exactly the hit servlet's set.

### 3.4 Read-tool kernel (shared, both tiers)

Two distinct reuse tracks — **faceted search** goes through `SearchResultsService`; **point lookups** go through CIF's public retriever abstraction. They solve different problems and are not interchangeable (retrievers do not do facets/aggregations/paging).

**Faceted search → `SearchResultsService` / `SearchFilterService`:**

| Tool | Delegates to | Result (compact) |
|---|---|---|
| `search_products` | `SearchResultsService.performSearch(...)` with a built `SearchOptions` | list of `{sku, name, price, currency, urlKey, thumbnail}` + paging + facets |
| `get_attributes` | `SearchFilterService` | filterable attributes + `FilterAttributeMetadata` |

`SearchOptions` is built from tool args: `getSearchQuery` ← `query`, `getCurrentPage`/`getPageSize` ← paging, `getAttributeFilters` ← `filters`, sorter ← `sort`.

**Point lookups → public retriever abstraction (`components.models.retriever.Abstract*`):**

| Tool | Delegates to | Result (compact) |
|---|---|---|
| `get_product` | `AbstractProductRetriever` (by SKU / url_key) | single product summary + key attributes |
| `browse_categories` | `AbstractCategoryRetriever` / `AbstractCategoriesRetriever` | category node(s) `{uid, name, urlPath, children[]}` |

The retrievers are constructed with the `MagentoGraphqlClient` from `StoreContextResolver`, so they inherit the correct endpoint/store-view/caching. Their `extendProductQueryWith(...)` / query-customization hooks let each tool request exactly the fields it maps (no over-fetch). See §3.7 for reuse constraints. `resolve_picker_selection` (§3.6) is batched point lookups and reuses the same `Abstract*` retrievers.

### 3.5 Store context — from the endpoint resource itself

The existing read services are **context-bound**: `SearchResultsService.performSearch(...)` requires a `Resource` and a `SlingHttpServletRequest`, and `MagentoGraphqlClient` is an adapter over `Resource`/`SlingHttpServletRequest` that reads the CIF **context-aware commerce configuration** from the page tree.

Because the servlet is **mounted on the nav-root page** (§3.1), this is solved by construction: the request's own resource **is** the store-root page (the one CIF marks `navRoot=true`), which already carries the commerce config — exactly how storefront components resolve it. So:

1. The store context is the **endpoint's own `(resource, request)`** — no `store` argument, no synthetic request, no default-store lookup for the common case.
2. `StoreContextResolver` shrinks to: hand the servlet's `(resource, request)` to `SearchResultsService` / `MagentoGraphqlClient`, and expose an **optional `path` override** for cross-store lookups (resolve another nav-root resource within allowed content roots).
3. Multi-store falls out of the URL: `<navRootA>.mcp.json` and `<navRootB>.mcp.json` are naturally distinct stores.

This keeps all endpoint/store-view/auth/caching/circuit-breaker behavior identical to the storefront — no second commerce client, and no context-synthesis code path.

### 3.6 Authoring tools (author tier only, write-capable)

All writes run under the **caller's** `ResourceResolver` (from the authenticated author request), so JCR ACLs are enforced naturally.

| Tool | Action |
|---|---|
| `resolve_picker_selection` | Given SKU(s) / category UID(s), return the display data an author picker needs (title, image, price, path). *Read-only.* |
| `configure_product_component` | Set the commerce selection properties on a target component resource (e.g. product SKU / selection type) via `ModifiableValueMap`. *Write.* |
| `configure_catalog_page` | Configure a PLP/PDP page's commerce properties (category binding / page type). *Write.* |

Write tools validate the target path, confirm the resource type is a CIF component/page they understand, and fail closed on anything else. Each write tool declares `writesContent() == true` so it can never be mounted on the shopper endpoint (and the authoring servlet, which carries them, is absent on publish).

### 3.7 Retriever reuse constraints

The point-lookup tools reuse CIF's retriever abstraction, subject to:

1. **Use the exported `Abstract*` bases, not the concrete `internal` retrievers.** The concrete `ProductRetriever`/`CategoryRetriever` classes live in non-exported `internal.models.v1.*` packages and are component-coupled. The MCP module adds tiny concrete subclasses of `AbstractProductRetriever` / `AbstractCategoryRetriever` / `AbstractCategoriesRetriever` — the intended extension pattern.
2. **Retrievers are single-use and stateful** (`populate()` caches results; one query per instance). Instantiate **per tool call**; never share as a singleton or across `store` contexts.
3. Retrievers return typed `magento-graphql` objects (`ProductInterface`, `CategoryInterface`); the tool maps these to the compact DTOs of §6.
4. Use the query-customization hooks to fetch only the mapped fields, avoiding over-fetch.

---

## 4. Data flow (search example)

```
agent → POST /content/venia/us/en.mcp.json         (store context = this resource)
        tools/call { name: "search_products",
                     arguments: { query: "yoga", filters: {...}, page: 1, pageSize: 12 } }
        [optional Authorization: Bearer <shopper commerce token>]
 → Shopper servlet → JsonRpcDispatcher → ToolRegistry(search_products)
 → store context = servlet (resource, request); shopper token (if any) → RequestOptions headers
 → SearchOptions built from args
 → SearchResultsService.performSearch(options, resource, request)
 → map SearchResultsSet → compact JSON
 → JSON-RPC result { content: [{ type: "text", text: <json> }] , structuredContent: {...} }
```

---

## 5. Security & identity

### Shopper endpoint (publish + author) — anonymous, by parity with the storefront

The MCP shopper endpoint is **just another client of APIs the site already exposes anonymously** (catalog, search, and — later — guest cart / anonymous checkout). It reveals nothing a browser on that site cannot already reach, so a bespoke endpoint token is not required and is deliberately **not** used. Two guardrails make that parity argument hold:

- **Tool set constrained to public-storefront-equivalent operations only.** The anonymous endpoint may expose only what the storefront already does anonymously; anything beyond that does not belong on it (§3.3).
- **Cost limits replace authN as the protection:** rate limiting at CDN/dispatcher, max page size, query depth/complexity caps, plus the existing `GraphqlClient` caching + circuit breaker guard against abuse/DoS.

**Honor the logged-in shopper when present (token pass-through, not gating):** the endpoint is anonymous-open but token-aware. If the client supplies the shopper's commerce/customer token (e.g. `Authorization` header), it is threaded into the request via `RequestOptions` / `getHttpHeaderMap()` so cart, customer, and customer-scoped pricing act on the right identity. No token → guest context, exactly like an anonymous storefront visitor. (Guest cart is stateful in Magento: cart tools — v2 — take and return a cart-id token so the client threads state across calls.)

### Authoring endpoint (author only)

- Runs under the **authenticated author request**; not registered on publish at all (`config.author`).
- Read tools may resolve a **service** `ResourceResolver` (read-only, restricted subservice) for config resolution; **write** tools use the caller's `ResourceResolver` so JCR **ACLs** enforce every write. No privilege escalation via the MCP layer.

### Both endpoints

All tool inputs are schema-validated before dispatch; any `path` override is normalized and confined to allowed content roots.

---

## 6. Result shaping

Tool results are compact, token-efficient JSON DTOs (only fields an agent needs) rather than raw GraphQL. Each `tools/call` result returns both a human-readable `content` text block and `structuredContent` for programmatic use. Raw GraphQL payloads are never passed through verbatim.

---

## 7. Packaging, exposure & build

**Bundle**
- New Maven module `bundles/mcp` in `aem-core-cif-components`, depending on the exported `core` services (`SearchResultsService`, `SearchFilterService`, retrievers, `MagentoGraphqlClient`) and on `magento-graphql` + `graphql-client` (already transitive).
- Same JDK/build/style config as the rest of core-cif-components (JDK 11 compile target 8; Eclipse formatter + impsort; license headers).
- Bundled into the `all` package alongside the existing `core` bundle; ships with the CIF Core Components release train.

**Exposure**
- Two servlets bound to `cq:Page` (§3.1), extension `json`, method `POST`, differing by selector: shopper `mcp`, authoring `mcp-authoring`. Both gate on the CIF nav-root via `SiteStructure`.
- No dedicated endpoint content nodes: the endpoint is the existing store-root (nav-root) page. Nothing extra to author or replicate.
- Authoring servlet component registered only via OSGi config under `config.author` → structurally absent on publish (so `.mcp-authoring.json` simply does not resolve there).
- **Dispatcher/CDN (publish):** add one narrow allow rule for the shopper endpoint (POST + selector `mcp` + extension `json` on nav-root paths), since dispatcher denies POST/selectors by default; apply rate limiting there. `.mcp-authoring.json` is denied at the CDN as defense in depth (and is absent on publish anyway).

---

## 8. Testing

- **Per-tool unit tests** with `aem-mock` (already used in core-cif-components): mock `SearchResultsService` etc., assert `SearchOptions` mapping and compact-result mapping.
- **JSON-RPC contract tests**: malformed envelope, unknown method, invalid params, error-code mapping; `GET` rejected.
- **Endpoint / tool-set tests**: shopper servlet exposes only read tools and rejects a write-tool `tools/call` (`-32601`); authoring servlet exposes both; authoring component absent under publish run mode.
- **Nav-root gate tests**: a nav-root page (`navRoot=true`) serves the endpoint; a non-nav-root page (e.g. `.../products`) returns `404`; gating goes through `SiteStructure`.
- **Store-context tests**: context resolves from the endpoint's own nav-root resource; optional `path` override confined to allowed roots; cross-store lookup works.
- **Shopper-token pass-through tests**: supplied token reaches `RequestOptions`/headers; absent token → guest context.
- **Write-tool tests**: ACL-respecting write via caller session; fail-closed on unexpected resource type / disallowed path.

---

## 9. Open items to resolve during implementation

- Exact shopper commerce-token header name/format and how it maps onto `RequestOptions` headers.
- Cost-limit specifics (rate-limit tier, max page size, query-complexity cap) and where enforced (servlet vs. dispatcher/CDN).
- Allowed content roots for the optional cross-store `path` override.

---

## 10. Reuse summary (why this is cheap and safe)

| Need | Existing asset reused |
|---|---|
| Typed GraphQL queries/mutations | `commerce-cif-magento-graphql` (pure Java, portable) |
| HTTP + caching + circuit breaker | `graphql-client` `GraphqlClient` (live OSGi service) |
| Context-aware commerce config | `MagentoGraphqlClient` adapter (Resource/request) |
| Store-root identification | `SiteStructure` / `SiteNavigation` (CIF navRoot concept) |
| Product search + facets + paging | `SearchResultsService`, `SearchOptions`, `SearchResultsSet` |
| Point lookups (product/category by id) | public `Abstract*Retriever` (thin MCP subclasses) |
| Filter/attribute metadata | `SearchFilterService`, `FilterAttributeMetadata` |
| Author content writes | Sling `ResourceResolver` / `ModifiableValueMap` under caller session |
| Test harness | `aem-mock` (already in the module) |

Net-new code is confined to: the two servlets over a shared `AbstractMcpServlet` + JSON-RPC layer, the tool registry, the thin tool handlers, and the (now-thin) store-context helper.
