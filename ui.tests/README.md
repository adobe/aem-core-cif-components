
# UI tests

This folder contains UI tests that test some client-side (= clientlib) features of the CIF components. The tests are based on the sample content of the [CIF components library](../examples). To execute the tests with a local AEM instance, simply setup and install the CIF components library. Make sure you also install the GraphQL client version >= `1.6.1` so you no longer have to setup HTTPS.

To execute the tests, simply run

```
mvn verify -Pui-tests-local-execution
```

## Notes
* We shouldn't update the @wdio/mocha-framework as @wdio/sync will not be compactible. Use 7.4.6 version only.


## Requirements

* Maven
* Chrome and/or Firefox browser installed locally in default location
* An AEM author instance running at http://localhost:4502


#### Remarks
* After execution, reports and logs are available in `test-module/reports` folder
* If you receive an error message like:
    ```
    This version of ChromeDriver only supports Chrome version XX.
    ```

    Try setting the `CHROMEDRIVER` environment variable to a version that matches your currently installed Chrome version. You can find matching versions at https://chromedriver.chromium.org/downloads.

    Example:

    ```bash
    CHROMEDRIVER=87.0.4280.20 mvn verify -Pui-tests-local-execution
    ```

### Parameters

| Parameter | Required | Default| Description |
| --- | --- | --- | --- |
| `AEM_AUTHOR_URL`        | false     | `http://localhost:4502` | URL of the author instance |
| `AEM_AUTHOR_USERNAME`   | false     | `admin`                 | Username used to access the author instance |
| `AEM_AUTHOR_PASSWORD`   | false     | `admin`                 | Password used to access the author instance |
| `AEM_PUBLISH_URL`       | false     | -                       | URL of the publish instance |
| `AEM_PUBLISH_USERNAME`  | false     | `admin`                 | Username used to access the publish instance |
| `AEM_PUBLISH_PASSWORD`  | false     | `admin`                 | Password used to access the publish instance |
| `SELENIUM_BROWSER`      | false     | `chrome`                | Browser used in the tests (`chrome` **_or_** `firefox`) |
| `HEADLESS_BROWSER`      | false     | `false`                 | Set [headless mode](https://en.wikipedia.org/wiki/Headless_browser) of the browser |

#### Example

Run tests on <span style="color:green">local</span> <span style="color:orange">headless</span> <span style="color:purple">firefox</span>, targeting a <span style="color:blue">custom AEM author instance</span>:

<PRE>
mvn test \
    <span style="color:green">-Plocal-execution</span> \
    <span style="color:orange">-DHEADLESS_BROWSER=true</span> \
    <span style="color:purple">-DSELENIUM_BROWSER=firefox</span> \
    <span style="color:blue">-DAEM_AUTHOR_URL=http://my-aem-author-instance.com</span> \
    <span style="color:blue">-DAEM_AUTHOR_USERNAME=testuser</span> \
    <span style="color:blue">-DAEM_AUTHOR_PASSWORD=aVVe5om3</span>
</PRE>

