# CIF Commerce MCP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a new `bundles/mcp` OSGi module to `aem-core-cif-components` that exposes a hand-rolled MCP (Model Context Protocol) server over Sling servlets, mounted on the CIF nav-root page, serving shopper (read) tools anonymously and authoring (read+write) tools on author.

**Architecture:** Two `SlingAllMethodsServlet`s bind to `cq:Page` (selectors `mcp` / `mcp-authoring`) and share an `AbstractMcpServlet` that owns a hand-rolled JSON-RPC 2.0 layer (`initialize` / `tools/list` / `tools/call`). Each servlet gates on the CIF nav-root via `SiteStructure`, resolves store context from the endpoint's own page, and dispatches to `McpTool` OSGi services collected in a `ToolRegistry`. Tools reuse existing CIF services (`SearchResultsService`, `SearchFilterService`) and the public retriever abstraction; results map to compact DTOs.

**Tech Stack:** Java (source level 8, JDK 11 build), OSGi DS annotations, Apache Sling servlets, Jackson (already on the AEM classpath), JUnit 4 + `io.wcm.testing.mock.aem` (aem-mock) + Mockito, Maven (`maven-bundle-plugin`/bnd).

**Reference design:** `docs/superpowers/specs/2026-07-02-cif-commerce-mcp-design.md` (co-located in this repo).

## Global Constraints

- **Repository:** `~/devel/aem-core-cif-components`. New module path: `bundles/mcp`.
- **Parent POM:** `com.adobe.commerce.cif:core-cif-components-parent:2.18.5-SNAPSHOT`.
- **Depends on:** `com.adobe.commerce.cif:core-cif-components-core`, `commerce-cif-magento-graphql`, `commerce-cif-graphql-client` (transitive via core).
- **Java:** source/target level 8, built under JDK 11 (match `bundles/core`).
- **Packaging:** `bundle` (bnd via `maven-bundle-plugin`).
- **License header:** Apache License 2.0 header on every `.java` file (copy the exact block from any `bundles/core` file).
- **Formatting:** Eclipse formatter + impsort + macker run in the build; run `mvn -q formatter:format impsort:sort` before each commit.
- **No external MCP SDK.** JSON handling via Jackson (`com.fasterxml.jackson.databind`) only.
- **Package root:** `com.adobe.cq.commerce.mcp` for exported API; `com.adobe.cq.commerce.mcp.internal.*` for implementation (macker enforces the split).
- **JSON-RPC error codes:** `-32700` parse, `-32600` invalid request, `-32601` method not found, `-32602` invalid params, `-32603` internal, `-32000` tool execution error.
- **MCP protocol version string:** `"2025-06-18"` returned from `initialize`.
- **Build one module:** `mvn -q -pl bundles/mcp -am clean install` from the repo root.
- **Run a single test:** `mvn -q -pl bundles/mcp test -Dtest=ClassName#method`.

---

## File Structure

**Module:** `bundles/mcp/pom.xml`; `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/` (exported SPI); `.../mcp/internal/` (impl); `bundles/mcp/src/test/java/...`; `bundles/mcp/src/test/resources/context/` (aem-mock fixtures).

**Protocol core (no AEM deps):** `mcp/JsonRpc.java`, `mcp/McpTool.java`, `mcp/McpCallContext.java`, `mcp/internal/ToolRegistry.java`, `mcp/internal/JsonRpcDispatcher.java`.

**Transport:** `mcp/internal/servlets/AbstractMcpServlet.java`, `ShopperMcpServlet.java`, `AuthoringMcpServlet.java`.

**Store context + DTOs:** `mcp/internal/StoreContext.java`, `StoreContextResolver.java`, `mcp/internal/dto/DtoMapper.java`, `mcp/internal/McpSearchOptions.java`.

**Tools:** `mcp/internal/tools/{SearchProductsTool, GetAttributesTool, GetProductTool, McpProductRetriever, BrowseCategoriesTool, McpCategoryRetriever, ResolvePickerSelectionTool, ConfigureProductComponentTool, ConfigureCatalogPageTool}.java`.

**Config/packaging:** `config.author` OSGi config (author-only authoring servlet); `all/pom.xml` bundle embed.

---

## Task 1: Scaffold the `bundles/mcp` module

**Files:**
- Create: `bundles/mcp/pom.xml`
- Modify: `pom.xml` (root reactor, add `<module>bundles/mcp</module>` after `bundles/core`)
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/package-info.java`

**Interfaces:**
- Consumes: nothing.
- Produces: a buildable `core-cif-components-mcp` bundle artifact.

- [ ] **Step 1: Create the module POM**

`bundles/mcp/pom.xml` (mirror `bundles/core/pom.xml`'s parent, plugins block, and license header; only coordinates + dependencies differ):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- Apache 2.0 license header copied verbatim from bundles/core/pom.xml -->
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.adobe.commerce.cif</groupId>
        <artifactId>core-cif-components-parent</artifactId>
        <version>2.18.5-SNAPSHOT</version>
        <relativePath>../../parent/pom.xml</relativePath>
    </parent>
    <artifactId>core-cif-components-mcp</artifactId>
    <packaging>bundle</packaging>
    <name>AEM CIF Core Components - MCP Server</name>

    <dependencies>
        <dependency>
            <groupId>com.adobe.commerce.cif</groupId>
            <artifactId>core-cif-components-core</artifactId>
            <version>${project.version}</version>
            <scope>provided</scope>
        </dependency>
        <!-- Copy the following provided-scope deps from bundles/core/pom.xml verbatim:
             org.osgi.* (component annotations, metatype), org.apache.sling.api,
             com.day.cq.wcm (wcm.api), jackson-databind, javax.servlet, jcr,
             commerce-cif-magento-graphql, geronimo-annotation / jsr305, commons-io.
             Test deps: junit, mockito-core, io.wcm.testing.aem-mock.junit4. -->
    </dependencies>

    <build>
        <!-- Copy the entire <plugins> block from bundles/core/pom.xml verbatim:
             maven-bundle-plugin (with sling.bnd.models), formatter, impsort,
             macker, maven-source-plugin. Tasks add Export-Package entries as needed. -->
    </build>
</project>
```

- [ ] **Step 2: Register the module in the root reactor**

Modify root `pom.xml`:

```xml
        <module>bundles/core</module>
        <module>bundles/mcp</module>
```

- [ ] **Step 3: Add an exported package marker**

`bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/package-info.java`:

```java
/* Apache 2.0 header */
@org.osgi.annotation.versioning.Version("1.0.0")
package com.adobe.cq.commerce.mcp;
```

- [ ] **Step 4: Verify the module builds**

Run: `mvn -q -pl bundles/mcp -am clean install`
Expected: `BUILD SUCCESS`, produces `bundles/mcp/target/core-cif-components-mcp-2.18.5-SNAPSHOT.jar`.

- [ ] **Step 5: Commit**

```bash
cd ~/devel/aem-core-cif-components
git add pom.xml bundles/mcp/pom.xml bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/package-info.java
git commit -m "feat(mcp): scaffold core-cif-components-mcp module"
```

---

## Task 2: JSON-RPC envelope + error types

**Files:**
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/JsonRpc.java`
- Test: `bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/JsonRpcTest.java`

**Interfaces:**
- Consumes: Jackson `ObjectMapper`, `JsonNode`.
- Produces: `JsonRpc.Request{String jsonrpc, String method, JsonNode params, JsonNode id}`; `JsonRpc.parse(ObjectMapper, String) : Request`; `JsonRpc.error(JsonNode id, int code, String msg) : ObjectNode`; `JsonRpc.result(JsonNode id, JsonNode result) : ObjectNode`; constants `PARSE_ERROR/INVALID_REQUEST/METHOD_NOT_FOUND/INVALID_PARAMS/INTERNAL_ERROR/TOOL_ERROR`.

- [ ] **Step 1: Write the failing test**

```java
package com.adobe.cq.commerce.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import static org.junit.Assert.*;

