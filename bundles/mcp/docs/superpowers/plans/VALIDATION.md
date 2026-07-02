# CIF Commerce MCP — Validation Report

**Date:** 2026-07-02
**Branch:** `mcp` (not pushed, not merged — per RUN_PROMPT.md)
**Plan:** `docs/superpowers/plans/2026-07-02-cif-commerce-mcp.md`
**Spec:** `docs/superpowers/specs/2026-07-02-cif-commerce-mcp-design.md`
**Executed with:** superpowers:subagent-driven-development (fresh implementer + task reviewer per task, TDD, one commit per task, plus a final whole-branch review).

This report records **actually observed** output. Where a check could not be run or a
capability was intentionally deferred, it says so plainly.

---

## 1. Build result

RUN_PROMPT hard-constraint command, at the final commit `bedfe3e1`:

```
mvn -pl bundles/mcp -am clean install   →   BUILD SUCCESS   (exit 0)
```

- **Tests:** `Tests run: 35, Failures: 0, Errors: 0, Skipped: 0` (module `core-cif-components-mcp`).
- **Quality gates green:** `formatter:2.16.0:validate`, `impsort:1.6.2:check`, `macker:1.0.2:macker`, `apache-rat:0.12:check` all passed on the module.
- **Java level:** source/target 8 (inherited from parent, unchanged) → AEM 6.5 compatible. Built under JDK 11 (Maven default `mvn` on this machine).
- The `all` package build (`mvn -pl all -am clean install`) is also green and embeds the bundle (see §5).

### Per-task tests (all TDD, RED→GREEN)
| Suite | Tests |
|---|---|
| JsonRpcTest | 3 |
| McpToolTest | 1 |
| ToolRegistryTest | 1 |
| JsonRpcDispatcherTest | 5 |
| StoreContextResolverTest | 3 |
| AbstractMcpServletTest | 2 |
| ServletRegistrationTest | 2 |
| AuthoringPolicyTest | 2 |
| DtoMapperTest | 5 |
| SearchProductsToolTest | 1 |
| GetAttributesToolTest | 1 |
| GetProductToolTest | 1 |
| BrowseCategoriesToolTest | 1 |
| ResolvePickerSelectionToolTest | 1 |
| ConfigureProductComponentToolTest | 3 |
| ConfigureCatalogPageToolTest | 2 |
| EndToEndTest | 1 |
| **Total** | **35** |

---

## 2. Runtime deployment (http://localhost:4502, admin:admin)

Deployed the built bundle to the running author instance via the Felix console
(`POST /system/console/bundles`, `action=install … bundlestart=start`) — the repo has no
sling-maven deploy profile, and RUN_PROMPT explicitly permits the Felix-console route.

**Bundle state (`/system/console/bundles.json`):**

```
659  Active  2.18.5.SNAPSHOT  com.adobe.commerce.cif.core-cif-components-mcp
```

- **ACTIVE**, not Installed/Resolved. It resolved cleanly against the `core-cif-components-core`
  **2.18.4** bundle present on the instance (my code builds against 2.18.5-SNAPSHOT; OSGi resolves
  on per-package versions, which were compatible — no manifest/import fixes were needed).

**Component states (`/system/console/components.json`):**

```
active     …internal.ToolRegistry
active     …internal.StoreContextResolver
active     …internal.servlets.ShopperMcpServlet
active     …internal.tools.SearchProductsTool
active     …internal.tools.GetAttributesTool
active     …internal.tools.GetProductTool
active     …internal.tools.BrowseCategoriesTool
active     …internal.tools.ResolvePickerSelectionTool
active     …internal.tools.ConfigureProductComponentTool
active     …internal.tools.ConfigureCatalogPageTool
no config  …internal.servlets.AuthoringMcpServlet   ← before authoring config applied
```

The authoring servlet showing **"no config"** with only the bundle installed is the *designed*
behavior: it is `configurationPolicy=REQUIRE` and activates only where its `config.author`
OSGi config exists. This structurally proves the author-only gating (on publish, where the
config is absent, `.mcp-authoring.json` simply will not resolve).

To validate the authoring endpoint on this author instance, the OSGi config for PID
`com.adobe.cq.commerce.mcp.internal.servlets.AuthoringMcpServlet` was created via the Felix
Config Manager (mirroring what the `ui.config` `config.author` package installs on author).
The component then became **active**.

---

## 3. Endpoint checks (CIF nav-root: `/content/venia/us/en`)

The Venia store nav-root is `/content/venia/us/en` (`navRoot` page, confirmed by the endpoint
responding). All requests are JSON-RPC 2.0 POSTs.

