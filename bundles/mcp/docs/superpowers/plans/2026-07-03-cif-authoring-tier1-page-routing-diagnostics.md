# CIF Authoring Tier-1 Page-Routing Diagnostics — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Ship the 8 Tier-1 read/diagnostic tools for CIF page routing from `AUTHORING_TOOLS_CATALOG.md` — §7 multi-catalog-page (T-25/26/27) and §8 specific-PDP/PLP (T-33/34/35/36/37) — so an authoring agent can see and explain which catalog/PDP/PLP page wins for a given category/product, and detect routing conflicts, BEFORE the Tier-2 write tools that create those bindings exist.

**Architecture:** All 8 are read tools (`writesContent()==false` → both endpoints), OSGi `@Component(service = McpTool.class)`, auto-discovered by `ToolRegistry`. They do **not** hit GraphQL; they read the AEM page tree and replay CIF's routing logic. Two shared internal helpers hold the logic so the 8 tools are thin: `CatalogPageRouting` (§7) reuses the EXPORTED `SiteStructure` API; `SpecificPageRouting` (§8) REIMPLEMENTS the non-exported `SpecificPageStrategy` traversal (redeclaring its binding constants as local literals). Tools return compact Jackson DTOs.

**Tech Stack:** Java 8 / JDK 11, OSGi DS, Jackson, CIF `SiteStructure` + `UrlProvider` (exported, `provided`), Sling `Resource`/`Page` tree APIs, aem-mock + Mockito + JUnit 4.

## Global Constraints

*(From `bundles/mcp/AGENTS.md` — every task implicitly includes these.)*

- **AEM 6.5 compatible. Java source/target 8, built under JDK 11.** No `var`/`List.of`/records/switch-expressions; no API newer than `bundles/core`. `javax` never `jakarta`.
- **Build/verify with `mvn -pl bundles/mcp clean install`** (the `clean` matters). `mvn test` alone is insufficient. Build under JDK 11: `export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-11.jdk/Contents/Home`; mvn at `/Users/levente/devel/prg/maven/bin/mvn`. If `-pl bundles/mcp` fails on missing reactor deps use `-pl bundles/mcp -am`.
- **Format with `mvn -pl bundles/mcp -Pformat-code process-classes`** before committing.
- **Apache 2.0 license header on every new `.java`** (copy verbatim from a sibling). **Explicit imports, no wildcards.** **Jackson only.** No external MCP SDK.
- **New code under `com.adobe.cq.commerce.mcp.internal[.tools]`** (helpers live in `…internal`, not `…internal.tools`, unless naturally tool-local); exported `…mcp` must not reference `…mcp.internal.*`.
- **Reuse CIF where exported; reimplement only where forced.** Compact DTOs, never raw JCR ValueMaps in output.
- **READ tools must NOT override `writesContent()`.** Keep the JSON-RPC envelope/error-codes/protocol-version/result-shape stable.
- **Conventional commits `feat(mcp):`/`test(mcp):`, one logical change per commit, `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>` trailer.** Stage only the files the task touches.

## Reuse-vs-reimplement boundary (source-verified — the crux of this plan)

- **REUSE (exported, importable from bundles/mcp at `provided` scope):**
  `com.adobe.cq.commerce.core.components.models.common.SiteStructure` — obtain via `request.adaptTo(SiteStructure.class)` or `page.adaptTo(...)` (see `StoreContextResolver` for the existing adaptTo entry point). Methods (verify exact signatures/return types against `SiteStructure.java` before use): `getLandingPage()`, `getCategoryPages()`, `getProductPages()`, `getSearchResultsPage()`, `getEntry(Page)`, `isCatalogPage(Page)`/`isProductPage`/`isCategoryPage`, and `SiteStructure.Entry` with `getPage()`/`getCatalogPage()`. Constants `RT_CATALOG_PAGE`, `RT_CATALOG_PAGE_V3`, `PN_NAV_ROOT`.
  > Naming note: what the catalog §7 calls "catalog pages" are `SiteStructure.getCategoryPages()` (the category/PLP pages, resourceType `RT_CATALOG_PAGE`). Use the real method name.