public class JsonRpcTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test public void parsesRequest() throws Exception {
        JsonRpc.Request r = JsonRpc.parse(mapper,
            "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}");
        assertEquals("2.0", r.jsonrpc);
        assertEquals("tools/list", r.method);
        assertEquals(1, r.id.asInt());
    }

    @Test public void buildsErrorEnvelope() {
        ObjectNode err = JsonRpc.error(mapper.getNodeFactory().numberNode(7),
            JsonRpc.METHOD_NOT_FOUND, "no such method");
        assertEquals("2.0", err.get("jsonrpc").asText());
        assertEquals(7, err.get("id").asInt());
        assertEquals(-32601, err.get("error").get("code").asInt());
        assertEquals("no such method", err.get("error").get("message").asText());
    }

    @Test public void buildsResultEnvelope() {
        ObjectNode result = mapper.createObjectNode().put("ok", true);
        ObjectNode env = JsonRpc.result(mapper.getNodeFactory().numberNode(9), result);
        assertEquals(9, env.get("id").asInt());
        assertTrue(env.get("result").get("ok").asBoolean());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl bundles/mcp test -Dtest=JsonRpcTest`
Expected: FAIL — `JsonRpc` does not compile / cannot find symbol.

- [ ] **Step 3: Write minimal implementation**

```java
package com.adobe.cq.commerce.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class JsonRpc {
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;
    public static final int TOOL_ERROR = -32000;

    private JsonRpc() {}

    public static final class Request {
        public final String jsonrpc;
        public final String method;
        public final JsonNode params;
        public final JsonNode id;
        public Request(String jsonrpc, String method, JsonNode params, JsonNode id) {
            this.jsonrpc = jsonrpc; this.method = method; this.params = params; this.id = id;
        }
    }

    public static Request parse(ObjectMapper mapper, String body) throws Exception {
        JsonNode n = mapper.readTree(body);
        return new Request(n.path("jsonrpc").asText(null), n.path("method").asText(null),
            n.get("params"), n.get("id"));
    }

    public static ObjectNode result(JsonNode id, JsonNode result) {
        ObjectMapper m = new ObjectMapper();
        ObjectNode env = m.createObjectNode();
        env.put("jsonrpc", "2.0");
        env.set("id", id == null ? m.nullNode() : id);
        env.set("result", result);
        return env;
    }

    public static ObjectNode error(JsonNode id, int code, String message) {
        ObjectMapper m = new ObjectMapper();
        ObjectNode env = m.createObjectNode();
        env.put("jsonrpc", "2.0");
        env.set("id", id == null ? m.nullNode() : id);
        ObjectNode err = m.createObjectNode();
        err.put("code", code);
        err.put("message", message);
        env.set("error", err);
        return env;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl bundles/mcp test -Dtest=JsonRpcTest`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/JsonRpc.java \
        bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/JsonRpcTest.java
git commit -m "feat(mcp): add JSON-RPC envelope and error codes"
```

---

## Task 3: `McpTool` SPI + `McpCallContext`

**Files:**
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/McpTool.java`
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/McpCallContext.java`
- Test: `bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/McpToolTest.java`

**Interfaces:**
- `McpCallContext.getResource() : Resource`; `.getRequest() : SlingHttpServletRequest`; `.getLandingPage() : Page`; `.getProductPage() : Page`.
- `McpTool.name()/description() : String`; `.inputSchema() : ObjectNode`; `.writesContent() : boolean` (default `false`); `.call(McpCallContext, JsonNode) : JsonNode`.

- [ ] **Step 1: Write the failing test**

```java
package com.adobe.cq.commerce.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import static org.junit.Assert.*;

public class McpToolTest {
    @Test public void defaultDoesNotWriteContent() {
        McpTool t = new McpTool() {
            public String name() { return "echo"; }
            public String description() { return "echo"; }
            public ObjectNode inputSchema() { return new ObjectMapper().createObjectNode(); }
            public JsonNode call(McpCallContext ctx, JsonNode args) { return args; }
        };
        assertEquals("echo", t.name());
        assertFalse(t.writesContent());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl bundles/mcp test -Dtest=McpToolTest`
Expected: FAIL — `McpTool` / `McpCallContext` not found.

- [ ] **Step 3: Write minimal implementation**

`McpCallContext.java`:

```java
package com.adobe.cq.commerce.mcp;

import com.day.cq.wcm.api.Page;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;

public interface McpCallContext {
    Resource getResource();
    SlingHttpServletRequest getRequest();
    Page getLandingPage();
    Page getProductPage();
}
```

`McpTool.java`:

```java
package com.adobe.cq.commerce.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.osgi.annotation.versioning.ConsumerType;

@ConsumerType
public interface McpTool {
    String name();
    String description();
    ObjectNode inputSchema();
    default boolean writesContent() { return false; }
    JsonNode call(McpCallContext ctx, JsonNode args) throws Exception;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl bundles/mcp test -Dtest=McpToolTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/McpTool.java \
        bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/McpCallContext.java \
        bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/McpToolTest.java
git commit -m "feat(mcp): add McpTool SPI and McpCallContext"
```

---

## Task 4: `ToolRegistry`

**Files:**
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/ToolRegistry.java`
- Test: `bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/ToolRegistryTest.java`

**Interfaces:**
- `ToolRegistry.bindTool(McpTool)` / `unbindTool(McpTool)` (OSGi dynamic).
- `forSelector(String) : List<McpTool>` — `"mcp"` → read tools (`writesContent()==false`); `"mcp-authoring"` → all.
- `byName(String selector, String name) : McpTool` (null if not visible).

- [ ] **Step 1: Write the failing test**

```java
package com.adobe.cq.commerce.mcp.internal;

import com.adobe.cq.commerce.mcp.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class ToolRegistryTest {
    private McpTool tool(String n, boolean writes) {
        return new McpTool() {
            public String name() { return n; }
            public String description() { return n; }
            public ObjectNode inputSchema() { return new ObjectMapper().createObjectNode(); }
            public boolean writesContent() { return writes; }
            public JsonNode call(McpCallContext c, JsonNode a) { return a; }
        };
    }

    @Test public void shopperSelectorHidesWriteTools() {
        ToolRegistry reg = new ToolRegistry();
        reg.bindTool(tool("search_products", false));
        reg.bindTool(tool("configure_product_component", true));

        List<McpTool> shopper = reg.forSelector("mcp");
        assertEquals(1, shopper.size());
        assertEquals("search_products", shopper.get(0).name());
        assertEquals(2, reg.forSelector("mcp-authoring").size());
        assertNull(reg.byName("mcp", "configure_product_component"));
        assertNotNull(reg.byName("mcp-authoring", "configure_product_component"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl bundles/mcp test -Dtest=ToolRegistryTest`
Expected: FAIL — `ToolRegistry` not found.

- [ ] **Step 3: Write minimal implementation**

```java
package com.adobe.cq.commerce.mcp.internal;

import com.adobe.cq.commerce.mcp.McpTool;
import org.osgi.service.component.annotations.*;
import java.util.*;
import java.util.stream.Collectors;

@Component(service = ToolRegistry.class)
public class ToolRegistry {
    private final List<McpTool> tools = new ArrayList<>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    void bindTool(McpTool tool) { synchronized (tools) { tools.add(tool); } }
    void unbindTool(McpTool tool) { synchronized (tools) { tools.remove(tool); } }

    public List<McpTool> forSelector(String selector) {
        boolean authoring = "mcp-authoring".equals(selector);
        synchronized (tools) {
            return tools.stream()
                .filter(t -> authoring || !t.writesContent())
                .collect(Collectors.toList());
        }
    }

    public McpTool byName(String selector, String name) {
        return forSelector(selector).stream()
            .filter(t -> t.name().equals(name)).findFirst().orElse(null);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl bundles/mcp test -Dtest=ToolRegistryTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/ToolRegistry.java \
        bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/ToolRegistryTest.java
git commit -m "feat(mcp): add ToolRegistry with per-selector visibility"
```

---

## Task 5: `JsonRpcDispatcher`

**Files:**
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/JsonRpcDispatcher.java`
- Test: `bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/JsonRpcDispatcherTest.java`

**Interfaces:**
- `new JsonRpcDispatcher(ObjectMapper, ToolRegistry)`.
- `dispatch(String selector, McpCallContext ctx, JsonRpc.Request req) : ObjectNode` — `initialize` → `{protocolVersion:"2025-06-18", capabilities:{tools:{}}, serverInfo:{name,version}}`; `tools/list` → `{tools:[...]}`; `tools/call` → `{content:[{type:"text",text}], structuredContent}`; unknown tool/method → `METHOD_NOT_FOUND`; tool throw → `TOOL_ERROR`.

- [ ] **Step 1: Write the failing test**

```java
package com.adobe.cq.commerce.mcp.internal;

import com.adobe.cq.commerce.mcp.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.*;
import static org.junit.Assert.*;

public class JsonRpcDispatcherTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private JsonRpcDispatcher dispatcher;

    @Before public void setup() {
        ToolRegistry reg = new ToolRegistry();
        reg.bindTool(new McpTool() {
            public String name() { return "ping"; }
            public String description() { return "ping tool"; }
            public ObjectNode inputSchema() { return mapper.createObjectNode().put("type","object"); }
            public JsonNode call(McpCallContext c, JsonNode a) { return mapper.createObjectNode().put("pong", true); }
        });
        dispatcher = new JsonRpcDispatcher(mapper, reg);
    }

    private JsonRpc.Request req(String method, String params) throws Exception {
        return JsonRpc.parse(mapper,
            "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\""+method+"\",\"params\":"+params+"}");
    }

    @Test public void initializeReportsToolsCapability() throws Exception {
        ObjectNode out = dispatcher.dispatch("mcp", null, req("initialize","{}"));
        assertEquals("2025-06-18", out.get("result").get("protocolVersion").asText());
        assertTrue(out.get("result").get("capabilities").has("tools"));
    }
    @Test public void toolsListReturnsVisibleTools() throws Exception {
        ObjectNode out = dispatcher.dispatch("mcp", null, req("tools/list","{}"));
        assertEquals(1, out.get("result").get("tools").size());
        assertEquals("ping", out.get("result").get("tools").get(0).get("name").asText());
    }
    @Test public void toolsCallInvokesTool() throws Exception {
        ObjectNode out = dispatcher.dispatch("mcp", null,
            req("tools/call","{\"name\":\"ping\",\"arguments\":{}}"));
        assertTrue(out.get("result").get("structuredContent").get("pong").asBoolean());
    }
    @Test public void unknownToolReturnsMethodNotFound() throws Exception {
        ObjectNode out = dispatcher.dispatch("mcp", null,
            req("tools/call","{\"name\":\"nope\",\"arguments\":{}}"));
        assertEquals(-32601, out.get("error").get("code").asInt());
    }
    @Test public void unknownMethodReturnsMethodNotFound() throws Exception {
        ObjectNode out = dispatcher.dispatch("mcp", null, req("frobnicate","{}"));
        assertEquals(-32601, out.get("error").get("code").asInt());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl bundles/mcp test -Dtest=JsonRpcDispatcherTest`
Expected: FAIL — `JsonRpcDispatcher` not found.

- [ ] **Step 3: Write minimal implementation**

```java
package com.adobe.cq.commerce.mcp.internal;

import com.adobe.cq.commerce.mcp.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;

public class JsonRpcDispatcher {
    private final ObjectMapper mapper;
    private final ToolRegistry registry;

    public JsonRpcDispatcher(ObjectMapper mapper, ToolRegistry registry) {
        this.mapper = mapper; this.registry = registry;
    }

    public ObjectNode dispatch(String selector, McpCallContext ctx, JsonRpc.Request req) {
        if (!"2.0".equals(req.jsonrpc) || req.method == null) {
            return JsonRpc.error(req.id, JsonRpc.INVALID_REQUEST, "invalid request");
        }
        switch (req.method) {
            case "initialize": return JsonRpc.result(req.id, initialize());
            case "tools/list": return JsonRpc.result(req.id, toolsList(selector));
            case "tools/call": return toolsCall(selector, ctx, req);
            default: return JsonRpc.error(req.id, JsonRpc.METHOD_NOT_FOUND, "unknown method: " + req.method);
        }
    }

    private ObjectNode initialize() {
        ObjectNode r = mapper.createObjectNode();
        r.put("protocolVersion", "2025-06-18");
        r.putObject("capabilities").putObject("tools");
        ObjectNode info = r.putObject("serverInfo");
        info.put("name", "cif-commerce-mcp");
        info.put("version", "1.0.0");
        return r;
    }

    private ObjectNode toolsList(String selector) {
        ObjectNode r = mapper.createObjectNode();
        ArrayNode arr = r.putArray("tools");
        for (McpTool t : registry.forSelector(selector)) {
            ObjectNode n = arr.addObject();
            n.put("name", t.name());
            n.put("description", t.description());
            n.set("inputSchema", t.inputSchema());
        }
        return r;
    }

    private ObjectNode toolsCall(String selector, McpCallContext ctx, JsonRpc.Request req) {
        JsonNode params = req.params == null ? mapper.createObjectNode() : req.params;
        String name = params.path("name").asText(null);
        JsonNode args = params.has("arguments") ? params.get("arguments") : mapper.createObjectNode();
        McpTool tool = name == null ? null : registry.byName(selector, name);
        if (tool == null) {
            return JsonRpc.error(req.id, JsonRpc.METHOD_NOT_FOUND, "unknown tool: " + name);
        }
        try {
            JsonNode structured = tool.call(ctx, args);
            ObjectNode result = mapper.createObjectNode();
            ObjectNode text = result.putArray("content").addObject();
            text.put("type", "text");
            text.put("text", mapper.writeValueAsString(structured));
            result.set("structuredContent", structured);
            return JsonRpc.result(req.id, result);
        } catch (Exception e) {
            return JsonRpc.error(req.id, JsonRpc.TOOL_ERROR, name + ": " + e.getMessage());
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl bundles/mcp test -Dtest=JsonRpcDispatcherTest`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/JsonRpcDispatcher.java \
        bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/JsonRpcDispatcherTest.java
git commit -m "feat(mcp): add JSON-RPC dispatcher for initialize/tools.list/tools.call"
```

---

## Task 6: `StoreContext` + `StoreContextResolver`

**Files:**
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/StoreContext.java`
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/StoreContextResolver.java`
- Test: `bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/StoreContextResolverTest.java`
- Fixture: `bundles/mcp/src/test/resources/context/venia-navroot.json`

**Interfaces:**
- `StoreContext implements McpCallContext` + `getClient() : MagentoGraphqlClient`.
- `StoreContextResolver.isNavRoot(SlingHttpServletRequest) : boolean` — current page equals `SiteStructure.getLandingPage()`.
- `StoreContextResolver.resolve(SlingHttpServletRequest) : StoreContext` — landingPage = current page; productPage = `siteStructure.getSearchResultsPage().getPage()` if non-null else landingPage; resource = request resource; client = `request.adaptTo(MagentoGraphqlClient.class)`.

- [ ] **Step 1: Create the test fixture**

`bundles/mcp/src/test/resources/context/venia-navroot.json`:

```json
{
  "content": {
    "jcr:primaryType": "sling:Folder",
    "store": {
      "jcr:primaryType": "cq:Page",
      "jcr:content": {
        "jcr:primaryType": "cq:PageContent",
        "sling:resourceType": "core/cif/components/structure/page/v3/page",
        "navRoot": true
      }
    },
    "store-products": {
      "jcr:primaryType": "cq:Page",
      "jcr:content": {
        "jcr:primaryType": "cq:PageContent",
        "sling:resourceType": "core/cif/components/structure/catalogpage/v3/catalogpage"
      }
    }
  }
}
```

- [ ] **Step 2: Write the failing test**

```java
package com.adobe.cq.commerce.mcp.internal;

import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.day.cq.wcm.api.Page;
import org.apache.sling.api.SlingHttpServletRequest;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class StoreContextResolverTest {
    @Rule public final AemContext context = new AemContext();
    private StoreContextResolver resolver;

    @Before public void setup() {
        context.load().json("/context/venia-navroot.json", "/content");
        resolver = new StoreContextResolver();
    }

    private SlingHttpServletRequest requestFor(String pagePath) {
        context.currentResource(pagePath + "/jcr:content");
        SiteStructure ss = mock(SiteStructure.class);
        when(ss.getLandingPage()).thenReturn(context.pageManager().getPage("/content/store"));
        when(ss.getSearchResultsPage()).thenReturn(null);
        context.registerAdapter(Page.class, SiteStructure.class, ss);
        context.registerAdapter(SlingHttpServletRequest.class, MagentoGraphqlClient.class,
            mock(MagentoGraphqlClient.class));
        return context.request();
    }

    @Test public void navRootPageIsRecognised() {
        assertTrue(resolver.isNavRoot(requestFor("/content/store")));
    }
    @Test public void nonNavRootPageIsRejected() {
        assertFalse(resolver.isNavRoot(requestFor("/content/store-products")));
    }
    @Test public void resolveFallsBackToLandingPageForProductPage() {
        StoreContext ctx = resolver.resolve(requestFor("/content/store"));
        assertEquals("/content/store", ctx.getLandingPage().getPath());
        assertEquals("/content/store", ctx.getProductPage().getPath());
        assertNotNull(ctx.getClient());
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `mvn -q -pl bundles/mcp test -Dtest=StoreContextResolverTest`
Expected: FAIL — `StoreContext` / `StoreContextResolver` not found.

- [ ] **Step 4: Write minimal implementation**

`StoreContext.java`:

```java
package com.adobe.cq.commerce.mcp.internal;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.day.cq.wcm.api.Page;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;

public class StoreContext implements McpCallContext {
    private final Resource resource;
    private final SlingHttpServletRequest request;
    private final Page landingPage;
    private final Page productPage;
    private final MagentoGraphqlClient client;

    public StoreContext(Resource resource, SlingHttpServletRequest request,
                        Page landingPage, Page productPage, MagentoGraphqlClient client) {
        this.resource = resource; this.request = request;
        this.landingPage = landingPage; this.productPage = productPage; this.client = client;
    }
    public Resource getResource() { return resource; }
    public SlingHttpServletRequest getRequest() { return request; }
    public Page getLandingPage() { return landingPage; }
    public Page getProductPage() { return productPage; }
    public MagentoGraphqlClient getClient() { return client; }
}
```

`StoreContextResolver.java`:

```java
package com.adobe.cq.commerce.mcp.internal;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;

@Component(service = StoreContextResolver.class)
public class StoreContextResolver {

    private Page currentPage(SlingHttpServletRequest request) {
        Resource r = request.getResource();
        PageManager pm = r.getResourceResolver().adaptTo(PageManager.class);
        return pm == null ? null : pm.getContainingPage(r);
    }

    public boolean isNavRoot(SlingHttpServletRequest request) {
        Page page = currentPage(request);
        if (page == null) return false;
        SiteStructure ss = page.adaptTo(SiteStructure.class);
        if (ss == null) return false;
        Page landing = ss.getLandingPage();
        return landing != null && landing.getPath().equals(page.getPath());
    }

    public StoreContext resolve(SlingHttpServletRequest request) {
        Page landing = currentPage(request);
        SiteStructure ss = landing.adaptTo(SiteStructure.class);
        Page productPage = landing;
        if (ss != null && ss.getSearchResultsPage() != null
                && ss.getSearchResultsPage().getPage() != null) {
            productPage = ss.getSearchResultsPage().getPage();
        }
        MagentoGraphqlClient client = request.adaptTo(MagentoGraphqlClient.class);
        return new StoreContext(request.getResource(), request, landing, productPage, client);
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn -q -pl bundles/mcp test -Dtest=StoreContextResolverTest`
Expected: PASS (3 tests).

- [ ] **Step 6: Commit**

```bash
git add bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/StoreContext.java \
        bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/StoreContextResolver.java \
        bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/StoreContextResolverTest.java \
        bundles/mcp/src/test/resources/context/venia-navroot.json
git commit -m "feat(mcp): add StoreContext and nav-root resolver via SiteStructure"
```

---

## Task 7: `AbstractMcpServlet` (transport + nav-root gate)

**Files:**
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/servlets/AbstractMcpServlet.java`
- Test: `bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/servlets/AbstractMcpServletTest.java`

**Interfaces:**
- `AbstractMcpServlet extends SlingAllMethodsServlet`; abstract `String selector()`; protected `init(ObjectMapper, JsonRpcDispatcher, StoreContextResolver)` (non-final fields, so both the constructor and OSGi `@Activate` can set them).
- `doPost` — body over `65536` → `413`; `!resolver.isNavRoot(request)` → `404`; parse error → `-32700` envelope (HTTP 200); else resolve `StoreContext` and `dispatch(selector(), ctx, req)`.
- `doGet` → `405`.

- [ ] **Step 1: Write the failing test**

```java
package com.adobe.cq.commerce.mcp.internal.servlets;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.mcp.*;
import com.adobe.cq.commerce.mcp.internal.*;
import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AbstractMcpServletTest {
    @Rule public final AemContext context = new AemContext();
    private final ObjectMapper mapper = new ObjectMapper();

    private TestServlet servletFor(String pagePath, String landing) {
        context.load().json("/context/venia-navroot.json", "/content");
        context.currentResource(pagePath + "/jcr:content");
        SiteStructure ss = mock(SiteStructure.class);
        when(ss.getLandingPage()).thenReturn(context.pageManager().getPage(landing));
        when(ss.getSearchResultsPage()).thenReturn(null);
        context.registerAdapter(Page.class, SiteStructure.class, ss);
        context.registerAdapter(SlingHttpServletRequest.class, MagentoGraphqlClient.class,
            mock(MagentoGraphqlClient.class));

        ToolRegistry reg = new ToolRegistry();
        reg.bindTool(new McpTool() {
            public String name() { return "ping"; }
            public String description() { return "ping"; }
            public ObjectNode inputSchema() { return mapper.createObjectNode(); }
            public JsonNode call(McpCallContext c, JsonNode a) { return mapper.createObjectNode().put("pong", true); }
        });
        return new TestServlet(mapper, new JsonRpcDispatcher(mapper, reg), new StoreContextResolver());
    }

    static class TestServlet extends AbstractMcpServlet {
        TestServlet(ObjectMapper m, JsonRpcDispatcher d, StoreContextResolver r) { super(m, d, r); }
        protected String selector() { return "mcp"; }
    }

    @Test public void non_navroot_returns_404() throws Exception {
        TestServlet servlet = servletFor("/content/store-products", "/content/store");
        context.request().setContent("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}".getBytes("UTF-8"));
        MockSlingHttpServletResponse resp = context.response();
        servlet.doPost(context.request(), resp);
        assertEquals(404, resp.getStatus());
    }

    @Test public void navroot_dispatches_tools_call() throws Exception {
        TestServlet servlet = servletFor("/content/store", "/content/store");
        context.request().setContent(
            "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"ping\",\"arguments\":{}}}"
            .getBytes("UTF-8"));
        MockSlingHttpServletResponse resp = context.response();
        servlet.doPost(context.request(), resp);
        assertEquals(200, resp.getStatus());
        JsonNode out = mapper.readTree(resp.getOutputAsString());
        assertTrue(out.get("result").get("structuredContent").get("pong").asBoolean());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl bundles/mcp test -Dtest=AbstractMcpServletTest`
Expected: FAIL — `AbstractMcpServlet` not found.

- [ ] **Step 3: Write minimal implementation**

```java
package com.adobe.cq.commerce.mcp.internal.servlets;

import com.adobe.cq.commerce.mcp.JsonRpc;
import com.adobe.cq.commerce.mcp.internal.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.*;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

import javax.servlet.ServletException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class AbstractMcpServlet extends SlingAllMethodsServlet {
    private static final int MAX_BODY = 65536;

    private transient ObjectMapper mapper;
    private transient JsonRpcDispatcher dispatcher;
    private transient StoreContextResolver resolver;

    protected AbstractMcpServlet() {}
    protected AbstractMcpServlet(ObjectMapper mapper, JsonRpcDispatcher dispatcher,
                                 StoreContextResolver resolver) {
        init(mapper, dispatcher, resolver);
    }
    protected final void init(ObjectMapper mapper, JsonRpcDispatcher dispatcher,
                              StoreContextResolver resolver) {
        this.mapper = mapper; this.dispatcher = dispatcher; this.resolver = resolver;
    }

    protected abstract String selector();

    @Override
    protected void doGet(SlingHttpServletRequest req, SlingHttpServletResponse resp) throws IOException {
        resp.sendError(405, "Use POST with a JSON-RPC body");
    }

    @Override
    protected void doPost(SlingHttpServletRequest req, SlingHttpServletResponse resp)
            throws ServletException, IOException {
        String body = IOUtils.toString(req.getInputStream(), StandardCharsets.UTF_8);
        if (body.length() > MAX_BODY) { resp.sendError(413, "Request too large"); return; }
        if (!resolver.isNavRoot(req)) { resp.sendError(404); return; }

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        JsonRpc.Request rpc;
        try {
            rpc = JsonRpc.parse(mapper, body);
        } catch (Exception e) {
            write(resp, JsonRpc.error(null, JsonRpc.PARSE_ERROR, "parse error"));
            return;
        }
        StoreContext ctx = resolver.resolve(req);
        write(resp, dispatcher.dispatch(selector(), ctx, rpc));
    }

    private void write(SlingHttpServletResponse resp, ObjectNode node) throws IOException {
        resp.getWriter().write(mapper.writeValueAsString(node));
    }
}
```

Note: the shopper commerce token flows through automatically — `resolver.resolve` obtains the request-scoped `MagentoGraphqlClient`, which carries the request's `Authorization` header per CIF's client. No explicit code path here.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl bundles/mcp test -Dtest=AbstractMcpServletTest`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/servlets/AbstractMcpServlet.java \
        bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/servlets/AbstractMcpServletTest.java
git commit -m "feat(mcp): add AbstractMcpServlet with nav-root gate and JSON-RPC transport"
```

---

## Task 8: `ShopperMcpServlet` + `AuthoringMcpServlet`

**Files:**
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/servlets/ShopperMcpServlet.java`
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/servlets/AuthoringMcpServlet.java`
- Test: `bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/servlets/ServletRegistrationTest.java`

**Interfaces:**
- Two OSGi `Servlet` components, both `cq:Page` + `POST` + `json`, selectors `mcp` / `mcp-authoring`; each `@Reference`s `ToolRegistry` + `StoreContextResolver` and builds its `JsonRpcDispatcher` in `@Activate`.

- [ ] **Step 1: Write the failing test**

```java
package com.adobe.cq.commerce.mcp.internal.servlets;

import org.junit.Test;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

public class ServletRegistrationTest {
    private String selectorOf(AbstractMcpServlet s) throws Exception {
        Method m = AbstractMcpServlet.class.getDeclaredMethod("selector");
        m.setAccessible(true);
        return (String) m.invoke(s);
    }
    @Test public void shopperSelector() throws Exception { assertEquals("mcp", selectorOf(new ShopperMcpServlet())); }
    @Test public void authoringSelector() throws Exception { assertEquals("mcp-authoring", selectorOf(new AuthoringMcpServlet())); }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl bundles/mcp test -Dtest=ServletRegistrationTest`
Expected: FAIL — servlet classes not found.

- [ ] **Step 3: Write minimal implementation**

`ShopperMcpServlet.java`:

```java
package com.adobe.cq.commerce.mcp.internal.servlets;

import com.adobe.cq.commerce.mcp.internal.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.osgi.service.component.annotations.*;
import javax.servlet.Servlet;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.resourceTypes=cq:Page",
        "sling.servlet.selectors=mcp",
        "sling.servlet.extensions=json",
        "sling.servlet.methods=POST"
    })
public class ShopperMcpServlet extends AbstractMcpServlet {
    @Reference private transient ToolRegistry registry;
    @Reference private transient StoreContextResolver resolver;

    @Activate
    void activate() {
        init(new ObjectMapper(), new JsonRpcDispatcher(new ObjectMapper(), registry), resolver);
    }
    protected String selector() { return "mcp"; }
}
```

`AuthoringMcpServlet.java` — identical structure, selectors `mcp-authoring`, `selector()` returns `"mcp-authoring"`:

```java
package com.adobe.cq.commerce.mcp.internal.servlets;

import com.adobe.cq.commerce.mcp.internal.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.osgi.service.component.annotations.*;
import javax.servlet.Servlet;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.resourceTypes=cq:Page",
        "sling.servlet.selectors=mcp-authoring",
        "sling.servlet.extensions=json",
        "sling.servlet.methods=POST"
    })
public class AuthoringMcpServlet extends AbstractMcpServlet {
    @Reference private transient ToolRegistry registry;
    @Reference private transient StoreContextResolver resolver;

    @Activate
    void activate() {
        init(new ObjectMapper(), new JsonRpcDispatcher(new ObjectMapper(), registry), resolver);
    }
    protected String selector() { return "mcp-authoring"; }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl bundles/mcp test -Dtest=ServletRegistrationTest`
Expected: PASS. Also re-run Task 7's test to confirm no regression: `mvn -q -pl bundles/mcp test -Dtest=AbstractMcpServletTest`.

- [ ] **Step 5: Commit**

```bash
git add bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/servlets/ShopperMcpServlet.java \
        bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/servlets/AuthoringMcpServlet.java \
        bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/servlets/ServletRegistrationTest.java
git commit -m "feat(mcp): add shopper and authoring MCP servlets bound to cq:Page"
```

---

## Task 9: DTOs + `DtoMapper`

**Files:**
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/dto/DtoMapper.java`
- Test: `bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/dto/DtoMapperTest.java`

**Interfaces:**
- `DtoMapper.product(ObjectMapper, ProductListItem) : ObjectNode` → `{sku,name,slug,imageUrl,imageAlt,price,currency}`.
- `DtoMapper.category(ObjectMapper, CategoryInterface, boolean withChildren) : ObjectNode` → `{uid,name,urlPath,children?}`.

- [ ] **Step 1: Write the failing test**

```java
package com.adobe.cq.commerce.mcp.internal.dto;

import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.common.Price;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DtoMapperTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test public void mapsProductListItem() {
        Price price = mock(Price.class);
        when(price.getFinalPrice()).thenReturn(19.99);
        when(price.getCurrency()).thenReturn("USD");
        ProductListItem item = mock(ProductListItem.class);
        when(item.getSKU()).thenReturn("VT01");
        when(item.getTitle()).thenReturn("Valeria Two-Layer Tank");
        when(item.getSlug()).thenReturn("valeria-tank");
        when(item.getImageURL()).thenReturn("http://x/img.jpg");
        when(item.getImageAlt()).thenReturn("tank");
        when(item.getPriceRange()).thenReturn(price);

        ObjectNode dto = DtoMapper.product(mapper, item);
        assertEquals("VT01", dto.get("sku").asText());
        assertEquals("Valeria Two-Layer Tank", dto.get("name").asText());
        assertEquals("valeria-tank", dto.get("slug").asText());
        assertEquals(19.99, dto.get("price").asDouble(), 0.001);
        assertEquals("USD", dto.get("currency").asText());
    }
}
```

Note: confirm `ProductListItem.getTitle()` is the display-name getter on the interface (alongside `getSKU`); adjust if the exact name differs.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl bundles/mcp test -Dtest=DtoMapperTest`
Expected: FAIL — `DtoMapper` not found.

- [ ] **Step 3: Write minimal implementation**

```java
package com.adobe.cq.commerce.mcp.internal.dto;

import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;

public final class DtoMapper {
    private DtoMapper() {}

    public static ObjectNode product(ObjectMapper m, ProductListItem item) {
        ObjectNode n = m.createObjectNode();
        n.put("sku", item.getSKU());
        n.put("name", item.getTitle());
        n.put("slug", item.getSlug());
        n.put("imageUrl", item.getImageURL());
        n.put("imageAlt", item.getImageAlt());
        Price p = item.getPriceRange();
        if (p != null) {
            n.put("price", p.getFinalPrice());
            n.put("currency", p.getCurrency());
        }
        return n;
    }

    public static ObjectNode category(ObjectMapper m, CategoryInterface cat, boolean withChildren) {
        ObjectNode n = m.createObjectNode();
        n.put("uid", cat.getUid() != null ? cat.getUid().toString() : null);
        n.put("name", cat.getName());
        n.put("urlPath", cat.getUrlPath());
        if (withChildren && cat instanceof CategoryTree) {
            List<CategoryTree> children = ((CategoryTree) cat).getChildren();
            if (children != null) {
                ArrayNode arr = n.putArray("children");
                for (CategoryTree c : children) arr.add(category(m, c, false));
            }
        }
        return n;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl bundles/mcp test -Dtest=DtoMapperTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/dto/DtoMapper.java \
        bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/dto/DtoMapperTest.java
git commit -m "feat(mcp): add DTO mapper for product and category results"
```

---

## Task 10: `McpSearchOptions` + `SearchProductsTool`

**Files:**
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/McpSearchOptions.java`
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/SearchProductsTool.java`
- Test: `bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/SearchProductsToolTest.java`

**Interfaces:**
- `McpSearchOptions implements SearchOptions` with `setSearchQuery/setCurrentPage/setPageSize/putFilter`.
- `SearchProductsTool` (`McpTool` `search_products`); args `{query?,page?,pageSize?,filters?}`; result `{total, items:[productDto]}`; field `SearchResultsService searchResultsService`.

- [ ] **Step 1: Write the failing test**

```java
package com.adobe.cq.commerce.mcp.internal.tools;

import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.core.search.models.SearchOptions;
import com.adobe.cq.commerce.core.search.services.SearchResultsService;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.databind.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SearchProductsToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test public void searchesAndMapsResults() throws Exception {
        Price price = mock(Price.class);
        when(price.getFinalPrice()).thenReturn(19.99); when(price.getCurrency()).thenReturn("USD");
        ProductListItem item = mock(ProductListItem.class);
        when(item.getSKU()).thenReturn("VT01"); when(item.getTitle()).thenReturn("Tank");
        when(item.getSlug()).thenReturn("tank"); when(item.getPriceRange()).thenReturn(price);

        SearchResultsSet resultsSet = mock(SearchResultsSet.class);
        when(resultsSet.getProductListItems()).thenReturn(Collections.singletonList(item));
        when(resultsSet.getTotalResults()).thenReturn(1);

        SearchResultsService service = mock(SearchResultsService.class);
        when(service.performSearch(any(SearchOptions.class), any(Resource.class),
             any(Page.class), any(SlingHttpServletRequest.class))).thenReturn(resultsSet);

        SearchProductsTool tool = new SearchProductsTool();
        tool.searchResultsService = service;

        StoreContext ctx = mock(StoreContext.class);
        when(ctx.getResource()).thenReturn(mock(Resource.class));
        when(ctx.getRequest()).thenReturn(mock(SlingHttpServletRequest.class));
        when(ctx.getProductPage()).thenReturn(mock(Page.class));

        JsonNode out = tool.call(ctx, mapper.readTree("{\"query\":\"tank\",\"page\":1,\"pageSize\":12}"));
        assertEquals(1, out.get("total").asInt());
        assertEquals("VT01", out.get("items").get(0).get("sku").asText());
        assertEquals("search_products", tool.name());
        assertFalse(tool.writesContent());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl bundles/mcp test -Dtest=SearchProductsToolTest`
Expected: FAIL — classes not found.

- [ ] **Step 3: Write `McpSearchOptions`**

```java
package com.adobe.cq.commerce.mcp.internal;

import com.adobe.cq.commerce.core.search.models.SearchOptions;
import com.adobe.cq.commerce.core.search.models.SorterKey;
import com.adobe.cq.commerce.core.search.models.Sorter;
import java.util.*;

public class McpSearchOptions implements SearchOptions {
    private String query;
    private int currentPage = 1;
    private int pageSize = 20;
    private final Map<String, String> attributeFilters = new HashMap<>();
    private final List<SorterKey> sorterKeys = new ArrayList<>();

    public void setSearchQuery(String q) { this.query = q; }
    public void setCurrentPage(int p) { this.currentPage = p; }
    public void setPageSize(int s) { this.pageSize = s; }
    public void putFilter(String k, String v) { attributeFilters.put(k, v); }

    @Override public Optional<String> getSearchQuery() { return Optional.ofNullable(query); }
    @Override public int getCurrentPage() { return currentPage; }
    @Override public int getPageSize() { return pageSize; }
    @Override public Map<String, String> getAttributeFilters() { return attributeFilters; }
    @Override public Map<String, String> getAllFilters() { return attributeFilters; }
    @Override public void addSorterKey(String name, String label, Sorter.Order order) { /* v1: sorting optional */ }
    @Override public List<SorterKey> getSorterKeys() { return sorterKeys; }
}
```

Note: implement any other abstract methods the compiler reports on `SearchOptions`; keep them minimal.

- [ ] **Step 4: Write `SearchProductsTool`**

```java
package com.adobe.cq.commerce.mcp.internal.tools;

import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.core.search.services.SearchResultsService;
import com.adobe.cq.commerce.mcp.*;
import com.adobe.cq.commerce.mcp.internal.McpSearchOptions;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.adobe.cq.commerce.mcp.internal.dto.DtoMapper;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.osgi.service.component.annotations.*;
import java.util.Iterator;
import java.util.Map;

@Component(service = McpTool.class)
public class SearchProductsTool implements McpTool {
    @Reference SearchResultsService searchResultsService;
    private final ObjectMapper mapper = new ObjectMapper();

    public String name() { return "search_products"; }
    public String description() { return "Search the commerce catalog by keyword and optional attribute filters."; }

    public ObjectNode inputSchema() {
        ObjectNode s = mapper.createObjectNode();
        s.put("type", "object");
        ObjectNode props = s.putObject("properties");
        props.putObject("query").put("type", "string");
        props.putObject("page").put("type", "integer");
        props.putObject("pageSize").put("type", "integer");
        props.putObject("filters").put("type", "object");
        return s;
    }

    public JsonNode call(McpCallContext c, JsonNode args) {
        StoreContext ctx = (StoreContext) c;
        McpSearchOptions options = new McpSearchOptions();
        if (args.hasNonNull("query")) options.setSearchQuery(args.get("query").asText());
        options.setCurrentPage(args.path("page").asInt(1));
        options.setPageSize(args.path("pageSize").asInt(20));
        if (args.has("filters") && args.get("filters").isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = args.get("filters").fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                options.putFilter(e.getKey(), e.getValue().asText());
            }
        }

        SearchResultsSet results = searchResultsService.performSearch(
            options, ctx.getResource(), ctx.getProductPage(), ctx.getRequest());

        ObjectNode out = mapper.createObjectNode();
        out.put("total", results.getTotalResults() == null ? 0 : results.getTotalResults());
        ArrayNode items = out.putArray("items");
        for (ProductListItem item : results.getProductListItems()) items.add(DtoMapper.product(mapper, item));
        return out;
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn -q -pl bundles/mcp test -Dtest=SearchProductsToolTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/McpSearchOptions.java \
        bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/SearchProductsTool.java \
        bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/SearchProductsToolTest.java
git commit -m "feat(mcp): add search_products tool over SearchResultsService"
```

---

## Task 11: `GetAttributesTool`

**Files:**
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/GetAttributesTool.java`
- Test: `bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/GetAttributesToolTest.java`

**Interfaces:**
- `GetAttributesTool` (`McpTool` `get_attributes`); field `SearchFilterService searchFilterService`; result `{attributes:[{code,inputType}]}` from `retrieveCurrentlyAvailableCommerceFilters(ctx.getLandingPage())`.

- [ ] **Step 1: Write the failing test**

```java
package com.adobe.cq.commerce.mcp.internal.tools;

import com.adobe.cq.commerce.core.search.models.FilterAttributeMetadata;
import com.adobe.cq.commerce.core.search.services.SearchFilterService;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.databind.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GetAttributesToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test public void listsFilterableAttributes() throws Exception {
        FilterAttributeMetadata meta = mock(FilterAttributeMetadata.class);
        when(meta.getAttributeCode()).thenReturn("color");
        when(meta.getFilterInputType()).thenReturn("equal");

        SearchFilterService service = mock(SearchFilterService.class);
        when(service.retrieveCurrentlyAvailableCommerceFilters(any(Page.class)))
            .thenReturn(Collections.singletonList(meta));

        GetAttributesTool tool = new GetAttributesTool();
        tool.searchFilterService = service;

        StoreContext ctx = mock(StoreContext.class);
        when(ctx.getLandingPage()).thenReturn(mock(Page.class));

        JsonNode out = tool.call(ctx, mapper.createObjectNode());
        assertEquals("color", out.get("attributes").get(0).get("code").asText());
        assertEquals("get_attributes", tool.name());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl bundles/mcp test -Dtest=GetAttributesToolTest`
Expected: FAIL — class not found.

- [ ] **Step 3: Write minimal implementation**

```java
package com.adobe.cq.commerce.mcp.internal.tools;

import com.adobe.cq.commerce.core.search.models.FilterAttributeMetadata;
import com.adobe.cq.commerce.core.search.services.SearchFilterService;
import com.adobe.cq.commerce.mcp.*;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.osgi.service.component.annotations.*;

@Component(service = McpTool.class)
public class GetAttributesTool implements McpTool {
    @Reference SearchFilterService searchFilterService;
    private final ObjectMapper mapper = new ObjectMapper();

    public String name() { return "get_attributes"; }
    public String description() { return "List filterable product attributes for the store."; }
    public ObjectNode inputSchema() { return mapper.createObjectNode().put("type", "object"); }

    public JsonNode call(McpCallContext c, JsonNode args) {
        StoreContext ctx = (StoreContext) c;
        ObjectNode out = mapper.createObjectNode();
        ArrayNode arr = out.putArray("attributes");
        for (FilterAttributeMetadata m :
                searchFilterService.retrieveCurrentlyAvailableCommerceFilters(ctx.getLandingPage())) {
            ObjectNode n = arr.addObject();
            n.put("code", m.getAttributeCode());
            n.put("inputType", m.getFilterInputType());
        }
        return out;
    }
}
```

Note: verify `FilterAttributeMetadata` getter names (`getAttributeCode`, `getFilterInputType`) in `core-cif-components`; adjust if different.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl bundles/mcp test -Dtest=GetAttributesToolTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/GetAttributesTool.java \
        bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/GetAttributesToolTest.java
git commit -m "feat(mcp): add get_attributes tool over SearchFilterService"
```

---

## Task 12: `GetProductTool` + `McpProductRetriever`

**Files:**
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/McpProductRetriever.java`
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/GetProductTool.java`
- Test: `bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/GetProductToolTest.java`

**Reference to copy the retriever query pattern from:** `bundles/core/src/main/java/com/adobe/cq/commerce/core/components/internal/models/v1/productteaser/ProductRetriever.java` — copy its query-definition (fields: sku, name, url_key, image, price_range) into `McpProductRetriever`. Implement exactly the abstract methods the compiler reports on `AbstractProductRetriever`.

**Interfaces:**
- `McpProductRetriever(MagentoGraphqlClient)` + `setIdentifier(String)` + `fetchProduct() : ProductInterface`.
- `GetProductTool` (`McpTool` `get_product`); args `{sku}`; result `{sku,name,urlKey}`; test seam `protected ProductInterface fetch(StoreContext, String sku)`.

- [ ] **Step 1: Write the failing test**

```java
package com.adobe.cq.commerce.mcp.internal.tools;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GetProductToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test public void returnsProductForSku() throws Exception {
        ProductInterface product = mock(ProductInterface.class);
        when(product.getSku()).thenReturn("VT01");
        when(product.getName()).thenReturn("Tank");
        when(product.getUrlKey()).thenReturn("tank");

        StoreContext ctx = mock(StoreContext.class);
        when(ctx.getClient()).thenReturn(mock(MagentoGraphqlClient.class));

        GetProductTool tool = new GetProductTool() {
            protected ProductInterface fetch(StoreContext c, String sku) { return product; }
        };
        JsonNode out = tool.call(ctx, mapper.createObjectNode().put("sku", "VT01"));
        assertEquals("VT01", out.get("sku").asText());
        assertEquals("Tank", out.get("name").asText());
        assertEquals("get_product", tool.name());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl bundles/mcp test -Dtest=GetProductToolTest`
Expected: FAIL — classes not found.

- [ ] **Step 3: Write `McpProductRetriever`**

```java
package com.adobe.cq.commerce.mcp.internal.tools;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
// import query-builder types used by the copied query definition

public class McpProductRetriever extends AbstractProductRetriever {
    public McpProductRetriever(MagentoGraphqlClient client) { super(client); }

    // Paste the generateQuery(String)/defineProductsQuery(...) body from the
    // productteaser ProductRetriever reference, trimmed to:
    //   sku, name, url_key, image { url label },
    //   price_range { minimum_price { final_price { value currency } } }
    // Implement exactly the abstract methods the compiler reports.
}
```

- [ ] **Step 4: Write `GetProductTool`**

```java
package com.adobe.cq.commerce.mcp.internal.tools;

import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.mcp.*;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.osgi.service.component.annotations.Component;

@Component(service = McpTool.class)
public class GetProductTool implements McpTool {
    private final ObjectMapper mapper = new ObjectMapper();

    public String name() { return "get_product"; }
    public String description() { return "Fetch a single product by SKU."; }
    public ObjectNode inputSchema() {
        ObjectNode s = mapper.createObjectNode().put("type", "object");
        s.putObject("properties").putObject("sku").put("type", "string");
        s.putArray("required").add("sku");
        return s;
    }

    protected ProductInterface fetch(StoreContext ctx, String sku) {
        McpProductRetriever r = new McpProductRetriever(ctx.getClient());
        r.setIdentifier(sku);
        return r.fetchProduct();
    }

    public JsonNode call(McpCallContext c, JsonNode args) {
        StoreContext ctx = (StoreContext) c;
        String sku = args.path("sku").asText(null);
        if (sku == null) throw new IllegalArgumentException("sku is required");
        ProductInterface p = fetch(ctx, sku);
        ObjectNode out = mapper.createObjectNode();
        if (p == null) { out.putNull("sku"); return out; }
        out.put("sku", p.getSku());
        out.put("name", p.getName());
        out.put("urlKey", p.getUrlKey());
        return out;
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn -q -pl bundles/mcp test -Dtest=GetProductToolTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/McpProductRetriever.java \
        bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/GetProductTool.java \
        bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/GetProductToolTest.java
git commit -m "feat(mcp): add get_product tool via product retriever"
```

---

## Task 13: `BrowseCategoriesTool` + `McpCategoryRetriever`

**Files:**
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/McpCategoryRetriever.java`
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/BrowseCategoriesTool.java`
- Test: `bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/BrowseCategoriesToolTest.java`

**Reference to copy the retriever query pattern from:** `bundles/core/src/main/java/com/adobe/cq/commerce/core/components/internal/models/v1/button/CategoryRetriever.java` — copy its category-query definition (uid, name, url_path, children { uid name url_path }) into `McpCategoryRetriever`.

**Interfaces:**
- `McpCategoryRetriever(MagentoGraphqlClient)` + `setIdentifier(String)` + `fetchCategory() : CategoryInterface`.
- `BrowseCategoriesTool` (`McpTool` `browse_categories`); args `{uid?}`; result `{category: categoryDto+children}`; test seam `protected CategoryInterface fetch(StoreContext, String uid)`.

- [ ] **Step 1: Write the failing test**

```java
package com.adobe.cq.commerce.mcp.internal.tools;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BrowseCategoriesToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test public void returnsCategoryTree() throws Exception {
        CategoryTree cat = mock(CategoryTree.class);
        when(cat.getName()).thenReturn("Tops");
        when(cat.getUrlPath()).thenReturn("tops");

        StoreContext ctx = mock(StoreContext.class);
        when(ctx.getClient()).thenReturn(mock(MagentoGraphqlClient.class));

        BrowseCategoriesTool tool = new BrowseCategoriesTool() {
            protected CategoryInterface fetch(StoreContext c, String uid) { return cat; }
        };
        JsonNode out = tool.call(ctx, mapper.createObjectNode());
        assertEquals("Tops", out.get("category").get("name").asText());
        assertEquals("browse_categories", tool.name());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl bundles/mcp test -Dtest=BrowseCategoriesToolTest`
Expected: FAIL — classes not found.

- [ ] **Step 3: Write `McpCategoryRetriever`** (mirror `McpProductRetriever`; category query copied from the button `CategoryRetriever` reference; implement exactly the abstract methods the compiler reports on `AbstractCategoryRetriever`).

- [ ] **Step 4: Write `BrowseCategoriesTool`**

```java
package com.adobe.cq.commerce.mcp.internal.tools;

import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.mcp.*;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.adobe.cq.commerce.mcp.internal.dto.DtoMapper;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.osgi.service.component.annotations.Component;

@Component(service = McpTool.class)
public class BrowseCategoriesTool implements McpTool {
    private final ObjectMapper mapper = new ObjectMapper();

    public String name() { return "browse_categories"; }
    public String description() { return "Browse the category tree; returns a category and its children."; }
    public ObjectNode inputSchema() {
        ObjectNode s = mapper.createObjectNode().put("type", "object");
        s.putObject("properties").putObject("uid").put("type", "string");
        return s;
    }

    protected CategoryInterface fetch(StoreContext ctx, String uid) {
        McpCategoryRetriever r = new McpCategoryRetriever(ctx.getClient());
        if (uid != null) r.setIdentifier(uid);
        return r.fetchCategory();
    }

    public JsonNode call(McpCallContext c, JsonNode args) {
        StoreContext ctx = (StoreContext) c;
        String uid = args.hasNonNull("uid") ? args.get("uid").asText() : null;
        CategoryInterface cat = fetch(ctx, uid);
        ObjectNode out = mapper.createObjectNode();
        out.set("category", cat == null ? mapper.nullNode() : DtoMapper.category(mapper, cat, true));
        return out;
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn -q -pl bundles/mcp test -Dtest=BrowseCategoriesToolTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/McpCategoryRetriever.java \
        bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/BrowseCategoriesTool.java \
        bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/BrowseCategoriesToolTest.java
git commit -m "feat(mcp): add browse_categories tool via category retriever"
```

---

## Task 14: `ResolvePickerSelectionTool` (author read tool)

**Files:**
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/ResolvePickerSelectionTool.java`
- Test: `bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/ResolvePickerSelectionToolTest.java`

**Interfaces:**
- `ResolvePickerSelectionTool` (`McpTool` `resolve_picker_selection`, read-only); args `{skus:[string]}`; result `{items:[{sku,name}]}`; test seam `protected ProductInterface fetch(StoreContext, String sku)` (real impl reuses `McpProductRetriever`).

- [ ] **Step 1: Write the failing test**

```java
package com.adobe.cq.commerce.mcp.internal.tools;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ResolvePickerSelectionToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test public void resolvesEachSku() throws Exception {
        ProductInterface p = mock(ProductInterface.class);
        when(p.getSku()).thenReturn("VT01"); when(p.getName()).thenReturn("Tank");

        StoreContext ctx = mock(StoreContext.class);
        when(ctx.getClient()).thenReturn(mock(MagentoGraphqlClient.class));

        ResolvePickerSelectionTool tool = new ResolvePickerSelectionTool() {
            protected ProductInterface fetch(StoreContext c, String sku) { return p; }
        };
        JsonNode out = tool.call(ctx, mapper.readTree("{\"skus\":[\"VT01\"]}"));
        assertEquals("Tank", out.get("items").get(0).get("name").asText());
        assertEquals("resolve_picker_selection", tool.name());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl bundles/mcp test -Dtest=ResolvePickerSelectionToolTest`
Expected: FAIL — class not found.

- [ ] **Step 3: Write minimal implementation**

```java
package com.adobe.cq.commerce.mcp.internal.tools;

import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.mcp.*;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.osgi.service.component.annotations.Component;

@Component(service = McpTool.class)
public class ResolvePickerSelectionTool implements McpTool {
    private final ObjectMapper mapper = new ObjectMapper();

    public String name() { return "resolve_picker_selection"; }
    public String description() { return "Resolve display data (name) for selected SKUs, for authoring pickers."; }
    public ObjectNode inputSchema() {
        ObjectNode s = mapper.createObjectNode().put("type", "object");
        ObjectNode skus = s.putObject("properties").putObject("skus");
        skus.put("type", "array"); skus.putObject("items").put("type", "string");
        return s;
    }

    protected ProductInterface fetch(StoreContext ctx, String sku) {
        McpProductRetriever r = new McpProductRetriever(ctx.getClient());
        r.setIdentifier(sku);
        return r.fetchProduct();
    }

    public JsonNode call(McpCallContext c, JsonNode args) {
        StoreContext ctx = (StoreContext) c;
        ObjectNode out = mapper.createObjectNode();
        ArrayNode items = out.putArray("items");
        for (JsonNode skuNode : args.path("skus")) {
            ProductInterface p = fetch(ctx, skuNode.asText());
            ObjectNode n = items.addObject();
            n.put("sku", skuNode.asText());
            n.put("name", p == null ? null : p.getName());
        }
        return out;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl bundles/mcp test -Dtest=ResolvePickerSelectionToolTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/ResolvePickerSelectionTool.java \
        bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/ResolvePickerSelectionToolTest.java
git commit -m "feat(mcp): add resolve_picker_selection author tool"
```

---

## Task 15: `ConfigureProductComponentTool` (author write tool)

**Files:**
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/ConfigureProductComponentTool.java`
- Test: `bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/ConfigureProductComponentToolTest.java`

**Interfaces:**
- `ConfigureProductComponentTool` (`McpTool` `configure_product_component`, `writesContent()==true`); args `{path,sku}`; writes `selection`/`selectionType` via caller `ResourceResolver` + `ModifiableValueMap` + `commit()`; result `{path,sku,updated:true}`; fails closed on missing/non-modifiable/non-`/content` path.

- [ ] **Step 1: Write the failing test**

```java
package com.adobe.cq.commerce.mcp.internal.tools;

import com.adobe.cq.commerce.mcp.internal.StoreContext;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import com.fasterxml.jackson.databind.*;
import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ConfigureProductComponentToolTest {
    @Rule public final AemContext context = new AemContext();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test public void setsSkuOnComponent() throws Exception {
        context.build().resource("/content/site/jcr:content/root/product",
            "sling:resourceType", "core/cif/components/commerce/product/v1/product").commit();

        StoreContext ctx = mock(StoreContext.class);
        SlingHttpServletRequest req = mock(SlingHttpServletRequest.class);
        when(req.getResourceResolver()).thenReturn(context.resourceResolver());
        when(ctx.getRequest()).thenReturn(req);

        ConfigureProductComponentTool tool = new ConfigureProductComponentTool();
        JsonNode out = tool.call(ctx, mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/product\",\"sku\":\"VT01\"}"));

        assertTrue(out.get("updated").asBoolean());
        Resource r = context.resourceResolver().getResource("/content/site/jcr:content/root/product");
        assertEquals("VT01", r.getValueMap().get("selection", String.class));
        assertTrue(tool.writesContent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingResource() throws Exception {
        StoreContext ctx = mock(StoreContext.class);
        SlingHttpServletRequest req = mock(SlingHttpServletRequest.class);
        when(req.getResourceResolver()).thenReturn(context.resourceResolver());
        when(ctx.getRequest()).thenReturn(req);
        new ConfigureProductComponentTool().call(ctx, mapper.readTree(
            "{\"path\":\"/content/does/not/exist\",\"sku\":\"X\"}"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl bundles/mcp test -Dtest=ConfigureProductComponentToolTest`
Expected: FAIL — class not found.

- [ ] **Step 3: Write minimal implementation**

```java
package com.adobe.cq.commerce.mcp.internal.tools;

import com.adobe.cq.commerce.mcp.*;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.sling.api.resource.*;
import org.osgi.service.component.annotations.Component;

@Component(service = McpTool.class)
public class ConfigureProductComponentTool implements McpTool {
    private final ObjectMapper mapper = new ObjectMapper();

    public String name() { return "configure_product_component"; }
    public String description() { return "Bind a product SKU to a CIF product component resource."; }
    public boolean writesContent() { return true; }
    public ObjectNode inputSchema() {
        ObjectNode s = mapper.createObjectNode().put("type", "object");
        ObjectNode props = s.putObject("properties");
        props.putObject("path").put("type", "string");
        props.putObject("sku").put("type", "string");
        s.putArray("required").add("path").add("sku");
        return s;
    }

    public JsonNode call(McpCallContext c, JsonNode args) throws Exception {
        StoreContext ctx = (StoreContext) c;
        String path = args.path("path").asText(null);
        String sku = args.path("sku").asText(null);
        if (path == null || sku == null || !path.startsWith("/content/")) {
            throw new IllegalArgumentException("path (under /content) and sku are required");
        }
        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        Resource target = resolver.getResource(path);
        if (target == null) throw new IllegalArgumentException("resource not found: " + path);
        ModifiableValueMap vm = target.adaptTo(ModifiableValueMap.class);
        if (vm == null) throw new IllegalArgumentException("resource not modifiable: " + path);
        vm.put("selection", sku);
        vm.put("selectionType", "combinedSku");
        resolver.commit();

        ObjectNode out = mapper.createObjectNode();
        out.put("path", path); out.put("sku", sku); out.put("updated", true);
        return out;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl bundles/mcp test -Dtest=ConfigureProductComponentToolTest`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/ConfigureProductComponentTool.java \
        bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/ConfigureProductComponentToolTest.java
git commit -m "feat(mcp): add configure_product_component write tool"
```

---

## Task 16: `ConfigureCatalogPageTool` (author write tool)

**Files:**
- Create: `bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/ConfigureCatalogPageTool.java`
- Test: `bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/ConfigureCatalogPageToolTest.java`

**Interfaces:**
- `ConfigureCatalogPageTool` (`McpTool` `configure_catalog_page`, `writesContent()==true`); args `{path,categoryUid}`; sets `categoryId` on the page `jcr:content`; result `{path,categoryUid,updated:true}`; fails closed on missing page / non-`/content`.

- [ ] **Step 1: Write the failing test**

```java
package com.adobe.cq.commerce.mcp.internal.tools;

import com.adobe.cq.commerce.mcp.internal.StoreContext;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import com.fasterxml.jackson.databind.*;
import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ConfigureCatalogPageToolTest {
    @Rule public final AemContext context = new AemContext();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test public void setsCategoryBinding() throws Exception {
        context.build().resource("/content/site/plp/jcr:content",
            "sling:resourceType", "core/cif/components/structure/catalogpage/v3/catalogpage").commit();

        StoreContext ctx = mock(StoreContext.class);
        SlingHttpServletRequest req = mock(SlingHttpServletRequest.class);
        when(req.getResourceResolver()).thenReturn(context.resourceResolver());
        when(ctx.getRequest()).thenReturn(req);

        JsonNode out = new ConfigureCatalogPageTool().call(ctx, mapper.readTree(
            "{\"path\":\"/content/site/plp\",\"categoryUid\":\"MT==\"}"));
        assertTrue(out.get("updated").asBoolean());
        Resource r = context.resourceResolver().getResource("/content/site/plp/jcr:content");
        assertEquals("MT==", r.getValueMap().get("categoryId", String.class));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl bundles/mcp test -Dtest=ConfigureCatalogPageToolTest`
Expected: FAIL — class not found.

- [ ] **Step 3: Write minimal implementation**

```java
package com.adobe.cq.commerce.mcp.internal.tools;

import com.adobe.cq.commerce.mcp.*;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.sling.api.resource.*;
import org.osgi.service.component.annotations.Component;

@Component(service = McpTool.class)
public class ConfigureCatalogPageTool implements McpTool {
    private final ObjectMapper mapper = new ObjectMapper();

    public String name() { return "configure_catalog_page"; }
    public String description() { return "Bind a category UID to a CIF catalog (PLP) page."; }
    public boolean writesContent() { return true; }
    public ObjectNode inputSchema() {
        ObjectNode s = mapper.createObjectNode().put("type", "object");
        ObjectNode props = s.putObject("properties");
        props.putObject("path").put("type", "string");
        props.putObject("categoryUid").put("type", "string");
        s.putArray("required").add("path").add("categoryUid");
        return s;
    }

    public JsonNode call(McpCallContext c, JsonNode args) throws Exception {
        StoreContext ctx = (StoreContext) c;
        String path = args.path("path").asText(null);
        String uid = args.path("categoryUid").asText(null);
        if (path == null || uid == null || !path.startsWith("/content/")) {
            throw new IllegalArgumentException("path (under /content) and categoryUid are required");
        }
        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        Resource content = resolver.getResource(path + "/jcr:content");
        if (content == null) throw new IllegalArgumentException("page not found: " + path);
        ModifiableValueMap vm = content.adaptTo(ModifiableValueMap.class);
        if (vm == null) throw new IllegalArgumentException("page not modifiable: " + path);
        vm.put("categoryId", uid);
        resolver.commit();

        ObjectNode out = mapper.createObjectNode();
        out.put("path", path); out.put("categoryUid", uid); out.put("updated", true);
        return out;
    }
}
```

Note: confirm the PLP category-binding property name used by the CIF catalog page component (`categoryId` vs `magentoCategoryId`) in `bundles/core`; adjust the key to match.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl bundles/mcp test -Dtest=ConfigureCatalogPageToolTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/tools/ConfigureCatalogPageTool.java \
        bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/tools/ConfigureCatalogPageToolTest.java
git commit -m "feat(mcp): add configure_catalog_page write tool"
```

---

## Task 17: Author-only authoring servlet + package embedding

**Files:**
- Create: `ui.config/src/main/content/jcr_root/apps/core/cif/config.author/com.adobe.cq.commerce.mcp.internal.servlets.AuthoringMcpServlet.cfg.json` (`{}`)
- Modify: `bundles/mcp/.../servlets/AuthoringMcpServlet.java` (`configurationPolicy = ConfigurationPolicy.REQUIRE`)
- Modify: `all/pom.xml` (embed `core-cif-components-mcp`)
- Test: `bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/servlets/AuthoringPolicyTest.java`

**Interfaces:**
- Authoring servlet activates only where the `config.author` OSGi config exists → absent on publish.

- [ ] **Step 1: Write the failing test**

```java
package com.adobe.cq.commerce.mcp.internal.servlets;

import org.junit.Test;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import static org.junit.Assert.*;

public class AuthoringPolicyTest {
    @Test public void authoringServletRequiresConfig() {
        Component c = AuthoringMcpServlet.class.getAnnotation(Component.class);
        assertNotNull(c);
        assertEquals(ConfigurationPolicy.REQUIRE, c.configurationPolicy());
    }
    @Test public void shopperServletDoesNotRequireConfig() {
        Component c = ShopperMcpServlet.class.getAnnotation(Component.class);
        assertEquals(ConfigurationPolicy.OPTIONAL, c.configurationPolicy());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl bundles/mcp test -Dtest=AuthoringPolicyTest`
Expected: FAIL — `configurationPolicy` still default on `AuthoringMcpServlet`.

- [ ] **Step 3: Add `configurationPolicy = REQUIRE` to `AuthoringMcpServlet`**

Update its `@Component` to include `configurationPolicy = ConfigurationPolicy.REQUIRE` (add the `import org.osgi.service.component.annotations.ConfigurationPolicy;`).

- [ ] **Step 4: Create the author-only OSGi config**

`ui.config/.../apps/core/cif/config.author/com.adobe.cq.commerce.mcp.internal.servlets.AuthoringMcpServlet.cfg.json`:

```json
{}
```

- [ ] **Step 5: Embed the bundle in the `all` package**

In `all/pom.xml`, add `core-cif-components-mcp` next to `core-cif-components-core` in both the `<dependencies>` and the filevault embedded-bundle list (mirror exactly how `core-cif-components-core` is embedded, same install path).

- [ ] **Step 6: Run test + full module build**

Run: `mvn -q -pl bundles/mcp test -Dtest=AuthoringPolicyTest` → PASS.
Run: `mvn -q -pl bundles/mcp -am clean install` → BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add bundles/mcp/src/main/java/com/adobe/cq/commerce/mcp/internal/servlets/AuthoringMcpServlet.java \
        bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/servlets/AuthoringPolicyTest.java \
        ui.config/src/main/content/jcr_root/apps/core/cif/config.author/*.cfg.json \
        all/pom.xml
git commit -m "feat(mcp): register authoring servlet author-only and embed bundle in all package"
```

---

## Task 18: End-to-end selector-visibility wiring test

**Files:**
- Test: `bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/EndToEndTest.java`

- [ ] **Step 1: Write the test**

```java
package com.adobe.cq.commerce.mcp.internal;

import com.adobe.cq.commerce.mcp.JsonRpc;
import com.adobe.cq.commerce.mcp.internal.tools.*;
import com.fasterxml.jackson.databind.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class EndToEndTest {
    private final ObjectMapper mapper = new ObjectMapper();

    private JsonRpc.Request listReq() {
        try { return JsonRpc.parse(mapper, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}"); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test public void writeToolHiddenFromShopperSelector() {
        ToolRegistry reg = new ToolRegistry();
        reg.bindTool(new SearchProductsTool());
        reg.bindTool(new ConfigureProductComponentTool());
        JsonRpcDispatcher d = new JsonRpcDispatcher(mapper, reg);

        JsonNode shopperTools = d.dispatch("mcp", null, listReq()).get("result").get("tools");
        boolean hasWrite = false;
        for (JsonNode t : shopperTools)
            if ("configure_product_component".equals(t.get("name").asText())) hasWrite = true;
        assertFalse(hasWrite);

        JsonNode authorTools = d.dispatch("mcp-authoring", null, listReq()).get("result").get("tools");
        assertEquals(2, authorTools.size());
    }
}
```

- [ ] **Step 2: Run the test**

Run: `mvn -q -pl bundles/mcp test -Dtest=EndToEndTest`
Expected: PASS.

- [ ] **Step 3: Full module build (all tests + formatting + macker)**

Run: `mvn -q -pl bundles/mcp -am clean install`
Expected: BUILD SUCCESS, all tests green, no macker/formatter violations.

- [ ] **Step 4: Commit**

```bash
git add bundles/mcp/src/test/java/com/adobe/cq/commerce/mcp/internal/EndToEndTest.java
git commit -m "test(mcp): end-to-end selector visibility wiring"
```

---

## Task 19: Manual smoke test against the running instance (4502)

**Files:** none (verification only).

- [ ] **Step 1: Deploy the bundle**

Run: `mvn -q -pl bundles/mcp -am clean install -PautoInstallBundle` (confirm the profile name in `bundles/core/pom.xml`; default AEM is `http://localhost:4502`, `admin:admin`).

- [ ] **Step 2: `initialize`**

```bash
curl -s -u admin:admin -X POST -H "Content-Type: application/json" \
  --data '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}' \
  http://localhost:4502/content/venia/us/en.mcp.json
```
Expected: `result.protocolVersion == "2025-06-18"`, `capabilities.tools` present.

- [ ] **Step 3: `tools/list`**

```bash
curl -s -u admin:admin -X POST -H "Content-Type: application/json" \
  --data '{"jsonrpc":"2.0","id":2,"method":"tools/list"}' \
  http://localhost:4502/content/venia/us/en.mcp.json
```
Expected: read tools (`search_products`, `get_product`, `browse_categories`, `get_attributes`, `resolve_picker_selection`).

- [ ] **Step 4: `tools/call search_products`**

```bash
curl -s -u admin:admin -X POST -H "Content-Type: application/json" \
  --data '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"search_products","arguments":{"query":"top","pageSize":5}}}' \
  http://localhost:4502/content/venia/us/en.mcp.json
```
Expected: `result.structuredContent.items[]` with real Venia SKUs.

- [ ] **Step 5: Nav-root gate (expect 404 on a sub-page)**

```bash
curl -s -o /dev/null -w "%{http_code}\n" -u admin:admin -X POST -H "Content-Type: application/json" \
  --data '{"jsonrpc":"2.0","id":4,"method":"tools/list"}' \
  http://localhost:4502/content/venia/us/en/products.mcp.json
```
Expected: `404`.

- [ ] **Step 6: Authoring selector on author**

```bash
curl -s -u admin:admin -X POST -H "Content-Type: application/json" \
  --data '{"jsonrpc":"2.0","id":5,"method":"tools/list"}' \
  http://localhost:4502/content/venia/us/en.mcp-authoring.json
```
Expected: read tools **plus** `configure_product_component`, `configure_catalog_page`.

- [ ] **Step 7: Record results**

Note any deviations (property names, retriever fields); fix the corresponding task's code and re-run its unit test.

---

## Self-Review

**1. Spec coverage:**
- Two servlets / shared base / `cq:Page` + selectors + nav-root gate → Tasks 6, 7, 8; author-only via config → Task 17. ✓
- Hand-rolled JSON-RPC (`initialize`/`tools/list`/`tools/call`, error codes) → Tasks 2, 5. ✓
- Tool SPI + per-selector tool sets + `writesContent` guard → Tasks 3, 4, 18. ✓
- Read kernel: `search_products`, `get_attributes` (search services) → Tasks 10, 11; `get_product`, `browse_categories` (retrievers) → Tasks 12, 13. ✓
- Store context from nav-root resource → Task 6 (optional cross-store `path` override is a documented extension point; base case implemented). ✓
- Anonymous shopper + shopper-token pass-through → Tasks 6/7 (request-scoped `MagentoGraphqlClient` carries the header). ✓
- Authoring writes under caller session + ACLs + fail-closed → Tasks 15, 16. ✓
- Compact DTO mapping → Task 9. ✓
- Packaging in `all` → Task 17 (dispatcher allow rule is ops config in spec §7, not a code task). ✓
- Tests: contract, nav-root gate, tool-set visibility, write-tool ACL, store context → Tasks 5, 6, 7, 15, 18. ✓

**2. Placeholder scan:** Retriever query bodies (Tasks 12/13) and a few property/getter names (`ProductListItem.getTitle`, `FilterAttributeMetadata` getters, PLP `categoryId`, `SorterKey`) are flagged **verify-against-core** with the exact reference file to copy from — resolved at the failing-compile step, not open design questions. All logic steps include complete code.

**3. Type consistency:** `McpTool.call(McpCallContext, JsonNode)` used consistently; tools cast `McpCallContext` → `StoreContext`; `StoreContext` exposes `getResource/getRequest/getLandingPage/getProductPage/getClient` used identically across Tasks 10–16; dispatcher result shape (`content`/`structuredContent`) matches the servlet and end-to-end tests.

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-02-cif-commerce-mcp.md`. Two execution options:

1. **Subagent-Driven (recommended)** — dispatch a fresh subagent per task, review between tasks, fast iteration.
2. **Inline Execution** — execute tasks in this session using executing-plans, batch execution with checkpoints.

Which approach?

