# CIF Authoring Tier-1 Metadata Reads — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship the 7 Tier-1 §3 "metadata & picker-support" read tools from `AUTHORING_TOOLS_CATALOG.md` (T-09, T-11, T-12, T-13, T-14, T-15, T-16), so authoring agents can inspect real backend metadata (sort fields, product links, breadcrumbs, custom-attribute metadata, URL→entity resolution, and binding validity) before driving the Tier-2/3 write tools.

**Architecture:** Each tool is an OSGi `@Component(service = McpTool.class)` in `bundles/mcp/.../internal/tools/`, discovered automatically by `ToolRegistry`. All are **read tools** (`writesContent()` stays `false` → served on both the shopper `mcp` and authoring `mcp-authoring` endpoints). They reuse the existing CIF GraphQL stack via one of two styles already in the module: **retriever subclasses** (`McpProductRetriever`/`McpCategoryRetriever`, extended with a query hook) or **raw typed queries** (`ctx.getClient().execute(queryString)`). Output is always a compact Jackson DTO, never raw GraphQL.

**Tech Stack:** Java 8 source/target (built under JDK 11), OSGi DS annotations, Jackson (`com.fasterxml.jackson.databind`), `commerce-cif-magento-graphql` (`mgql.` bindings), CIF `Abstract*Retriever`/`MagentoGraphqlClient`, aem-mock + Mockito + JUnit 4 for tests.

## Global Constraints