- **REIMPLEMENT (internal, NOT importable):** `…core.components.internal.services.SpecificPageStrategy` and its binding constants, plus the catalog-scope property names from `SiteStructureImpl`. Redeclare these literals in `bundles/mcp` (a `PageRoutingConstants` holder or private constants):
  - `magentoRootCategoryId`, `magentoRootCategoryIdType` (special value `"urlPath"`; any other ⇒ UID/generic).
  - `selectorFilter`, `selectorFilterType` (default `"uidAndUrlPath"`), `includesSubCategories`, `useForCategories`, separator `"|"`.
  - Product-page structure resourceType base `core/cif/components/structure/page` (product/category pages); catalog page RTs come from `SiteStructure` constants.
- **ALGORITHM ORACLE:** mirror behavior from `bundles/core`'s `SpecificPageStrategyTest` and `SiteStructureImplTest` (read them). Catalog `AUTHORING_TOOLS_CATALOG.md` §7 and §8.1 document the algorithm precisely — **each task below cites the subsection; read it.**

## Reference material (read before starting)
- Spec: `bundles/mcp/docs/superpowers/AUTHORING_TOOLS_CATALOG.md` §7 (catalog pages) and §8 (specific pages) — mechanism, constants, gotchas, resolution algorithm.
- Module guide: `bundles/mcp/AGENTS.md` §3 (read tool), §4 (fail-closed validation for path args — these read tools still validate `path` args live under `/content` and resolve to a real `cq:Page`), §6 (testing), §8 (pitfalls).
- Existing tool/test templates: this branch's `internal/tools/*Tool.java` (e.g. `ResolveCategoryDetailsTool`, `ValidateContentBindingsTool`) for tool shape + seams; `StoreContextResolver.java` for the `SiteStructure` adaptTo entry point; `bundles/core` `SpecificPageStrategyTest`/`SiteStructureImplTest` for algorithm behavior.

---

## Shared conventions (every task)

**Tool skeleton:** read tool, do NOT override `writesContent()`; `StoreContext ctx = (StoreContext) context;`. Validate args → `IllegalArgumentException` on bad input (dispatcher → -32000). Tools that take a `path`/`page`/`siteRoot` arg must resolve it via `ctx.getRequest().getResourceResolver()`, require it under `/content`, and require it to adapt to `com.day.cq.wcm.api.Page` — else `IllegalArgumentException` (fail closed). Tools without a `siteRoot` arg default to the endpoint's own nav root (`ctx.getLandingPage()`).

**Test seam / testability:** these tools read the JCR tree, so tests use aem-mock (`AemContext`) with JCR fixtures in `src/test/resources/context/*.json` loaded via `context.load().json("/context/x.json", "/content")` (AGENTS.md §6 — top-level keys become children of the load path). Build small page-tree fixtures (nav root + a couple of catalog/product/category pages with the binding properties) and assert the tool's JSON output. Where a tool needs `SiteStructure`, adapt the fixture request/page (aem-mock supports Sling-model adaptation when `bundles/core` models are registered — register them in the test context as existing tests do; check a `bundles/core` model test for the `AemContext` setup). If adapting `SiteStructure` in aem-mock proves impractical, add a `protected` seam returning the catalog/product/category `List<Page>` so the test supplies pages directly — mirror the seam approach used by the §3 tools.

**Per-task TDD loop:** (1) failing test; (2) RED `mvn -q -pl bundles/mcp test -Dtest=XxxTest`; (3) implement; (4) GREEN; (5) green gate `-Pformat-code process-classes` then `clean install`; (6) commit.

