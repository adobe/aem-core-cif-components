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
- `com.adobe.cq.commerce.mcp.internal[.servlets|.tools|.tools.authoring|.dto]` — everything else.
  May freely use the exported API. **New code almost always goes under `internal`.**
- **Endpoint exposure is decided by `McpTool.authoringOnly()` — NOT by the Java package.** A tool is
  served by the anonymous shopper endpoint (`…mcp`) unless `authoringOnly()` returns `true`; the
  authoring endpoint (`…mcp-authoring`) serves everything. `authoringOnly()` defaults to
  `writesContent()`, so **write tools are authoring-only automatically**; an authoring-oriented
  **read** tool must override `authoringOnly()` to `return true` (see `ToolRegistry.forSelector`).
- **`internal.tools.authoring` is just code grouping** for the authoring surface — the authoring
  (write) tools, the authoring-only read tools, and their helpers (`CommerceWriteSupport`,
  `PageCreationSupport`, `PageTemplateSupport`, `AssociatedContentSupport`, `CatalogPageRouting`,
  `SpecificPageRouting`, `CommerceContentTagger`, `PathArgs`) live here; shopper/catalog read tools,
  the `Mcp*Retriever`s, and the cart helpers stay in `internal.tools`. Putting a class in this
  package does **not** change its exposure — only `authoringOnly()` does. Tests mirror the package
  under `src/test/java/…`.

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

- `com.google.code.gson:gson:2.8.9` (**test** scope, not provided) — needed the first time a
  test exercises real Gson (de)serialization, e.g. `MutationDeserializer.getGson()` /
  `QueryDeserializer.getGson()`. `provided`-scope dependencies don't bring their own transitive
  deps onto this module's classpath, so `gson` (a transitive dep of `magento-graphql`/
  `graphql-client`) isn't there until declared directly. Mirrors `bundles/core/pom.xml`'s
  existing test-scope `gson` dependency. Missing this fails at test *runtime* with
  `NoClassDefFoundError: com/google/gson/internal/Excluder`, not at compile time.

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

## 3b. How to add a cart/mutation tool (shopper endpoint, `writesContent() == false`)

Cart tools (`add_to_cart`, `view_cart`, `update_cart_item`, `clear_cart`) mutate the **remote
Magento cart**, not AEM JCR content — do not confuse this with §4 below. `writesContent()`
stays `false` and the tool stays on the anonymous shopper endpoint, same as an anonymous
storefront visitor adding to cart.

**The one thing that will trip you up:** `MagentoGraphqlClient.execute(String)` (in
`bundles/core`) **cannot be used for mutations.** Its implementation hardcodes response
deserialization to `Query.class` (`MagentoGraphqlClientImpl.execute()` calls
`graphqlClient.execute(request, Query.class, Error.class, options)`), so a mutation response
(shaped by `Mutation.class`) silently fails to deserialize. Confirmed live, not just by reading
the code — the symptom was a `GraphQL client not available for resource …` error even though the
exact same resource worked fine for `view_cart`'s query.

Use the existing **`CartMutationClient`** (`internal/tools/CartMutationClient.java`) for every
mutation instead:

```java
private final CartMutationClient mutationClient = new CartMutationClient();

protected Cart doSomething(StoreContext ctx, /* ... */) {
    return mutationClient
        .execute(ctx, m -> m.someMutation(args -> args.input(input), out -> out
            .cart(CartMutationClient.cartFields())   // shared field selection, don't re-inline it
            .userErrors(e -> e.code().message())))    // check this — see below
        .getSomeMutation()
        .getCart();
}
```