### 3.1 `initialize` — shopper `.mcp.json` → HTTP 200 ✅
```json
{"jsonrpc":"2.0","id":1,"result":{"protocolVersion":"2025-06-18",
 "capabilities":{"tools":{}},"serverInfo":{"name":"cif-commerce-mcp","version":"1.0.0"}}}
```
`protocolVersion == "2025-06-18"` and a `tools` capability, as required.

### 3.2 `tools/list` — shopper `.mcp.json` → 5 READ tools ✅
```
browse_categories, get_attributes, get_product, resolve_picker_selection, search_products
```
No write tools are exposed on the anonymous shopper endpoint.

### 3.3 `tools/list` — authoring `.mcp-authoring.json` → 7 tools ✅
```
browse_categories, configure_catalog_page, configure_product_component,
get_attributes, get_product, resolve_picker_selection, search_products
```
The authoring selector adds the two write tools (`configure_product_component`,
`configure_catalog_page`) on top of the read kernel.

### 3.4 `tools/call search_products` (query "top") → structuredContent ✅ (live commerce backend)
```json
{"result":{"content":[{"type":"text","text":"{…}"}],
 "structuredContent":{"total":18,"items":[
   {"sku":"VT12","name":"Jillian Top","slug":"jillian-top","price":58.0,"currency":"USD","imageUrl":"https://mcprod.catalogservice-commerce.fun/…vt12-kh_main.jpg", …},
   {"sku":"VT10","name":"Vitalia Top","price":98.0,"currency":"USD", …},
   {"sku":"VT04","name":"Anna Draped Top","price":88.0,"currency":"USD", …}
 ]}}}
```
Real Venia SKUs, names, prices, currencies, and image URLs — the commerce backend is reachable
and the `SearchResultsService`→DTO mapping works end to end.

### 3.5 `tools/call browse_categories` → structuredContent ✅ (live commerce backend)
```json
{"result":{"structuredContent":{"category":{"uid":"Mg==","name":"Default Category","urlPath":null,
 "children":[
   {"uid":"MjA=","name":"Tops","urlPath":"venia-tops"},
   {"uid":"Mjk=","name":"Bottoms","urlPath":"venia-bottoms"},
   {"uid":"Mzg=","name":"Dresses","urlPath":"venia-dresses"},
   {"uid":"NQ==","name":"Accessories","urlPath":"venia-accessories"},
   {"uid":"NDE=","name":"Shop The Look","urlPath":"shop-the-look"},
   {"uid":"MTc=","name":"New Products","urlPath":"new-products3"}
 ]}}}}
```
Real Venia category tree via the `AbstractCategoryRetriever` subclass.

### 3.6 Authoring write-tool reachability + fail-closed ✅
`.mcp-authoring.json` is reachable on author and exposes the write tools (§3.3). A write call
against a **non-CIF** resource is rejected fail-closed (no mutation occurs):

Request: `tools/call configure_product_component { path:"/content/venia/us/en/jcr:content", sku:"…" }`
```json
{"jsonrpc":"2.0","id":21,"error":{"code":-32000,
 "message":"configure_product_component: resource is not a CIF product component: /content/venia/us/en/jcr:content"}}
```
A well-formed JSON-RPC tool error (`-32000`) — proving both authoring dispatch and the
resource-type guard. Re-reading `/content/venia/us/en/jcr:content` afterward confirmed **no
`selection` property was written** (the guard blocked the write before commit). No real CIF
component was mutated during validation.

### 3.7 Nav-root gate — non-nav-root page → HTTP 404 ✅
```
POST /content/venia/us/en/products.mcp.json  (a real cq:Page, not a nav-root)  →  HTTP 404
```
`/content/venia/us/en/products` is a genuine `cq:Page` that is **not** the nav-root, so the
servlet's `SiteStructure`-based gate returns 404.

> **Honest note on the gate's scope:** the servlets bind `resourceTypes=cq:Page` + selector.
> A request to a path that is *not* a `cq:Page` (e.g. a non-existent page) does not reach the
> MCP servlet at all — Sling's default POST servlet handles it (and, being a POST, will try to
> create a node). During probing, two throwaway paths that were not pages caused Sling to create
> stray nodes under the nav-root; **these were deleted and the nav-root children were verified
> clean afterward.** The 404 nav-root-gate check above was therefore validated against a *real*
> `cq:Page` sub-page (`…/products`), which is the meaningful case. Operators should still deny
> POST/selectors at the dispatcher/CDN as the spec (§7) prescribes.

---

## 4. Final code review (whole branch)

A final whole-branch review (most-capable model) over all 20 commits returned **"Ready to merge
with fixes"**, **no Critical findings**. It confirmed the anonymous-exposure threat model is
airtight (three independent layers: `writesContent()==true` on write tools; `ToolRegistry`
filters the shopper selector and `byName`/`tools/call` returns `-32601` for a hidden tool;
`AuthoringMcpServlet` `configurationPolicy=REQUIRE` + `config.author` → physically absent on
publish), writes use the caller's `ResourceResolver` only, and retrievers are per-call.

