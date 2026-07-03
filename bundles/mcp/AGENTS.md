# AGENTS.md — `core-cif-components-mcp`

Guidance for coding agents adding features to the CIF Commerce MCP bundle. This is
**module-scoped** and complements the repo-root `AGENTS.md` / `CLAUDE.md` (which win on
anything general). Read `README.md` in this directory first for the user-facing overview;
this file is about *how to develop here safely*.

If you only remember three things:
1. **Build/verify with `mvn -pl bundles/mcp clean install`, not `test`** — the quality gates
   (`formatter:validate`, `impsort:check`, `macker`, `apache-rat`) run at the `verify` phase,
   and `clean` is required so the formatter cache is wiped and validation is real.
2. **Format with the repo profile:** `mvn -pl bundles/mcp -Pformat-code process-classes`.
   The bare `formatter:format` goal uses Eclipse defaults and will fail `formatter:validate`.
3. **Write tools are a security boundary.** They must be `writesContent()==true`, use the
   caller's `ResourceResolver`, and fail closed on unknown resource types. The anonymous
   shopper endpoint's safety depends on this.

---

## 1. Architecture & request flow

One bundle, deployed to author and publish. Two `SlingAllMethodsServlet`s bind `cq:Page` +
`POST` + extension `json`, differing only by selector, and share one protocol core:

```
POST <navRoot>.mcp.json            POST <navRoot>.mcp-authoring.json
  (ShopperMcpServlet, selector      (AuthoringMcpServlet, selector
   "mcp", author+publish,            "mcp-authoring", AUTHOR ONLY via
   anonymous)                        config.author, REQUIRE policy)
        └──────────────┬───────────────────┘
                       │  both extend AbstractMcpServlet
                       ▼
   AbstractMcpServlet.doPost:
     • body > 65536 bytes → 413
     • !StoreContextResolver.isNavRoot(req) → 404      (gate on CIF nav-root)
     • parse JSON-RPC (bad → -32700 envelope, HTTP 200)
     • ctx = StoreContextResolver.resolve(req)         (store context = endpoint's own page)
     • dispatcher.dispatch(selector(), ctx, req)
                       ▼
   JsonRpcDispatcher:  initialize | tools/list | tools/call
                       ▼
   ToolRegistry.forSelector(selector) / byName(selector, name)
     • "mcp"           → tools where writesContent()==false
     • "mcp-authoring" → all tools
                       ▼
   McpTool.call(StoreContext, args)  →  DtoMapper  →  {content:[{type:text,text}], structuredContent}
```

- **Store context** is the endpoint's own nav-root `(resource, request)` — no `store`
  argument. `StoreContext` exposes `getResource()`, `getRequest()`, `getLandingPage()`
  (nav-root), `getProductPage()`, `getClient()` (`MagentoGraphqlClient`).
