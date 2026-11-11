# Integration tests

This folder contains integration tests that test all the components based on the sample content of the [CIF components library](../../examples). To execute the tests with a local AEM instance, simply setup and install the CIF components library. Make sure you also install the GraphQL client version >= `1.6.1` so you no longer have to setup HTTPS.

To execute the tests, simply run `mvn clean verify -Ptest-all`

You can also execute the tests in your favorite IDE.

## Catalog backend failures (SITES-54845)

With the core bundle and examples installed on a local AEM instance, run:

```bash
mvn -f it/http/pom.xml verify -Ptest-all -Dit.test=CatalogPageBackendErrorIT,SpecificCatalogPageNotFoundIT
```

`CatalogPageBackendErrorIT` temporarily redirects the **examples** GraphQL client to an unreachable loopback endpoint and restores its full configuration in a `finally` block. Use a disposable/local test instance and do not run configuration-mutating tests concurrently. The default filter configuration (`serviceUnavailableOnBackendError=true`) must be active. It checks product/category HTML and `.model.json` 503 responses, genuine missing-product 404, and author edit/preview 200 responses. The mode checks verify that upstream AEM processing initializes `WCMMode` before the catalog filter (service ranking `-6000`). `SpecificCatalogPageNotFoundIT` retains the missing-product/category 404 checks for specific-page forwards.

The examples mock endpoint accepts `page=999` but does not reproduce Commerce's out-of-range GraphQL error. The HTTP test checks its 200 status; transport-only classification is covered by the core unit tests. Verify the actual Commerce error and dispatcher/CDN behavior manually on a test environment:

1. With a healthy Commerce endpoint and disabled WCM mode, request an existing product and category: expect 200. Request a genuinely missing SKU/category: expect 404. Request an existing category with `?wcmmode=disabled&page=999`: verify Commerce returns a GraphQL-only pagination error and AEM still returns 200.
2. Save the GraphQL client configuration, point it at an unreachable endpoint, and use uncached identifiers/queries (or clear the test client's caches). Request product/category HTML and `.model.json` with `wcmmode=disabled`: expect 503 and no backend details in the response. Confirm the filter logs one WARN with page path/error message and no stack trace. Restore the endpoint afterwards.
3. During the outage, request the same pages with `wcmmode=edit` and `wcmmode=preview` on author: expect the previous placeholder/empty-result behavior, not 503. On each supported AEM runtime, inspect the Sling request filter chain to confirm WCM mode initialization precedes ranking `-6000`.
4. Configure the filter PID `com.adobe.cq.commerce.core.components.internal.servlets.CatalogPageNotFoundFilter`: with a positive `retryAfterSeconds`, expect `Retry-After` on 503 only; with zero/negative values, expect no header. Set `serviceUnavailableOnBackendError=false` and verify the previous 404/empty-200 behavior is restored.
5. Restore all configurations. Through dispatcher/CDN, verify the custom 503 page preserves the status, error responses do not replace healthy cached content, stale-if-error serves eligible stale content, and availability monitors detect the outage. Align or remove Venia's earlier `CatalogPageErrorFilter` before validating the GraphQL-only case in Venia.