**File structure:**
- `internal/CatalogPageRouting.java` (+ test) — §7 helper (Task 1).
- `internal/SpecificPageRouting.java` (+ test) — §8 helper (Task 5).
- `internal/tools/ListCatalogPagesTool.java`, `ExplainCatalogPageRoutingTool.java`, `DetectCatalogPageConflictsTool.java` (§7).
- `internal/tools/ExplainPageResolutionTool.java`, `ListSpecificPagesTool.java`, `DetectSpecificPageConflictsTool.java`, `ValidateSelectorFilterFormatTool.java`, `CheckSpecificPageCapabilityTool.java` (§8).
- Each with its `*Test.java` and JCR fixture(s) under `src/test/resources/context/`.

---

### Task 1: `CatalogPageRouting` helper (§7 shared logic)

Catalog §7. Reuses `SiteStructure` (exported); reimplements only the generic-vs-`urlPath` first-match resolution.

**Files:** Create `internal/CatalogPageRouting.java`; Test `internal/CatalogPageRoutingTest.java` + fixture `src/test/resources/context/catalog-pages.json`.

**Interfaces — Produces (tools in Tasks 2-4 consume these):**
- `static final class CatalogPageInfo { Page page; String path; String rootCategoryId; String idType; boolean genericFallback; }` (or expose via getters).
- `List<CatalogPageInfo> listCatalogPages(SiteStructure site)` — from `site.getCategoryPages()` in order, reading `jcr:content` props `magentoRootCategoryId`/`magentoRootCategoryIdType`; the nav-root/landing page is the generic fallback appended last (`genericFallback=true`, per §7). Mark an entry generic when `idType != "urlPath"` or `rootCategoryId` is blank.
- `CatalogPageResolution resolveFor(SiteStructure site, String categoryUrlPath)` — replays `getGenericPage`: iterate the ordered list, return the FIRST entry that is generic OR whose `urlPath` scope contains `categoryUrlPath` (a page with `idType="urlPath"` scopes `rootCategoryId` and all sub-paths, i.e. `categoryUrlPath.equals(scope) || categoryUrlPath.startsWith(scope + "/")`). Return the winner + the ordered evaluation trace (each candidate: matched? why).

- [ ] **Step 1: failing test** — fixture: nav root with 2 category pages (one `idType=urlPath rootCategoryId=venia-tops`, one generic) + nav root as fallback. Assert `listCatalogPages` order + parsed props + `genericFallback` flags; assert `resolveFor("venia-tops/venia-blouses")` returns the urlPath-scoped page (sub-path match) and `resolveFor("venia-bottoms")` falls through to the generic fallback, with a correct trace.
- [ ] **Step 2: RED** (`-Dtest=CatalogPageRoutingTest`).
- [ ] **Step 3: implement** — verify `SiteStructure.getCategoryPages()`/`Entry` accessors against source first; redeclare `magentoRootCategoryId`/`magentoRootCategoryIdType` literals; implement listing + first-match resolution per §7.
- [ ] **Step 4: GREEN.**
- [ ] **Step 5: green gate + commit** (`feat(mcp): add CatalogPageRouting helper for catalog-page diagnostics`).

---

### Task 2: `list_catalog_pages` (T-25)

Catalog §7. Thin wrapper over `CatalogPageRouting.listCatalogPages`.

**Files:** Create `internal/tools/ListCatalogPagesTool.java`; Test + reuse `catalog-pages.json`.

**Interfaces:** Consumes Task 1. Produces tool `list_catalog_pages`; args `{ "siteRoot": string (optional; default endpoint nav root) }`; result `{"siteRoot":"…","catalogPages":[{"path":"…","rootCategoryId":"…","idType":"…","genericFallback":true|false}]}` in `SiteStructure` order (generic fallback last).

- [ ] Step 1 failing test (assert order + fields + fallback). Step 2 RED. Step 3 implement (resolve `siteRoot` arg fail-closed, or default nav root; call helper; map DTO). Step 4 GREEN. Step 5 green gate + commit (`feat(mcp): add list_catalog_pages read tool`).

---

### Task 3: `explain_catalog_page_routing` (T-26)

