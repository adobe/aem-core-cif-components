# CIF IT Site (`it/site`)

Integration-test site for **AEM CIF Core Components**, living inside the `aem-core-cif-components` monorepo under `it/site/`. It is a **FileVault-only** layout (no local OSGi `core` bundle, no `ui.frontend` build, no `it.tests` / `ui.tests`). See **`generate.md`** for how this site was originally created and how to recreate it for a different project.

## Modules

* **ui.apps** — `/apps` code: components, clientlibs, HTL, etc.
* **ui.apps.structure** — repository root filters for package validation (declared in `pom.xml`, no `src/main/content`).
* **ui.content** — mutable content, templates under `/conf`, sample pages.
* **ui.config** — OSGi configurations (cloud). GraphQL URL is set at runtime (see [GraphQL endpoint](#graphql-endpoint)).
* **all** — container package embedding the site's `ui.apps`, `ui.content`, and `ui.config`.
* **classic/all** (reactor profile **`classic`**) — 6.5 / AMS mixed package embedding site + classic overlays + CIF Core + WCM Core.

## Local environment setup

This module does **not** embed CIF Core artifacts. The following three things must be installed on your local AEM SDK in order, before the site works correctly.

### Step 1 — Install CIF Core Components

Build and install the main repo `all` package from the **monorepo root**. This installs the `core-cif-components-core` OSGi bundle, `core-cif-components-apps`, and `core-cif-components-config`:

```bash
# from the monorepo root
mvn clean install -PautoInstallSinglePackage -Dskip-it
```

### Step 2 — Install `magento-graphql` bundle

`core-cif-components-core` imports `com.adobe.cq.commerce.magento.graphql` from a standalone JAR that is not embedded in any package. Without it the core bundle stays in `Installed` state and no CIF React components (navigation, cart, etc.) will render.

After step 1, the JAR is in your local Maven repository. Install it via the Felix console (`/system/console/bundles` → **Install/Update**):

```
~/.m2/repository/com/adobe/commerce/cif/magento-graphql/9.1.0-magento242ee/magento-graphql-9.1.0-magento242ee.jar
```

Or with curl:

```bash
curl -u admin:admin \
  -F action=install \
  -F bundlestartlevel=20 \
  -F bundlefile=@~/.m2/repository/com/adobe/commerce/cif/magento-graphql/9.1.0-magento242ee/magento-graphql-9.1.0-magento242ee.jar \
  http://localhost:4502/system/console/bundles
```

Confirm `com.adobe.commerce.cif.core-cif-components-core` shows **Active** at `/system/console/bundles` before continuing.

### Step 3 — Install the IT Site

```bash
# from it/site/
mvn clean install -PautoInstallSinglePackage
```

This installs `ui.apps`, `ui.content`, and `ui.config` for the test site via the `all` container package.

After all three steps, open `http://localhost:4502/content/cif-components-it-site/us/en.html` to verify the site loads with navigation and header.

## Build

### From `it/site/` directly

```bash
# Cloud only
mvn clean install

# Cloud + classic (AEM 6.5 / AMS)
mvn clean install -Pclassic
```

### From the monorepo root

```bash
# Build only it/site and its upstream dependencies
mvn clean install -pl it/site -am

# Skip it/site and all integration-test modules
mvn clean install -Dskip-it
```

## Testing

HTTP integration tests for commerce components live in `../http` (`it/http`). Run them against a running AEM author:

```bash
# from the monorepo root
mvn clean verify -pl it/http -am -Dit
```

See `it/http/README.md` for override properties (`aem.host`, `aem.port`, `it.commerce.library.path`).

## GraphQL endpoint

`GraphqlClientImpl~default` drives IT site commerce pages (`ui.config` / `classic/ui.config`). The package does **not** ship a Catalog Service URL — only OSGi interpolation and **`httpMethod: POST`**:

```json
"httpMethod": "POST",
"url": "$[env:COMMERCE_ENDPOINT;default=]"
```

Use the full GraphQL URL (`https://…/graphql`). Do not use `${COMMERCE_ENDPOINT}` in `.cfg.json`; that syntax is for Cloud Manager deploy-time substitution, not AEM OSGi.

### CircleCI / local Quickstart

After AEM is ready, `.circleci/ci/it-tests.js` applies the URL the same way as `GraphqlClientImpl~examples`: a Felix `configMgr` POST to `com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl~default` with `url` and `httpMethod=POST` from the CircleCI `COMMERCE_ENDPOINT` env var (full `https://…/graphql` URL).

Changing backends does not require a code change — only that pipeline variable.

### Local development

* **Cloud SDK** — `export COMMERCE_ENDPOINT=https://…/graphql` before starting AEM, or set `url` in `/system/console/configMgr` for `GraphqlClientImpl~default`.
* **AEM 6.5 / AMS** (`-Pclassic`) — `$[env:…]` is not applied on 6.5; use the CI curl step above or override the factory config in OSGi.
* **AEM as a Cloud Service** — define `COMMERCE_ENDPOINT` in Cloud Manager; the `$[env:COMMERCE_ENDPOINT;default=]` placeholder is resolved on the AEM JVM.

### HTTP integration tests

The same `COMMERCE_ENDPOINT` is passed to Maven for `CacheInvalidationIT` (Magento REST). See `it/http/README.md` for overrides.

## ClientLibs

There is no `ui.frontend` webpack/npm build step. CSS is a single static file committed directly to `ui.apps`:

```
ui.apps/src/main/content/jcr_root/apps/cif-components-it-site/clientlibs/clientlib-site/css/site.css
```

To update styles, edit that file directly and redeploy `ui.apps`.
