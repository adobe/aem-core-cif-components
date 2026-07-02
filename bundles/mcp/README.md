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
        ├── dto/DtoMapper.java          # compact product/category DTOs (incl. PDP/PLP links)
        └── tools/                      # the McpTool implementations (below)
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
| `browse_categories` | `{uid?}` | `{category:{uid,name,urlPath,url,children:[…]}}` (`url` = PLP link, on category + children) |
| `get_attributes` | `{}` | `{attributes:[{code,inputType}]}` |
| `resolve_picker_selection` | `{skus:[…]}` | `{items:[{sku,name}]}` (authoring picker helper; read-only) |

Write tools (authoring endpoint only — `writesContent() == true`). They run under the
**caller's** `ResourceResolver` (ACLs enforced) and **fail closed** on a non-`/content`
path or a resource that is not a CIF component/page they understand:

| Tool | Args | Effect |
|---|---|---|
| `configure_product_component` | `{path, sku}` | sets `selection`/`selectionType` on a CIF product component |
| `configure_catalog_page` | `{path, categoryUid}` | sets `category` on a CIF catalog (PLP) page's `jcr:content` |

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

## Known limitations (v1)

- No cart/checkout tools, and no MCP `resources`/`prompts` surface — tools only.
- Shopper **commerce-token pass-through** is not yet implemented: the endpoint operates as a
  guest (public catalog data). Deferred to v2 (see the design spec's open items).