Catalog §7. Replays `resolveFor` and reports the winner + why.

**Files:** Create `internal/tools/ExplainCatalogPageRoutingTool.java`; Test + fixture reuse.

**Interfaces:** Consumes Task 1 (`resolveFor`). Produces tool `explain_catalog_page_routing`; args `{ "urlPath": string (required — category url_path), "siteRoot": string (optional) }`; result `{"identifier":"…","winningPage":"…","reason":"generic-fallback|urlPath-scope-match","candidates":[{"path":"…","matched":true|false,"why":"…"}]}`.

- [ ] Step 1 failing test (a sub-path resolves to the scoped page; a non-matching path resolves to fallback; trace correct). Step 2 RED. Step 3 implement. Step 4 GREEN. Step 5 commit (`feat(mcp): add explain_catalog_page_routing read tool`).

---

### Task 4: `detect_catalog_page_conflicts` (T-27)

Catalog §7. Flags overlapping `urlPath` scopes among catalog pages (ambiguous routing).

**Files:** Create `internal/tools/DetectCatalogPageConflictsTool.java`; Test + fixture (add two pages with overlapping urlPath scopes, e.g. `venia-tops` and `venia-tops/venia-blouses`, or two pages both scoping `venia-tops`).

**Interfaces:** Consumes Task 1 (`listCatalogPages`). Produces tool `detect_catalog_page_conflicts`; args `{ "siteRoot": string (optional) }`; result `{"siteRoot":"…","overlaps":[{"pages":["…","…"],"scope":"…","kind":"duplicate-scope|ancestor-descendant"}]}`. Scope creep guard: this tool does structural overlap detection over the catalog pages' own scope props only — do NOT fetch the category tree (dead-link detection against the live catalog is deferred, note in the tool description).

- [ ] Step 1 failing test (two overlapping scopes → one overlap entry; disjoint scopes → none). Step 2 RED. Step 3 implement (compare each pair of non-generic urlPath scopes for equality or ancestor/descendant containment). Step 4 GREEN. Step 5 commit (`feat(mcp): add detect_catalog_page_conflicts read tool`).

---

### Task 5: `SpecificPageRouting` helper (§8 shared logic — the highest-risk task)

Catalog §8.1. REIMPLEMENTS `SpecificPageStrategy` traversal. **Read `SpecificPageStrategyTest` in `bundles/core` as the behavioral oracle before implementing.**

**Files:** Create `internal/SpecificPageRouting.java`; Test `internal/SpecificPageRoutingTest.java` + fixture `src/test/resources/context/specific-pages.json` (nav root → product page + nested product pages with `selectorFilter`/`useForCategories`; category pages with `selectorFilter`+`includesSubCategories`).

**Interfaces — Produces (Tasks 6-10 consume):**
- `static final class Binding { String pageType /*product|category*/; String[] selectorFilter; String selectorFilterType; boolean includesSubCategories; String[] useForCategories; }`
- `static final class ParsedFilter { String raw; boolean valid; String uid; String urlPath; String issue; }`
- `List<Page> specificPages(Page searchRoot)` — depth-first descendants of `searchRoot` that are product/category structure pages with a non-empty `selectorFilter` OR `useForCategories` (candidate = `isSpecificPage`).
- `Binding readBinding(Page page)` — reads the raw binding properties (redeclared constants), `selectorFilterType` defaulting to `"uidAndUrlPath"`.
- `List<ParsedFilter> parseSelectorFilter(Binding b)` — when `selectorFilterType=="uidAndUrlPath"`, split each entry on `"|"` into `uid|urlPath`; a missing pipe ⇒ `valid=false, issue="missing '|' separator"` (ambiguous fallback per §8). Otherwise treat entries as plain url-paths.
- `Resolution resolveSpecificPage(Page searchRoot, String urlPath, String type)` — mirror `getSpecificPage`: depth-first where **descendants precede ancestors**, `findFirst()` ⇒ **deepest match wins**; subtree predicate: `categoryUrlPath.equals(given) || (includesSubCategories && given.startsWith(categoryUrlPath + "/"))`; product pages match by their `selectorFilter` slugs / `useForCategories` scope. Return winner + **its tree depth** + ordered candidate trace.

