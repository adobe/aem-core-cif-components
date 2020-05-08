# AEM CIF Core Components Library

This folder contains the projects required to build the CIF Components Library that extends the [WCM Core Components Library](https://www.aemcomponents.dev/) with the commerce components.

## Prerequisites

The CIF Components Library must be installed on top of the WCM Core Components Library, so **make sure** you first install both the latest [ui.content](https://oss.sonatype.org/content/repositories/public/com/adobe/cq/core.wcm.components.examples.ui.content/2.8.1-SNAPSHOT-20200410121300/core.wcm.components.examples.ui.apps-2.8.1-SNAPSHOT-20200410121300.zip) and [ui.apps](https://oss.sonatype.org/content/repositories/public/com/adobe/cq/core.wcm.components.examples.ui.apps/2.8.1-SNAPSHOT-20200410121300/core.wcm.components.examples.ui.apps-2.8.1-SNAPSHOT-20200410121300.zip) content packages of the WCM Core Components Library.

You will also need the latest version of the WCM Core Components, the easiest is to install the ["all" content package](https://oss.sonatype.org/content/repositories/public/com/adobe/cq/core.wcm.components.all/2.8.1-SNAPSHOT-20200410121300/core.wcm.components.all-2.8.1-SNAPSHOT-20200410121300.zip).

Simply download the zip files, and install these three content packages directly in AEM's CRX Package Manager.

## Installation

There are three sub-projects for the CIF Core Components Library:
* **bundle**: this contains a mock GraphQL server that will serve mock responses to all the example components.
* **ui.apps**: this contains some application content for the library, including the OSGi configuration of the services required by the examples.
* **ui.content**: this contains the example pages demonstrating the use of the CIF Components.

You can install all 3 artifacts by running `mvn clean install -PautoInstallPackage`

## Required configuration

The mock GraphQL server can only serve content via HTTPS because our GraphQL client does not support non-secure connections for security reasons. This means you have to enable HTTPS on your AEM instance if you want to install and use the mock server. To do this, simply follow the [following documentation](https://docs.adobe.com/content/help/en/experience-manager-65/administering/security/ssl-by-default.html).

You must also enable anonymous access to the mock GraphQL server which will by defaut receive its requests on `https://localhost:8443/apps/cif-components-examples/graphql`. To do that, do the following:
* In the AEM system configuration console, look for `Apache Sling Authentication Service`
* Add the following line at the bottom of the "Authentication Requirements" property: `-/apps/cif-components-examples/graphql`

## How does it work?

When everything is installed, you should find the `com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl~examples` and `com.adobe.cq.commerce.graphql.magento.GraphqlDataServiceImpl~examples` configurations in the AEM configuration console. These configure the GraphQL client and data service to point to the mock GraphQL server.

The components are configured to use the CIF configuration defined at `/conf/core-components-examples/settings/cloudconfigs/commerce`. This is configured via the usual `cq:conf` property defined at `/content/core-components-examples/library/commerce/jcr:content`.

All the CIF components used on the CIF library pages issue GraphQL requests to the mock GraphQL server which responds with mocked JSON responses that contain the data and links to images to be rendered by the component. We use a mock GraphQL server to avoid having any dependency on a pre-installed Magento instance with sample data.

When everything is correctly installed, you should be able to open the library page at [http://localhost:4502/content/core-components-examples/library.html](http://localhost:4502/content/core-components-examples/library.html) and see the "Commerce" at the bottom of the left-side panel and at the bottom of the page content.