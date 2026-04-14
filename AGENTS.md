# AEM Core CIF Components

Adobe Commerce integration components for AEM (Sling Models, HTL templates, React storefronts).

## Build

- JDK 11 (CI uses `cimg/openjdk:11.0.16`; compiles to Java 8 bytecode)
- Node 12.14.1 (managed by `frontend-maven-plugin` for React modules)
- `mvn clean install` -- full build with integration test modules
- `mvn clean install -Dskip-it` -- skip integration test modules
- `mvn -Pformat-code clean compile` -- auto-format Java code and sort imports

## Testing

- **Unit tests** (JUnit 4, Mockito, io.wcm AEM Mocks): `mvn test` -- 75% branch coverage enforced via JaCoCo
- **HTTP integration tests** (JUnit 4, CQ Testing Clients): `mvn verify` -- requires AEM instance at `localhost:4502`
- **UI tests** (WebdriverIO, Mocha, Selenium): `mvn verify -Pui-tests-local-execution` -- requires AEM instance and Chrome/Firefox
- **React unit tests** (Jest): `cd react-components && npm test`
- Skip integration tests with `-Dskip-it`

## Code Style

- **Java formatting**: Eclipse formatter (`eclipse-formatter.xml`) via `formatter-maven-plugin`; imports sorted by `impsort-maven-plugin` (groups: `java`, `javax`, `org`)
- **Java auto-fix**: `mvn -Pformat-code clean compile`
- **JS/CSS formatting**: Prettier (120 char width, 4-space indent, single quotes, semicolons) + ESLint
- **JS auto-fix**: `cd react-components && npm run lint:fix && npm run prettier:fix`
- **License headers**: Apache 2.0 required on all source files; enforced by Apache RAT (Java) and `eslint-plugin-header` (JS)
- **Architecture rules**: Macker plugin enforces package dependency constraints

## Module Map

| Module | Path | Description |
|--------|------|-------------|
| parent | `parent/` | Parent POM with shared plugin/dependency config |
| core | `bundles/core/` | Core OSGi bundle: Sling Models, servlets, GraphQL retrievers |
| ui.apps | `ui.apps/` | AEM content package: CIF component definitions and HTL templates |
| ui.config | `ui.config/` | AEM OSGi configuration content package |
| all | `all/` | Aggregator package embedding core bundle + subpackages |
| react-components | `react-components/` | Reusable React/Apollo storefront components (NPM package) |
| examples | `examples/` | Demo implementation with mock GraphQL server |
| product-recs | `extensions/product-recs/` | Product recommendations extension (bundle + React + content) |
| ep-connector | `extensions/experience-platform-connector/` | Experience Platform connector (NPM) |
| it/http | `it/http/` | HTTP integration tests (Failsafe) |
| it/content | `it/content/` | Test content package for integration tests |
| ui.tests | `ui.tests/` | Selenium UI tests |

## Architecture

- **DI**: OSGi Declarative Services (`@Component`, `@Reference`) -- no Spring
- **Sling Models**: Versioned implementations (v1/v2/v3 in `bundles/core`) with `@Model` adapting `SlingHttpServletRequest`; later versions extend earlier ones for backward compatibility
- **GraphQL**: Type-safe Java query builders from `magento-graphql` library; Retriever classes (e.g. `ProductRetriever`) build and execute queries via `MagentoGraphqlClient`; React side uses Apollo Client with queries in `react-components/src/queries/`
- **Rendering**: Server-side HTL templates (`ui.apps/.../commerce/`) bind to Sling Models via `data-sly-use`; client-side React components hydrate for interactivity
- **Key packages in `bundles/core`**: `models/v{1,2,3}/` (Sling Models), `models/retriever/` (GraphQL fetchers), `internal/servlets/` (redirects, data sources), `internal/services/` (URL providers, sitemap), `internal/datalayer/` (Adobe Data Layer)
- **External deps**: `aem-core-wcm-components` 2.29.0, `graphql-client` 1.10.0, `magento-graphql` 9.1.0, `@apollo/client`, `@magento/peregrine`