- [ ] **Step 1: failing test** — mirror `SpecificPageStrategyTest`'s nested cases: a nested specific product page deeper in the tree wins over a shallower matching one (deepest-wins, NOT filter-specificity); `includesSubCategories` subtree match; malformed filter (no pipe) flagged by `parseSelectorFilter`. Assert winner + depth + trace.
- [ ] **Step 2: RED** (`-Dtest=SpecificPageRoutingTest`).
- [ ] **Step 3: implement** — redeclare constants; traversal emitting `traverse(child)` before the node so descendants precede parents; `findFirst`. Cross-check semantics against `SpecificPageStrategyTest`.
- [ ] **Step 4: GREEN.**
- [ ] **Step 5: green gate + commit** (`feat(mcp): add SpecificPageRouting helper for specific-page diagnostics`).

---

### Task 6: `explain_page_resolution` (T-33)

Catalog §8. Replays `resolveSpecificPage`, reports the winner **and its tree depth**.

**Files:** Create `internal/tools/ExplainPageResolutionTool.java`; Test + reuse `specific-pages.json`.

**Interfaces:** Consumes Task 5. Produces tool `explain_page_resolution`; args `{ "identifier": string (required — a category url_path or product slug/url_key), "type": "product"|"category" (required), "siteRoot": string (optional) }`; result `{"identifier":"…","type":"…","winningPage":"…","depth":N,"candidates":[{"path":"…","depth":N,"matched":true|false,"why":"…"}]}`.

- [ ] Step 1 failing test (deepest of two matching pages wins, depth reported). Step 2 RED. Step 3 implement. Step 4 GREEN. Step 5 commit (`feat(mcp): add explain_page_resolution read tool`).

---

### Task 7: `list_specific_pages` (T-34)

Catalog §8. Lists every descendant page with a non-empty binding + what it binds.

**Files:** Create `internal/tools/ListSpecificPagesTool.java`; Test + reuse fixture.

**Interfaces:** Consumes Task 5 (`specificPages`, `readBinding`, `parseSelectorFilter`). Produces tool `list_specific_pages`; args `{ "siteRoot": string (optional) }`; result `{"siteRoot":"…","specificPages":[{"path":"…","pageType":"product|category","selectorFilter":[…],"selectorFilterType":"…","includesSubCategories":bool,"useForCategories":[…]}]}` (omit empty binding fields).

- [ ] Step 1 failing test. Step 2 RED. Step 3 implement. Step 4 GREEN. Step 5 commit (`feat(mcp): add list_specific_pages read tool`).

---

### Task 8: `detect_specific_page_conflicts` (T-35)

Catalog §8. Flags identical-scope duplicates and structural shadowing (a narrower binding at ≤ the depth of a broader one).

**Files:** Create `internal/tools/DetectSpecificPageConflictsTool.java`; Test + fixture (two pages binding the same scope; a shadowing pair).

**Interfaces:** Consumes Task 5. Produces tool `detect_specific_page_conflicts`; args `{ "siteRoot": string (optional) }`; result `{"siteRoot":"…","duplicates":[{"scope":"…","pages":["…","…"]}],"shadowing":[{"broader":"…","narrower":"…","reason":"…"}]}`.

- [ ] Step 1 failing test (duplicate-scope pair detected; shadowing pair detected; clean tree → empty). Step 2 RED. Step 3 implement. Step 4 GREEN. Step 5 commit (`feat(mcp): add detect_specific_page_conflicts read tool`).

---

### Task 9: `validate_selector_filter_format` (T-36)

Catalog §8. Catches malformed `uid|urlPath` (missing pipe → ambiguous fallback).