`CartMutationClient` handles the two things a hand-rolled mutation call needs that
`MagentoGraphqlClient` doesn't provide:
1. Resolves the raw `GraphqlClient` correctly. The endpoint's own resource is **not** directly
   adaptable to `GraphqlClient` (`resource.adaptTo(GraphqlClient.class)` returns `null` for a
   nav-root page) — `MagentoGraphqlClientImpl` resolves the CIF context-aware commerce config
   first (`resource.adaptTo(ComponentsConfiguration.class)`) and adapts a synthetic
   `ValueMapResource` wrapping it instead. `CartMutationClient.resolveGraphqlClient()`
   reproduces that using only public API — no `bundles/core` changes needed. It's `public
   static`, along with `toHeaders()`, so any tool needing the raw `GraphqlClient` (not just cart
   mutations) can reuse it — see `GetOrderTool` below.
2. Uses `MutationDeserializer.getGson()` (the mutation-side counterpart of the `QueryDeserializer`
   `MagentoGraphqlClient` uses internally) so the `Mutation`-shaped response actually deserializes.

**Prefer the unified `addProductsToCart` mutation** over type-specific ones
(`addSimpleProductsToCart`, `addConfigurableProductsToCart`, …) — it accepts
`CartItemInput.selectedOptions` and works for simple *and* configurable products through one
code path (see `AddToCartTool`).

**Check `user_errors` on the mutation output, not just top-level GraphQL errors.** Newer
mutations like `addProductsToCart` report validation failures (e.g. "you need to choose
options") via a `user_errors: [CartUserInputError]` field on the output type, separate from
`CartMutationClient`'s top-level `response.getErrors()` check. Each tool's own mutation method
must check `output.getUserErrors()` itself and throw if non-empty (see
`AddToCartTool.addItem()`).

**Cart field selection is shared, not re-inlined.** `CartMutationClient.cartFields()` returns
the one `CartQueryDefinition` every cart tool's cart selection uses (`.id().totalQuantity()
.items(...).prices(...)`), because it has to match `DtoMapper.cart()`'s mapping exactly. It works
both for a plain query (`Query.cart(id, CartQueryDefinition)`, used by `view_cart`) and for a
mutation's `cart(...)` output selection (used by the other three) — both take the same
`CartQueryDefinition` type. If you add a field to `DtoMapper.cart()`, add it here once, not in
four places.

**Configurable-product option resolution** (`ConfigurableOptionResolver`): an agent supplies
human-readable option values (e.g. `{"fashion_color": "Peach"}`), never Magento's internal
option-value UIDs. The resolver fetches the product's `configurable_options` (extend
`McpProductRetriever`'s query hook — `q.onConfigurableProduct(cp ->
cp.configurableOptions(...))`) and matches case-insensitively against either the attribute code
or its label. On a missing/invalid option it throws `IllegalArgumentException` naming the real
attribute and its available values — this is a deliberate UX improvement over Magento's generic
error, don't swallow it into a generic message.

**Bundle products don't need a separate mutation — don't assume otherwise from the schema
alone.** Magento has a dedicated `addBundleProductsToCart` mutation with its own
`BundleOptionInput` shape (`int id, double quantity, List<String> value`), which looks like the
obviously-intended path and was in fact the first implementation here. **Verified live that it's
unnecessary**: a bundle choice's own `uid` field (query it via `q.onBundleProduct(bp ->
bp.items(i -> i.title().required().options(o -> o.label().uid())))`) is a base64 string like
`bundle/2/2/1` that works exactly like a configurable option-value UID in
`CartItemInput.selectedOptions` — the same unified `addProductsToCart` mutation `add_to_cart`
already uses for simple/configurable products handles bundles too. `BundleOptionResolver`
mirrors `ConfigurableOptionResolver`'s shape (match a human label, e.g. `{"Necklace": "Carmina
Necklace"}`, against the bundle item's `title` and each choice's `label`) but resolves straight
to that `uid` — no `BundleOptionInput` construction, no separate mutation call, no
`instanceof BundleProduct` branch in the tool itself (`AddToCartTool.call()` just concatenates
both resolvers' `List<ID>` results before one `addItem()` call). If you're about to reach for a
product-type-specific mutation because its dedicated input type looks like the "correct" one,
check whether the polymorphic `uid` field on the type's options already solves it through the
unified mutation first.

**Resolvers are not a `DtoMapper` job.** `DtoMapper` formats an already-fetched GraphQL response
into the tool's output DTO — it runs after a query/mutation succeeds. `*OptionResolver` classes
solve the opposite problem: translating an agent-supplied human label into the opaque ID a
mutation's *input* needs, before that mutation can be built at all. There's no GraphQL response
to format yet at that point, so `DtoMapper` has no role in it.

**Checkout mutations that feed into an order need a confirm-before-commit gate; cart-edit
mutations don't.** `set_shipping_address`, `set_shipping_method`, `set_payment_method` each take
an optional `confirm` boolean (default `false`, checked via `args.path("confirm").asBoolean(false)`).
Without `confirm: true`, the tool must not call any mutation — it returns a `pending_*` preview
object built purely from the validated input (no `CartMutationClient` call at all) plus
`confirmed: false` and a `message` telling the caller to re-call with `confirm: true`. Only when
`confirm` is `true` does it actually commit and return `confirmed: true` with the real result.
`add_to_cart`/`view_cart`/`update_cart_item`/`clear_cart` deliberately do **not** have this gate —
cart edits are cheap to undo (`update_cart_item` with `quantity: 0`, or `clear_cart`), but a
shipping/payment choice is about to feed into a real order, which isn't undoable in the same way.
`place_order` itself also has no `confirm` flag — calling it *is* the final confirmation; there's
no further "would-be" state to preview once the order exists. See `SetShippingAddressTool`,
`SetShippingMethodTool`, `SetPaymentMethodTool` for the exact pattern — all three shape it
identically (validate required fields first regardless of `confirm`, branch on `confirm` next,
preview branch never touches `CartMutationClient`).

**The `magento-graphql` Java library can lag behind the live GraphQL schema — verify against the
live endpoint before concluding a capability doesn't exist.** While designing order lookup, an
initial `javap` inspection of `Query.class` in every locally available `magento-graphql` version
(including the newest `11.2.2-magento244ee-SNAPSHOT`) found no `guestOrder` field, leading to the
wrong conclusion that anonymous guest-order lookup wasn't possible in this schema at all. It
turned out `guestOrder(input: {number, email, lastname})` *is* a real, documented, unauthenticated
Adobe Commerce GraphQL query (confirmed by testing it directly against the live endpoint) — the
Java client library (a code-generated typed query builder) simply hadn't been regenerated to
include it. **The lesson: `javap`-inspecting the vendored library tells you what the typed builder
supports, not what the live schema supports.** When a capability seems suspiciously absent, test a
raw query directly against the live GraphQL endpoint (`curl` is fine for this) before designing
around its absence. This only affects *new* capabilities being investigated — it can't silently
break any already-shipped tool, since every existing tool's fields exist in both the live schema
and the typed library (otherwise the code wouldn't compile).

**When the typed builder doesn't have a field, hand-write the raw query — don't force-fit an
unrelated typed class.** `GetOrderTool` needs `guestOrder`, which has no generated Java class at
all. Rather than trying to coerce the response into an existing typed class like `CustomerOrder`
(generated for a *different* query context — the Shopify-style codegen used here typically aliases
response fields per query structure, so there's no guarantee a hand class built for one query
shape deserializes correctly from a differently-shaped raw response), it sends a hand-written
GraphQL query string via the raw `GraphqlClient` (`CartMutationClient.resolveGraphqlClient()`) and
deserializes into a plain `com.google.gson.JsonObject`, reading fields off it directly. Use GraphQL
**variables**, not string-interpolated values, when building a hand-written query with
caller-supplied input (`order_number`/`email`/`lastname` here) — string-interpolating untrusted
input into query text risks GraphQL injection, whereas `GraphqlRequest.setVariables(Map)` passes
values through the request's `variables` field, encoded safely by the underlying JSON serialization.

---

## 4. How to add a write tool (author-only) — READ THIS

**This section is about JCR content writes only.** If your tool mutates the remote commerce
backend (cart, and eventually checkout/orders) rather than AEM content, it does **not** belong
here — see §3b above instead; it stays `writesContent() == false` and lives on the shopper
endpoint. The `writesContent()` flag exists specifically to keep JCR content writes off the
anonymous endpoint; a commerce-backend mutation is not what it guards against, and misclassifying
one as `writesContent() == true` would incorrectly hide it from the shopper endpoint where it's
supposed to live.

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
- **`MagentoGraphqlClient.execute()` cannot deserialize mutation responses** — hardcoded to
  `Query.class`. Use `CartMutationClient` (§3b) for any mutation; this is the single most
  time-costly mistake to make here (it fails at runtime, not compile time — the code compiles
  fine and the response just comes back with everything null).
- **A cart tool's endpoint resource is not directly `adaptTo(GraphqlClient.class)`-able.**
  Confirmed live, contrary to what reading `MagentoGraphqlClientImpl`'s happy-path code alone
  suggests. `CartMutationClient.resolveGraphqlClient()` is the fix — don't re-derive this from
  scratch in a new tool, call it.
- **Adding a `magento-graphql`/`graphql-client` test that exercises real (de)serialization
  needs `gson` as an explicit test-scope dependency in `bundles/mcp/pom.xml`.** `provided`-scope
  dependencies (`magento-graphql`, `graphql-client`) do not propagate *their own* transitive
  dependencies to this module's classpath, so `gson` (needed by `MutationDeserializer.getGson()`
  / `QueryDeserializer.getGson()`) isn't there unless declared directly — mirror
  `bundles/core/pom.xml`'s `com.google.code.gson:gson:2.8.9` (test scope). Symptom if missing:
  `NoClassDefFoundError: com/google/gson/internal/Excluder`, only at test *runtime*, not compile.
- **Magento has no bulk/"empty cart" mutation.** `clear_cart` fetches items then calls
  `removeItemFromCart` once per item (N+1 by necessity, not an oversight — don't try to
  "optimize" this into a single call, the mutation doesn't exist).
- **A JSON `null` value in an object argument is not the same as the key being absent —
  Jackson's `NullNode.asText()` returns the *string* `"null"`, not Java `null`.** Found in
  `add_to_cart`'s `options` handling: `{"options": {"fashion_color": null}}` used to be read as
  the literal value `"null"` and fail option matching with a misleading error. Always check
  `!entry.getValue().isNull()` before calling `.asText()` when iterating a JSON object's fields
  into a `Map<String, String>`.
- **`JsonNode.asInt()` truncates fractional numbers instead of rejecting them.** A `quantity` of
  `0.5` silently becomes `0`; a `quantity` of `1.9` silently becomes `1`. Any integer threshold in
  a tool's validation is vulnerable, not just a `0` sentinel: `update_cart_item` treats `0` as
  "remove the item," so a truncated `0.5` gets misrouted into that branch instead of being
  rejected; `add_to_cart` requires `>= 1`, so a truncated `1.9` silently passed as quantity `1`
  before this was caught by review. Guard with `quantityNode.isIntegralNumber()` before calling
  `.asInt()` in every tool that reads a quantity from JSON input, not just the one already fixed —
  this bug was found once, fixed in one tool, and then found again in a sibling tool during the
  next review pass.
- **Cart reads must never be cacheable, even defensively.** `ViewCartTool` calls
  `ctx.getClient().execute(query, HttpMethod.POST)` (not the cacheable default `execute(query)`)
  specifically because cart contents mutate on every `add_to_cart`/`update_cart_item`/
  `clear_cart` call and `CartMutationClient` has no cache-invalidation hook. No OSGi cache
  config in this repo currently caches the `mcp`-selector resource type, so this isn't an active
  bug today — but it's cheap insurance against a future config change silently reintroducing
  stale-cart reads, and any new cart-reading tool should do the same.
- **"Category binding" is two different things — don't conflate page vs component.** A *catalog
  page* is scoped by `magentoRootCategoryId` (+ `magentoRootCategoryIdType`, + `showMainCategories`),
  read by `SiteStructure`/`NavigationImpl` — that's `configure_catalog_page`, written on the page's
  `jcr:content`. A *product-list / carousel component* is pinned by `category`, read by
  `ProductListImpl`/`ProductCarouselImpl` — that's `configure_productlist_component`, written on the
  component resource. Writing `category` on a catalog page node is a **silent no-op** (the original
  `configure_catalog_page` bug). A unit test that only reads back the property it just wrote will NOT
  catch this — validate the *consuming* property name against source, and prove consumption live
  (render/nav), not just a write→readback.
- **`javap`-ing the vendored `magento-graphql` library only tells you what the typed Java builder
  supports, not what the live GraphQL schema supports — the library can lag behind.** This led to
  wrongly concluding anonymous guest-order lookup was impossible, when Magento's `guestOrder` query
  actually exists and works unauthenticated; the Java library just hadn't been regenerated to
  include it. Test a raw query directly against the live endpoint before designing around an
  apparently-missing capability. See §3b's "the `magento-graphql` Java library can lag behind"
  paragraph for the full story and `GetOrderTool` for the hand-written-query pattern this requires
  when the typed builder has no matching method.

---

## 9. Reference material

- Design & plan: `docs/superpowers/specs/2026-07-02-cif-commerce-mcp-design.md`,
  `docs/superpowers/plans/2026-07-02-cif-commerce-mcp.md`. Validation log: repo-root
  `VALIDATION.md`.
- Cart tools (guest cart, simple + configurable products): design
  `docs/superpowers/specs/2026-07-03-cif-shopper-cart-design.md`, plan
  `docs/superpowers/plans/2026-07-03-cif-shopper-cart.md`; configurable-product follow-up design
  `docs/superpowers/specs/2026-07-03-cif-shopper-configurable-cart-design.md`, plan
  `docs/superpowers/plans/2026-07-03-cif-shopper-configurable-cart.md`. Bundle products +
  checkout: `docs/superpowers/specs/2026-07-03-cif-shopper-bundle-and-checkout.md` (design, plan,
  and live verification combined in one doc — includes the bundle-mutation-unification finding
  above). All have a "live verification" section documenting what live testing against a running
  AEM instance found beyond what the design assumed — read before touching cart/checkout tools.
- Key CIF core types to study before extending: `SearchResultsService`, `SearchOptions`,
  `SearchResultsSet`, `SearchFilterService`, `FilterAttributeMetadata`,
  `AbstractProductRetriever`, `AbstractCategoryRetriever`, `UrlProvider`,
  `CategoryUrlFormat.Params`, `SiteStructure`, `ProductListItem` (+ `ProductListItemImpl`),
  `MagentoGraphqlClient`.
- Key `magento-graphql` types for cart/mutation work: `Operations` (`.query`/`.mutation`
  builders), `Mutation`/`MutationQueryDefinition`, `Cart`/`CartQueryDefinition`,
  `CartItemInput`/`CartItemUpdateInput`, `ConfigurableProduct`/`ConfigurableProductOptions`/
  `ConfigurableProductOptionsValues`, `BundleProduct`/`BundleItem`/`BundleItemOption`,
  `CartUserInputError`, `MutationDeserializer`.
- Key `magento-graphql` types for checkout: `SetGuestEmailOnCartInput`,
  `SetShippingAddressesOnCartInput`/`ShippingAddressInput`/`CartAddressInput`,
  `SetBillingAddressOnCartInput`/`BillingAddressInput` (has `sameAsShipping`),
  `SetShippingMethodsOnCartInput`/`ShippingMethodInput`, `AvailableShippingMethod`,
  `SetPaymentMethodOnCartInput`/`PaymentMethodInput`, `AvailablePaymentMethod`,
  `PlaceOrderInput`/`PlaceOrderOutput`/`Order`.