Two **Important** findings were raised; disposition:

- **#1 — Write tools were fail-OPEN (no resource-type validation), vs spec §3.6/§5 fail-closed.**
  **FIXED** (commit `bedfe3e1`): both write tools now validate the target via super-type-aware
  `Resource.isResourceType(…)` against the real CIF types
  (`ProductImpl.RESOURCE_TYPE` v1/v2/v3; `SiteStructure.RT_CATALOG_PAGE` / `RT_CATALOG_PAGE_V3`)
  and throw fail-closed otherwise. Two negative tests added; re-reviewed clean; validated live (§3.6).

- **#2 — Shopper commerce-token pass-through (spec §5) is not implemented.**
  **Deferred to v2 as a known limitation** (not silently claimed as working). The request-scoped
  `MagentoGraphqlClient` does not thread an inbound `Authorization`/customer token into
  `RequestOptions`, so the shopper endpoint operates as guest. This is acceptable for v1 because:
  the spec itself lists the exact token header/format as an **open item (§9)**; cart/checkout
  (the primary consumers of a customer token) are explicit **v1 non-goals (§2)**; and the read
  kernel serves public catalog data. Implementing header-threading is a v2 follow-up.

Minor findings (all plan-mandated or cosmetic, left as-is and noted for follow-up): `JsonRpc.parse`
declares broad `throws Exception`; `JsonRpc.result/error` and each servlet allocate throwaway
`ObjectMapper`s; `TOOL_ERROR` message can read `"name: null"` on a null exception message;
`McpProductRetriever` fetches image/price fields the plan listed but `get_product` doesn't map;
`configure_product_component` writes `selectionType=combinedSku` (ignored by the v1 product
component); the 413/405/parse-error transport branches and the write tools' happy paths have unit
coverage but some negative branches were only added where the review required them.

---

## 5. Packaging

`all/pom.xml` embeds `core-cif-components-mcp` next to `core-cif-components-core`. Verified from a
green `mvn -pl all -am clean install` and `unzip -l all/target/*.zip`:

```
jcr_root/apps/core/cif/install/core-cif-components-mcp-2.18.5-SNAPSHOT.jar
```

Same install path as `core-cif-components-core`. The `config.author` OSGi config
(`…AuthoringMcpServlet.cfg.json`) is present in the config sub-package (author-only run mode).

---

## 6. Notable plan deviations (all reviewed and justified)

- **Formatting workflow:** the plan's `mvn formatter:format impsort:sort` (bare goals) does **not**
  pick up the project's execution-scoped `${formatter.config}`, so it formats with Eclipse defaults
  and fails `formatter:validate` (which runs at `verify`, surfaced only after `clean` wipes the
  formatter cache). Correct command is the repo's own profile: `mvn -pl bundles/mcp -Pformat-code
  process-classes`. One corrective commit (`f64f763f`) reformatted the Task-2 files; all later tasks
  used the profile and gated on a real `clean install`.
- **`AuthoringPolicyTest` (Task 17):** the plan's test used `Class.getAnnotation(Component.class)`,
  which is unusable — OSGi DS `@Component` has `RetentionPolicy.CLASS` (invisible at runtime). Rewritten
  to assert the compiler-generated SCR descriptor's `configuration-policy` attribute (the real artifact).
- **Config path (Task 17):** real convention is `ui.config/src/content/…` (not the plan's `src/main/content`).
- **PLP category property (Task 16):** the binding property is `category` (from
  `ProductListImpl.CATEGORY_PROPERTY`), not the plan's tentative `categoryId`.
- Several dependency scopes were promoted `test`→`provided` (`core.wcm.components.core`, `graphql-client`)
  to compile main code — mirroring `bundles/core`; all remain non-bundled `provided`.

---

## 7. Bottom line

- `mvn -pl bundles/mcp -am clean install` → **BUILD SUCCESS**, **35/35 tests**, formatter/impsort/macker/rat clean. ✅
- Bundle **ACTIVE** on the running instance; all components active; author-only servlet gated correctly. ✅
- MCP protocol (`initialize`/`tools/list`/`tools/call`) works over the shopper and authoring endpoints. ✅
- Shopper endpoint exposes only read tools; authoring adds the write tools; write tools fail closed on non-CIF targets (verified live). ✅
- `search_products` and `browse_categories` return real Venia commerce data. ✅
- Nav-root gate returns 404 on a non-nav-root `cq:Page`. ✅
- Known v1 limitation (documented, not hidden): shopper commerce-token pass-through is deferred to v2 (spec §9 open item; cart/checkout are v1 non-goals).

All 20 commits remain on the `mcp` branch. Nothing was pushed or merged.
