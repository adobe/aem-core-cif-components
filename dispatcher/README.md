# AEM Dispatcher with Magento Proxy

## Installation via Docker

1. Update Magento configuration
    * Open `conf/magento-proxy.conf`.
    * Update the `magento_host` variable to point to your Magento instance.
2. Update AEM configuration
    * Open `conf/dispatcher.any`.
    * Scroll to the `/farms/publish/renders/rend01` area.
    * Update hostname and port values to point to your AEM instance.
3. Build Docker Image
    ```bash
    docker build -t aem-dispatcher .
    ```
4. Run Docker Image
    ```bash
    docker run -it -p 80:80 aem-dispatcher:latest
    ```

## Installation in Dispatcher
1. Update Magento configuration
    * Open `conf/magento-proxy.conf`.
    * Update the `magento_host` variable to point to your Magento instance.
2. Include the `conf/magento-proxy.conf` file in your Apache configuration. For example, in `httpd.conf` add:
    ```
    Include conf/magento-proxy.conf
    ```

## Usage
The dispatcher is configured to proxy Magento's GraphQL and REST endpoints.

In the dispatcher these endpoints are mapped as following:

| Request               | Proxy to          |
| --------------------- | ----------------- |
| `{APACHE}/magento/graphql` | `{MAGENTO}/graphql` |
| `{APACHE}/magento/rest/*`  | `{MAGENTO}/rest/*`  |
