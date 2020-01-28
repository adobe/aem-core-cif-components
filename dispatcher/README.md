# AEM Dispatcher with Magento Proxy

## Installation via Docker

1. Update Magento configuration
    - Open `conf/magento-proxy.conf`.
    - Update the `magento_host` variable to point to your Magento instance.
2. Update AEM configuration
    - Open `conf/dispatcher.any`.
    - Scroll to the `/farms/publish/renders/rend01` area.
    - Update hostname and port values to point to your AEM instance.
3. Build Docker Image
    ```bash
    docker build -t aem-dispatcher .
    ```
4. Run Docker Image
    ```bash
    docker run -it -p 80:80 aem-dispatcher:latest
    ```

### Configure the dispatcher to access the authoring instance

The default configuration of the dispatcher is valid for the publish instance, where certain paths are blocked (i.e. `/libs` or `/conf`). During development, it's useful to access AEM via dispatcher to detect some early misses in the code.

To access the author via the dispatcher, the following configuration options must be updated:

-   Open `conf/dispatcher.any`
-   Scroll to the `/filter` section
-   Uncomment the line which will allow access to the login page (and other URLs under `/libs`) via the dispatcher

```
/0024 { /type "allow" /url "/libs/*" }
```

## Installation in Dispatcher

1. Update Magento configuration
    - Open `conf/magento-proxy.conf`.
    - Update the `magento_host` variable to point to your Magento instance.
2. Include the `conf/magento-proxy.conf` file in your Apache configuration. For example, in `httpd.conf` add:
    ```
    Include conf/magento-proxy.conf
    ```

## Usage

The dispatcher is configured to proxy Magento's GraphQL endpoint.

In the dispatcher the endpoint is mapped as following:

| Request                    | Proxy to            |
| -------------------------- | ------------------- |
| `{APACHE}/magento/graphql` | `{MAGENTO}/graphql` |