*(Copied from `bundles/mcp/AGENTS.md` — every task's requirements implicitly include these.)*

- **AEM 6.5 compatible. Java source/target 8, built under JDK 11.** No `var`, no `List.of`, no records, no switch-expressions, no API newer than `bundles/core` uses. `javax.servlet` never `jakarta`.
- **Build/verify with `mvn -pl bundles/mcp clean install`** (the `clean` matters — it wipes the formatter cache so `formatter:validate`/`impsort:check`/`macker`/`apache-rat` run for real). `mvn test` alone is NOT sufficient.
- **Format with the repo profile before committing:** `mvn -pl bundles/mcp -Pformat-code process-classes` (bare `formatter:format` uses Eclipse defaults and fails validation).
- Build under **JDK 11**: `export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-11.jdk/Contents/Home`; mvn at `/Users/levente/devel/prg/maven/bin/mvn`. If `-pl bundles/mcp` fails on missing reactor deps, use `-pl bundles/mcp -am`.
- **Apache 2.0 license header on every new `.java`** — copy the exact block verbatim from a sibling file (`apache-rat:check` enforces it).
- **Explicit imports, no wildcards**; `impsort` order is `java, javax, org`, then others, static last — let `-Pformat-code` sort them.
- **Jackson only** for JSON. No external MCP SDK.
- **New code goes under `com.adobe.cq.commerce.mcp.internal[.tools|.dto]`** (macker forbids the exported `…mcp` package from referencing `…mcp.internal.*`).
- **Reuse CIF, don't reimplement.** Retriever subclasses for point lookups; `ctx.getClient().execute(...)` for raw queries; `DtoMapper` for output shaping.
- **Conventional commits scoped to this module:** `feat(mcp): …` / `test(mcp): …`, one logical change per commit, ending with the repo-root `Co-Authored-By` trailer. Stage only the files the task touches.
- **Keep the JSON-RPC envelope, error codes, protocol version, and `tools/call` result shape stable** — they are the wire contract.

## Reference material (read before starting)

- **Spec:** `bundles/mcp/docs/superpowers/AUTHORING_TOOLS_CATALOG.md` — §3 has the per-tool backing classes, exact `mgql.` binding methods, output shapes, and gotchas. Each task below cites its §3.x subsection; **read that subsection** for the authoritative GraphQL field selection.
- **Module dev guide:** `bundles/mcp/AGENTS.md` §3 ("How to add a new read tool"), §6 ("Testing conventions"), §8 ("Known pitfalls").
- **Copy-paste templates in the codebase:**
  - Retriever-based read tool: `internal/tools/GetProductTool.java`, `BrowseCategoriesTool.java` (+ `McpProductRetriever.java`, `McpCategoryRetriever.java`, `McpProductVariantsRetriever.java`).
  - Raw typed query: `core/.../internal/servlets/ProductSortFieldsDataSourceServlet.java` (in `bundles/core`) is the reference for the sort-fields query; run raw queries via `ctx.getClient().execute(queryString)` → `GraphqlResponse<Query, Error>`.
  - Output DTO: `internal/dto/DtoMapper.java`, `ProductVariantsDtoMapper.java`.
  - Test shape: any `*ToolTest.java`, e.g. `GetProductVariantsToolTest.java` (retriever seam), `GetAttributesToolTest.java`.

---

## Shared conventions (apply to every task)

**Tool skeleton** (read tool — do NOT override `writesContent()`):

```java
@Component(service = McpTool.class)
public class XxxTool implements McpTool {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override public String name() { return "the_tool_name"; }
    @Override public String description() { return "…what an agent needs to decide when to call it…"; }

    @Override public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        // … per-tool properties …
        // schema.putArray("required").add("sku");
        return schema;
    }

    @Override public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;   // always safe — the servlet passes a StoreContext
        // validate args, throw IllegalArgumentException on bad input (dispatcher → -32000)
        // fetch via retriever seam or raw query
        // build and return the DTO (an ObjectNode)
    }
}
```

**Test seam** (so unit tests don't hit a live GraphQL backend): put the fetch behind a `protected` method the test overrides. Example for a product-retriever tool:

```java
protected ProductInterface fetch(StoreContext ctx, String sku) {
    McpProductRetriever r = new McpProductRetriever(ctx.getClient());
    r.setIdentifier(sku);
    return r.fetchProduct();
}
```

For raw-query tools, make the seam return the already-executed typed result, e.g. `protected Query runQuery(StoreContext ctx, ...) { GraphqlResponse<Query,Error> resp = ctx.getClient().execute(q); return resp.getData(); }` and have the test override it to return a hand-built `Query`/typed object (or a Mockito mock).

**Registration:** none beyond `@Component(service = McpTool.class)` — `ToolRegistry` binds it dynamically.

**Per-task TDD loop (every task follows this):**
1. Write the failing test (`src/test/java/.../internal/tools/XxxToolTest.java`).
2. Run RED: `export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-11.jdk/Contents/Home && /Users/levente/devel/prg/maven/bin/mvn -q -pl bundles/mcp test -Dtest=XxxToolTest` → expect failure (class/method not found or assertion fail).
3. Implement the tool minimally.
4. Run GREEN: same `-Dtest=XxxToolTest` → pass.
5. Green gate: `mvn -pl bundles/mcp -Pformat-code process-classes` then `mvn -pl bundles/mcp clean install` → all pass.
6. Commit: `git add` the tool + test (+ any `DtoMapper` change), `git commit -m "feat(mcp): add <tool_name> …"` with the `Co-Authored-By` trailer.

**Output-DTO placement:** if a shape is reused or non-trivial, add a static mapper method to `DtoMapper`; if it is one-off and small, build the `ObjectNode` inline in the tool. Note per task below.

---

### Task 1: `get_product_relationships` (T-11)

Catalog §3.3. Retriever-based (extend the product query with `product_links`).

**Files:**
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/GetProductRelationshipsTool.java`
- Test: `bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/GetProductRelationshipsToolTest.java`

**Interfaces:**
- Consumes: `StoreContext.getClient()`, `McpProductRetriever` (extend via query hook), `mgql.ProductInterface.getProductLinks()` → `List<ProductLinksInterface>` with `getLinkType()` (plain `String`), `getLinkedProductSku()`, `getLinkedProductType()`, `getPosition()`, `getSku()`.
- Produces: tool name `get_product_relationships`; args `{ "sku": string (required), "linkType": string (optional filter: "related"|"upsell"|"crosssell") }`; result `{"sku":"…","links":[{"linkType":"…","sku":"…","linkedProductSku":"…","linkedProductType":"…","position":N}]}`.

- [ ] **Step 1: Write the failing test.** Build a Mockito `ProductInterface` whose `getProductLinks()` returns two fake `ProductLinksInterface` mocks (one `"related"`, one `"upsell"`). Instantiate the tool with the `fetch` seam overridden to return that mock. Call `call(ctx, args)` with `{"sku":"MJ01"}` and assert the result has 2 links with the right `linkType`/`linkedProductSku`. Add a second test: `{"sku":"MJ01","linkType":"related"}` returns only the related link. Add a negative test: missing `sku` → `IllegalArgumentException`.
- [ ] **Step 2: Run RED** (`-Dtest=GetProductRelationshipsToolTest`) → fails (class missing).
- [ ] **Step 3: Implement.** Tool skeleton above. `fetch` seam: `new McpProductRetriever(ctx.getClient())` with a query hook adding `product_links{ link_type, linked_product_sku, linked_product_type, position, sku }` (see §3.3 for exact `mgql` selection; mirror the hook pattern in `McpProductVariantsRetriever`). Map `getProductLinks()` to the DTO array inline (small, one-off — no `DtoMapper` change needed), filtering by `linkType` when present. `link_type` is a **plain String**, not an enum — compare case-insensitively.
- [ ] **Step 4: Run GREEN** → pass.
- [ ] **Step 5: Green gate + commit** (`feat(mcp): add get_product_relationships read tool`).

---

### Task 2: `get_category_breadcrumbs` (T-12)

Catalog §3.4. Retriever-based (extend the category query with `breadcrumbs`). **Do this before Task 3 — `resolve_category_details` reuses this breadcrumb selection.**

**Files:**
- Create: `.../internal/tools/GetCategoryBreadcrumbsTool.java`
- Test: `.../internal/tools/GetCategoryBreadcrumbsToolTest.java`

**Interfaces:**
- Consumes: `McpCategoryRetriever` (extend query hook), `mgql.CategoryInterface.getBreadcrumbs()` → `List<Breadcrumb>` with `getCategoryUid()`, `getCategoryName()`, `getCategoryLevel()`, `getCategoryUrlPath()`. `McpCategoryRetriever.setCategoryIdType("urlPath")` selects urlPath vs UID.
- Produces: tool name `get_category_breadcrumbs`; args `{ "uid": string (required unless urlPath given), "urlPath": string (optional; if set, resolve by urlPath) }`; result `{"uid":"…","breadcrumbs":[{"uid":"…","name":"…","level":N,"urlPath":"…"}]}` **ordered by ascending `level`**.
- **Reusable helper to Produce:** a `static ArrayNode breadcrumbs(ObjectMapper m, CategoryInterface cat)` — put it on `DtoMapper` so Task 3 can reuse it. Signature: `DtoMapper.breadcrumbs(ObjectMapper, List<Breadcrumb>)` returning an ordered `ArrayNode`.

- [ ] **Step 1: Write the failing test.** Mock a `CategoryInterface` whose `getBreadcrumbs()` returns two `Breadcrumb` mocks with levels 2 and 1 (out of order). Override the `fetch` seam to return it. Assert the result's `breadcrumbs` array is ordered level 1 then 2 with the right fields. Negative test: neither `uid` nor `urlPath` → `IllegalArgumentException`.
- [ ] **Step 2: Run RED** → fails.
- [ ] **Step 3: Implement.** Add `DtoMapper.breadcrumbs(ObjectMapper, List<Breadcrumb>)` (sorts by `getCategoryLevel()`, maps the four fields). `fetch` seam: `McpCategoryRetriever` with a hook adding `breadcrumbs{ category_uid, category_name, category_level, category_url_path }` (§3.4); set `setCategoryIdType("urlPath")` when `urlPath` arg present, else default UID. Tool builds `{uid, breadcrumbs}` via the new mapper.
- [ ] **Step 4: Run GREEN** → pass.
- [ ] **Step 5: Green gate + commit** (`feat(mcp): add get_category_breadcrumbs read tool`). Stage `DtoMapper.java` too.

---

### Task 3: `resolve_category_details` (T-16)

Catalog §3.7/§3.8. Retriever-based; **reuses `DtoMapper.breadcrumbs` from Task 2.**

**Files:**
- Create: `.../internal/tools/ResolveCategoryDetailsTool.java`
- Test: `.../internal/tools/ResolveCategoryDetailsToolTest.java`

**Interfaces:**
- Consumes: `McpCategoryRetriever` (extend query hook with name/urlPath **+** breadcrumbs), `DtoMapper.breadcrumbs(...)` (Task 2), `mgql.CategoryInterface` `getUid()/getName()/getUrlPath()/getBreadcrumbs()`.
- Produces: tool name `resolve_category_details`; args `{ "uid": string (required), "urlPath": string (optional idType override) }`; result `{"uid":"…","name":"…","urlPath":"…","breadcrumbs":[…]}`. Return a clear "not found" result (`{"uid":"…","resolves":false}`) when the retriever yields null / `getErrors()` is non-empty.

- [ ] **Step 1: Write the failing test.** Mock a `CategoryInterface` with uid/name/urlPath + two breadcrumbs; override `fetch` seam; assert the flattened DTO. Second test: `fetch` returns `null` → result `{"uid":…,"resolves":false}`. Negative: missing `uid` → `IllegalArgumentException`.
- [ ] **Step 2: Run RED** → fails.
- [ ] **Step 3: Implement.** `fetch` seam: `McpCategoryRetriever` with a hook adding `uid name url_path` and the breadcrumb selection (same fields as Task 2). Build the DTO; reuse `DtoMapper.breadcrumbs(...)`.
- [ ] **Step 4: Run GREEN** → pass.
- [ ] **Step 5: Green gate + commit** (`feat(mcp): add resolve_category_details read tool`).

---

### Task 4: `get_sort_options` (T-09)

Catalog §3.1. Raw typed query (no retriever fits). Mirrors `ProductSortFieldsDataSourceServlet` in `bundles/core`.

**Files:**
- Create: `.../internal/tools/GetSortOptionsTool.java`
- Test: `.../internal/tools/GetSortOptionsToolTest.java`

**Interfaces:**
- Consumes: `ctx.getClient().execute(queryString)` → `GraphqlResponse<Query, Error>`; `mgql.Query.getProducts().getSortFields()` → `mgql.SortFields` with `getDefault()` and `getOptions()` → `List<SortField>` (`getValue()`, `getLabel()`).
- Produces: tool name `get_sort_options`; args `{}` (store comes from context); result `{"default":"…","options":[{"value":"…","label":"…"}]}`.

- [ ] **Step 1: Write the failing test.** Override a `protected SortFields fetchSortFields(StoreContext ctx)` seam to return a hand-built/mocked `SortFields` (default `"position"`, options `price`/`name`). Assert the DTO. (No live query in the unit test.)
- [ ] **Step 2: Run RED** → fails.
- [ ] **Step 3: Implement.** Build the query `{products(filter:{}){sort_fields{default options{label value}}}}` exactly as `ProductSortFieldsDataSourceServlet` does (see §3.1 / that servlet for the `Operations.query(...)` form), run via `ctx.getClient().execute(...)`, read `getProducts().getSortFields()`. Map to `{default, options[]}`. Note per §3.1: `relevance` is servlet-injected for searchresults and is **not** in the plain-listing API response — do not synthesize it.
- [ ] **Step 4: Run GREEN** → pass.
- [ ] **Step 5: Green gate + commit** (`feat(mcp): add get_sort_options read tool`).

---

### Task 5: `get_custom_attribute_metadata` (T-13)

Catalog §3.5. Raw typed query requiring a `List<AttributeInput>`.

**Files:**
- Create: `.../internal/tools/GetCustomAttributeMetadataTool.java`
- Test: `.../internal/tools/GetCustomAttributeMetadataToolTest.java`

**Interfaces:**
- Consumes: `mgql.QueryQuery.customAttributeMetadata(List<AttributeInput>, def)`; each `AttributeInput` needs `{attribute_code, entity_type}`; result `mgql.CustomAttributeMetadata.getItems()` → `List<Attribute>` (`getAttributeCode()`, `getAttributeType()`, `getInputType()`, `getEntityType()`, `getAttributeOptions()` → `{getLabel(), getValue()}`).
- Produces: tool name `get_custom_attribute_metadata`; args `{ "attributes": [ {"code": string (required), "entityType": string (optional, default "catalog_product")} ] (required, non-empty) }`; result `{"items":[{"code":"…","attributeType":"…","inputType":"…","entityType":"…","options":[{"label":"…","value":"…"}]}]}`.

- [ ] **Step 1: Write the failing test.** Override a `protected CustomAttributeMetadata fetch(StoreContext ctx, List<AttributeInput> inputs)` seam returning a mocked metadata with one `Attribute` (with two options). Assert the DTO. Negative test: empty/missing `attributes` → `IllegalArgumentException`.
- [ ] **Step 2: Run RED** → fails.
- [ ] **Step 3: Implement.** Parse the `attributes` array into `List<AttributeInput>` (default `entity_type="catalog_product"` when absent). Build the raw query `Operations.query(q -> q.customAttributeMetadata(inputs, def))` (see §3.5), run it, map `getItems()` to the DTO.
- [ ] **Step 4: Run GREEN** → pass.
- [ ] **Step 5: Green gate + commit** (`feat(mcp): add get_custom_attribute_metadata read tool`).

---

### Task 6: `resolve_url_to_entity` (T-14)

Catalog §3.6. Raw typed query via `urlResolver(url)`.

**Files:**
- Create: `.../internal/tools/ResolveUrlToEntityTool.java`
- Test: `.../internal/tools/ResolveUrlToEntityToolTest.java`

**Interfaces:**
- Consumes: `mgql.QueryQuery.urlResolver(String, def)` → `mgql.EntityUrl` (`getType()` → `UrlRewriteEntityTypeEnum`, `getId()`, `getEntityUid()`, `getCanonicalUrl()`, `getRelativeUrl()`, `getRedirectCode()`).
- Produces: tool name `resolve_url_to_entity`; args `{ "url": string (required) }`; result `{"url":"…","type":"PRODUCT|CATEGORY|CMS_PAGE|…","id":N,"uid":"…","canonicalUrl":"…","relativeUrl":"…","redirectCode":N}`. Return `{"url":"…","resolves":false}` when `urlResolver` yields null.

- [ ] **Step 1: Write the failing test.** Override `protected EntityUrl fetch(StoreContext ctx, String url)` seam returning a mocked `EntityUrl` (type PRODUCT, id 42, uid "abc"). Assert the DTO (`type` as `getType().toString()`). Second test: seam returns null → `{"url":…,"resolves":false}`. Negative: missing `url` → `IllegalArgumentException`.
- [ ] **Step 2: Run RED** → fails.
- [ ] **Step 3: Implement.** Build `Operations.query(q -> q.urlResolver(url, u -> u.type().id().entityUid().canonicalUrl().relativeUrl().redirectCode()))` (see §3.6 for exact selection), run via `ctx.getClient().execute(...)`, read `getData().getUrlResolver()`, map to DTO. `getType()` is an enum — emit its `.toString()`.
- [ ] **Step 4: Run GREEN** → pass.
- [ ] **Step 5: Green gate + commit** (`feat(mcp): add resolve_url_to_entity read tool`).

---

### Task 7: `validate_content_bindings` (T-15)

Catalog §3.7. Retriever-based batch validity check (loops single-identifier retrievers).

**Files:**
- Create: `.../internal/tools/ValidateContentBindingsTool.java`
- Test: `.../internal/tools/ValidateContentBindingsToolTest.java`

**Interfaces:**
- Consumes: `McpProductRetriever`/`McpCategoryRetriever` (a null fetch or non-empty `getErrors()` = "does not resolve"). Reuse the `fetch` seams so the test can stub resolve/not-resolve per identifier.
- Produces: tool name `validate_content_bindings`; args `{ "products": [sku,…] (optional), "categories": [uid,…] (optional) }` (at least one non-empty); result `{"products":[{"sku":"…","resolves":true|false}],"categories":[{"uid":"…","resolves":true|false}]}`.

- [ ] **Step 1: Write the failing test.** Override two seams — `protected boolean productResolves(StoreContext, String sku)` and `protected boolean categoryResolves(StoreContext, String uid)` — to return true for one id and false for another. Assert the per-identifier result flags. Negative test: both arrays empty/absent → `IllegalArgumentException`.
- [ ] **Step 2: Run RED** → fails.
- [ ] **Step 3: Implement.** For each sku: `McpProductRetriever`, `setIdentifier`, `fetchProduct()`; treat null or `getErrors()` non-empty as `resolves:false`. Same for categories with `McpCategoryRetriever`/`fetchCategory()`. Reuse one client; instantiate a fresh retriever per identifier (they are single-use/stateful — see AGENTS.md §3). Build the DTO.
- [ ] **Step 4: Run GREEN** → pass.
- [ ] **Step 5: Green gate + commit** (`feat(mcp): add validate_content_bindings read tool`).

---

## Self-Review

**Spec coverage (catalog §3):** T-09 → Task 4; T-10 already shipped (`get_product_variants`, not in this plan); T-11 → Task 1; T-12 → Task 2; T-13 → Task 5; T-14 → Task 6; T-15 → Task 7; T-16 → Task 3. All seven unshipped §3 tools covered.

**Type consistency:** `DtoMapper.breadcrumbs(ObjectMapper, List<Breadcrumb>)` is defined in Task 2 and consumed in Task 3 under the same name/signature. All tools return an `ObjectNode` from `call(...)`; none override `writesContent()` (all read). Tool names match the catalog §11 index exactly.

**Placeholder scan:** GraphQL field selections are specified by their exact `mgql.` binding methods and cross-referenced to the catalog §3.x subsection the implementer must read; test bodies are described concretely (mock shapes + assertions). Where a lambda's exact field-selection method names must be confirmed, the task points to the sibling retriever/servlet that already does it — not a TODO.

**Ordering:** Task 2 (breadcrumbs helper) precedes Task 3 (reuses it). All other tasks are independent and may be parallelized across subagents.

---

## Execution Handoff

Execute via **superpowers:subagent-driven-development**: one fresh subagent per task, each instructed to read `bundles/mcp/AGENTS.md` + the cited catalog §3.x subsection first, do the TDD loop, run the green gate, and commit. Review between tasks. Tasks 1, 4, 5, 6, 7 are independent; Task 2 must land before Task 3.