**Files:** Create `internal/tools/ValidateSelectorFilterFormatTool.java`; Test + fixture (a page with one well-formed and one pipe-less `selectorFilter` entry).

**Interfaces:** Consumes Task 5 (`readBinding`, `parseSelectorFilter`). Produces tool `validate_selector_filter_format`; args `{ "path": string (required — a specific page) }`; result `{"path":"…","selectorFilterType":"…","entries":[{"raw":"…","valid":true|false,"uid":"…","urlPath":"…","issue":"…"}]}`.

- [ ] Step 1 failing test (well-formed → valid with uid+urlPath; pipe-less → invalid with issue). Step 2 RED. Step 3 implement (fail-closed on non-`/content`/non-Page `path`). Step 4 GREEN. Step 5 commit (`feat(mcp): add validate_selector_filter_format read tool`).

---

### Task 10: `check_specific_page_capability` (T-37)

Catalog §8. Resolves the page's structure-component version and reports which binding fields exist (`useForCategories`/product-page `includesSubCategories` are v2+).

**Files:** Create `internal/tools/CheckSpecificPageCapabilityTool.java`; Test + fixture (a v1 and a v2/v3 structure page).

**Interfaces:** Consumes Task 5 (page-type detection). Produces tool `check_specific_page_capability`; args `{ "path": string (required) }`; result `{"path":"…","pageType":"product|category","componentVersion":"v1|v2|v3","fields":{"selectorFilter":true,"selectorFilterType":true,"includesSubCategories":bool,"useForCategories":bool}}`. Derive the version from the page's structure component `sling:resourceType`/`resourceSuperType` (`core/cif/components/structure/page/vN/page`); per §8: `useForCategories` is v2+; product-page `includesSubCategories` applicability is v2+.

- [ ] Step 1 failing test (v1 page → useForCategories:false; v2/v3 page → true). Step 2 RED. Step 3 implement (fail-closed on bad `path`; resolve version via resourceType, super-type-aware). Step 4 GREEN. Step 5 commit (`feat(mcp): add check_specific_page_capability read tool`).

---

## Self-Review

**Spec coverage:** §7 → Tasks 2 (T-25), 3 (T-26), 4 (T-27) on the Task-1 helper; §8 → Tasks 6 (T-33), 7 (T-34), 8 (T-35), 9 (T-36), 10 (T-37) on the Task-5 helper. All 8 unshipped page-routing diagnostics covered. T-28 (`configure_catalog_page`) is a Tier-2 WRITE already shipped — not in this plan.

**Type consistency:** `CatalogPageRouting` (Task 1) produces `listCatalogPages`/`resolveFor` consumed by Tasks 2-4 under those names; `SpecificPageRouting` (Task 5) produces `specificPages`/`readBinding`/`parseSelectorFilter`/`resolveSpecificPage`/`Binding`/`ParsedFilter` consumed by Tasks 6-10. All tools return `ObjectNode`; none overrides `writesContent()`. Tool names match catalog §11 index (T-25/26/27/33/34/35/36/37).

**Ordering:** Task 1 before 2-4; Task 5 before 6-10. §7 (1-4) and §8 (5-10) are independent groups. The two helpers are the risk concentration — the reviewer must scrutinize the reimplemented algorithm (Task 5) against `SpecificPageStrategyTest` semantics.

**Placeholder scan:** algorithm detail is specified by reference to catalog §7/§8.1 + the exact `bundles/core` oracle tests, with the reimplemented constants enumerated; tool contracts (name/args/result) are concrete. The one genuine unknown — whether `SiteStructure` adapts cleanly in aem-mock vs. needing a `protected` page-list seam — is called out in Shared conventions with the fallback.

---

## Execution Handoff

Execute via superpowers:subagent-driven-development: fresh subagent per task (read `AGENTS.md` + the cited catalog §7/§8 subsection + the `bundles/core` oracle tests first), TDD, green gate, commit; task review after each; whole-branch review at the end. Tasks 1 & 5 (helpers) are the highest-risk — review them hardest.
