# CIF IT Site (`it/site`)

Integration-test site for **AEM CIF Core Components**, living inside the `aem-core-cif-components` monorepo under `it/site/`. It is a **FileVault + frontend** layout (no local OSGi `core` bundle, no `it.tests` / `ui.tests`). See **`generate.md`** for how this site was originally created and how to recreate it for a different project.

## Modules

* **ui.apps** — `/apps` code: components, clientlibs, HTL, etc.
* **ui.apps.structure** — repository root filters for package validation (declared in `pom.xml`, no `src/main/content`).
* **ui.content** — mutable content, templates under `/conf`, sample pages.
* **ui.config** — OSGi configurations (cloud). GraphQL endpoint uses `${COMMERCE_ENDPOINT}`.
* **ui.frontend** — Webpack build; output is emitted into `ui.apps` clientlibs.
* **all** — container package embedding site packages and CIF vendor packages (AEM as a Cloud Service).
* **classic/all** (reactor profile **`classic`**) — 6.5 / AMS mixed package embedding site + classic overlays + CIF Core + WCM Core.

## How to build

### From `it/site/` directly

```bash
# Cloud only
mvn clean install

# Cloud + classic (AEM 6.5 / AMS)
mvn clean install -Pclassic
```

### From the monorepo root

The `it/site` reactor is part of the `integration-tests` profile (active by default unless `-Dskip-it` is set):

```bash
# Build only it/site and its dependencies from the monorepo root
mvn clean install -pl it/site -am

# Or build all integration-test modules together
mvn clean install -Pintegration-tests
```

### Deploy to a local AEM instance

```bash
# AEM as a Cloud Service SDK (author)
mvn clean install -PautoInstallSinglePackage

# AEM as a Cloud Service SDK (publish)
mvn clean install -PautoInstallSinglePackagePublish

# AEM 6.5 / AMS — build first, then upload the classic all zip via Package Manager UI
mvn clean install -Pclassic
# Upload: classic/all/target/cif-components-it-site.all-classic-*.zip
```

> **Do not** combine `-PautoInstallSinglePackage,classic` on AEM 6.5 — it deploys the cloud `all` container before the classic overlay can correct it.

Or to deploy a single content package from its sub-module directory (e.g. `ui.apps`):

```bash
mvn clean install -PautoInstallPackage
```

## Testing

HTTP integration tests for commerce components live in `../http` (`it/http`). Run them against a running AEM author:

```bash
# from the monorepo root
mvn clean verify -pl it/http -am -Dit
```

See `it/http/README.md` for override properties (`aem.host`, `aem.port`, `it.commerce.library.path`).

## GraphQL endpoint

The cloud `ui.config` OSGi config uses `${COMMERCE_ENDPOINT}`. Set this:

* **Cloud Manager / AMS** — define the environment variable `COMMERCE_ENDPOINT` pointing to your `https://…/graphql` URL.
* **Local AEM SDK** — set it in the OSGi console or add a dev-specific config override with a concrete URL.

The classic `classic/ui.config` ships with a placeholder URL (`https://hostname.com/graphql`); replace it via OSGi or an AMS environment variable once the instance is up.

## ClientLibs

The `ui.frontend` Webpack build produces output consumed by the [`aem-clientlib-generator`](https://github.com/wcm-io-frontend/aem-clientlib-generator), which packages it as an AEM ClientLib under `ui.apps`. After cloning or first checkout:

```bash
cd ui.frontend && npm ci
```

If `webpack` is missing from PATH during a Maven build, the npm dependencies were not installed.