- **Protocol invariants** (don't change casually): protocolVersion `"2025-06-18"`; error
  codes `-32700/-32600/-32601/-32602/-32603/-32000` (see `JsonRpc`); `tools/call` result is
  `{content:[{type:"text",text:<json>}], structuredContent:<toolResult>}`.

### Package layout (macker-enforced)

- `com.adobe.cq.commerce.mcp` — **exported API only**: `JsonRpc`, `McpTool`,
  `McpCallContext`. Must **not** reference `…mcp.internal.*` (macker fails the build).
- `com.adobe.cq.commerce.mcp.internal[.servlets|.tools|.dto]` — everything else. May freely
  use the exported API. **New code almost always goes under `internal`.**

See `README.md` for the full file tree.

---

## 2. Coding guidelines (this module)

- **AEM 6.5 compatible. Java source/target 8, built under JDK 11.** No API newer than what
  `bundles/core` uses. `javax.servlet` (never `jakarta`). No `var`, no `List.of`, no
  `records`, no `switch` expressions — nothing past Java 8.
- **Apache 2.0 license header on every new `.java`** — copy the exact block from a sibling
  file (`apache-rat:check` enforces it).
- **Explicit imports, no wildcards** — `impsort:check` enforces order (`java, javax, org`,
  then others; static after). Just run `-Pformat-code` and let it sort.
- **Jackson only** for JSON (`com.fasterxml.jackson.databind`). No external MCP SDK.
- **Reuse CIF, don't reimplement.** Search/facets → `SearchResultsService` /
  `SearchFilterService`; point lookups → `Abstract*Retriever` subclasses; product/category
  URLs → `UrlProvider`; store-root identification → `SiteStructure`. The whole point of this
  bundle is that it is thin glue over `core-cif-components-core`.
- **Compact DTOs, never raw GraphQL.** Map to the smallest useful shape via `DtoMapper`.

### Dependencies / scopes (a real trip-hazard)

CIF types resolve through `core-cif-components-core` (scope `provided`). But a type that is
only *transitively* provided by core is **not** on this module's compile classpath — you must
declare it explicitly, and always at **`provided`** scope (never `compile`/`runtime`, which
would embed it and change the OSGi import surface). Precedents already in `pom.xml`:

- `com.adobe.cq:core.wcm.components.core` (provided) — needed because `ProductListItem`
  inherits `getTitle()`/`getURL()` from WCM Core Components' `ListItem`.
- `com.adobe.aem:aem-cif-sdk-api` (provided) — `AssociatedContentService` / `AssociatedContentQuery`
  used by associated-content MCP tools (not on the compile classpath via core alone).
- `com.adobe.commerce.cif:graphql-client` (provided) — `GraphqlResponse` etc. used by the
  retrievers' compile surface.

Test deps for aem-mock (provided already): `io.wcm.testing.aem-mock`,
`org.apache.sling.models.impl`, `com.adobe.cq:core.wcm.components.core`, `graphql-client`.
Mirror `bundles/core/pom.xml` when adding any of these.

---

## 3. How to add a new read tool

TDD, following the existing tools (`SearchProductsTool`, `GetProductTool`,
`BrowseCategoriesTool` are good templates).

1. **Write the failing test first** in `src/test/java/…/internal/tools/`. For tools that hit
   commerce, give the tool a **test seam** so the unit test doesn't do a real GraphQL call:

   ```java
   // in the tool
   protected ProductInterface fetch(StoreContext ctx, String sku) {
       McpProductRetriever r = new McpProductRetriever(ctx.getClient());
       r.setIdentifier(sku);
       return r.fetchProduct();
   }
   // in the test
   MyTool tool = new MyTool() {
       @Override protected ProductInterface fetch(StoreContext c, String sku) { return mockProduct; }
   };
   ```

2. **Run it RED:** `mvn -q -pl bundles/mcp test -Dtest=MyToolTest`.

3. **Implement** in `…/internal/tools/`:

   ```java
   @Component(service = McpTool.class)
   public class MyTool implements McpTool {
       private final ObjectMapper mapper = new ObjectMapper();
       @Reference SearchResultsService searchResultsService;   // package-visible so tests can set it

       public String name() { return "my_tool"; }
       public String description() { return "…what an agent needs to decide when to call it…"; }
       public ObjectNode inputSchema() { /* JSON Schema as an ObjectNode */ }
       // read tool: do NOT override writesContent() (defaults to false → visible on both endpoints)

       public JsonNode call(McpCallContext context, JsonNode args) {
           StoreContext ctx = (StoreContext) context;      // always safe: the servlet passes a StoreContext
           // … build result via DtoMapper …
       }
   }
   ```

   - Registering as `@Component(service = McpTool.class)` is all it takes — `ToolRegistry`
     binds it dynamically. No manual wiring, no list to edit.
   - `@Reference` fields should be **package-visible** (no modifier) so unit tests can assign
     them directly, matching the existing tools.

4. **Run it GREEN**, then the green gate (§5), then commit.

### Data-access cheat sheet

| Need | Use | Notes |
|---|---|---|
| keyword/faceted product search | `SearchResultsService.performSearch(options, resource, productPage, request)` | build `McpSearchOptions` from args; pass `ctx.getProductPage()` |
| filterable attribute metadata | `SearchFilterService.retrieveCurrentlyAvailableCommerceFilters(page)` | pass `ctx.getLandingPage()` |
| single product by sku/url_key | subclass `AbstractProductRetriever` (`McpProductRetriever`) | |
| category tree | subclass `AbstractCategoryRetriever` (`McpCategoryRetriever`) | |
| product PDP link | `ProductListItem.getURL()` | already mapped by `DtoMapper.product` as `url` |
| category PLP link | `urlProvider.formatCategoryUrl(request, navRootPage, new CategoryUrlFormat.Params(cat))` | `@Reference UrlProvider`; pass the nav-root page so it resolves the store's category page |

### Retriever subclasses

`Abstract*Retriever` are **single-use and stateful** (`populate()` caches; one query per
instance). **Instantiate per tool call**, never share. Implement exactly the one abstract
query method and preserve the hook passthrough:

```java
public class McpProductRetriever extends AbstractProductRetriever {
    public McpProductRetriever(MagentoGraphqlClient client) { super(client); }
    @Override protected ProductInterfaceQueryDefinition generateProductQuery() {
        return q -> {
            q.sku().name().urlKey()/* only the fields your DTO maps */;
            if (productQueryHook != null) productQueryHook.accept(q);   // keep this
        };
    }
}
```

Trim the query to the fields you actually map (avoid over-fetch). Copy the field-selection
pattern from a concrete `bundles/core` retriever (e.g. `productteaser/ProductRetriever`,
`button/CategoryRetriever`) — but subclass the **exported `Abstract*` base**, never the
`internal.models.v1.*` concretes.

---

## 4. How to add a write tool (author-only) — READ THIS

Write tools are the security boundary that makes the anonymous shopper endpoint safe. Follow
`ConfigureProductComponentTool` / `ConfigureCatalogPageTool` exactly.

- **`writesContent()` MUST return `true`.** This is what keeps the tool off the shopper
  selector (`ToolRegistry.forSelector("mcp")` filters it, and `tools/call` for it there
  returns `-32601`). It is also physically absent on publish (authoring servlet is
  `config.author`-gated).
- **Use the caller's resolver only:** `ctx.getRequest().getResourceResolver()`. Never a
  service/admin `ResourceResolver` — JCR ACLs must enforce every write. No privilege
  escalation through the MCP layer.
- **Fail closed.** Validate, then throw `IllegalArgumentException` (the dispatcher maps it to
  a `-32000` tool error) on: missing args; path not under `/content/`; resource not found;
  not adaptable to `ModifiableValueMap`; **and the resource is not a CIF component/page you
  understand.** For the type check use super-type-aware `Resource.isResourceType(type)`
  (handles proxied/superTyped project components like Venia's) against the real CIF constants:
  - product component: `ProductImpl.RESOURCE_TYPE` (v1/v2/v3, `core/cif/components/commerce/product/vN/product`)
  - catalog/PLP page: `SiteStructure.RT_CATALOG_PAGE` / `RT_CATALOG_PAGE_V3`
- Commit the write, return `{path, …, updated:true}`.
- Add a **negative test** proving the fail-closed path (build a resource with a non-CIF
  `sling:resourceType` and assert `IllegalArgumentException`) — a happy-path-only test is not
  enough for a write tool.
- Author-only activation is not automatic from the code: the servlet is
  `configurationPolicy=REQUIRE`; the OSGi config lives at
  `ui.config/src/content/jcr_root/apps/core/cif/config.author/…AuthoringMcpServlet.cfg.json`.

---

## 5. Build, verify, format, deploy

```bash
# format with the PROJECT config (bare formatter:format is WRONG here)
mvn -pl bundles/mcp -Pformat-code process-classes

# the real green gate — clean is required (wipes the formatter cache so validate runs for real)
mvn -pl bundles/mcp clean install        # tests + formatter:validate + impsort:check + macker + apache-rat

# whole-reactor check (mirrors CI / RUN_PROMPT)
mvn -pl bundles/mcp -am clean install

# deploy to a running author (no sling-maven deploy profile in this repo)
curl -s -u admin:admin -F action=install -F bundlestart=start -F bundlestartlevel=20 -F refreshPackages=true \
  -F bundlefile=@bundles/mcp/target/core-cif-components-mcp-*.jar http://localhost:4502/system/console/bundles
```

Runtime checks after deploy:
- Bundle `Active` at `/system/console/bundles.json` (`com.adobe.commerce.cif.core-cif-components-mcp`).
- Components `active` at `/system/console/components.json` (all tools + `ShopperMcpServlet`;
  `AuthoringMcpServlet` shows `no config` until its `config.author` config exists — create it
  via ConfigMgr for local authoring tests).
- Exercise the endpoint: `POST <navRoot>.mcp.json` with `initialize` / `tools/list` /
  `tools/call`. Find a nav-root under `/content` (e.g. Venia `/content/venia/us/en`).

---

## 6. Testing conventions

- **aem-mock** (`io.wcm.testing.mock.aem.junit.AemContext`) + **Mockito** + JUnit 4. JCR
  fixtures go in `src/test/resources/context/*.json` and load via
  `context.load().json("/context/x.json", "/content")` — the JSON's top-level keys become
  children of the load path (no extra `content` wrapper).
- **Mockito idiom here:** `import static org.mockito.Mockito.any;` (NOT
  `org.mockito.ArgumentMatchers` — it doesn't resolve on this classpath). `any(Class)` does
  **not** match `null`; use the no-arg `any()` for arguments that will be null in the test.
- **Assert real behavior, not tautologies.** Trace a mocked value through the code into the
  JSON output; don't assert a mock echoing itself.
- **Test seams** (`protected fetch(...)`) let retriever-based tools be unit-tested without a
  live GraphQL backend.
- **Asserting OSGi `@Component` metadata:** `@Component` has `RetentionPolicy.CLASS` and is
  invisible to runtime reflection. To assert e.g. `configurationPolicy`, read the
  compiler-generated SCR descriptor `OSGI-INF/<fqcn>.xml` and check its
  `configuration-policy` attribute (see `AuthoringPolicyTest`).

---

## 7. Commits & scope

- Conventional commits scoped to this module: `feat(mcp):`, `fix(mcp):`, `test(mcp):`,
  `docs(mcp):`. End the message with the `Co-Authored-By` trailer per the repo-root rules.
- One logical change per commit; run the green gate (§5) **before** committing.
- Stage only the files your change touches. In particular do **not** stage `RUN_PROMPT.md`
  or unrelated working-tree noise.
- Keep the JSON-RPC envelope, error codes, protocol version, and result shape stable — they
  are the wire contract with MCP clients.

---

## 8. Known pitfalls (learned the hard way)

- **`mvn test` is not enough.** It passes even with formatting/macker/rat violations because
  those gates run at `verify`. Always `clean install` before you trust a change.
- **Nav-root 404 gate only fires for `cq:Page` resources.** A POST to a path that is not a
  page falls through to Sling's default POST servlet (which may *create* a node). When testing
  the 404 gate, use a real non-nav-root `cq:Page` (e.g. `<navRoot>/products`), not an
  arbitrary path.
- **PDP/PLP URLs are page-relative** (as `UrlProvider` emits them). Prepend scheme/host if an
  absolute link is needed; don't hand-build URLs from slugs/url_path.
- **Fetch `url_key` and `url_path`** for categories if you build PLP links — the configured
  URL format may use either, and the fallback needs both to avoid a GraphQL round-trip.
- **`AuthoringMcpServlet` won't activate without its config** — expected on publish (absent),
  and locally you must supply the `config.author` config (or a ConfigMgr entry).

---

## 9. Reference material

- Design & plan: `docs/superpowers/specs/2026-07-02-cif-commerce-mcp-design.md`,
  `docs/superpowers/plans/2026-07-02-cif-commerce-mcp.md`. Validation log: repo-root
  `VALIDATION.md`.
- Key CIF core types to study before extending: `SearchResultsService`, `SearchOptions`,
  `SearchResultsSet`, `SearchFilterService`, `FilterAttributeMetadata`,
  `AbstractProductRetriever`, `AbstractCategoryRetriever`, `UrlProvider`,
  `CategoryUrlFormat.Params`, `SiteStructure`, `ProductListItem` (+ `ProductListItemImpl`),
  `MagentoGraphqlClient`.
