# AEM CIF Core Components Library

This folder contains the projects required to build the CIF Components Library that extends the [WCM Core Components Library](https://www.aemcomponents.dev/) with the commerce components.

## Prerequisites

The CIF Components Library must be installed on top of the WCM Core Components Library, so **make sure** you first install both the latest [ui.content](https://repo1.maven.org/maven2/com/adobe/cq/core.wcm.components.examples.ui.content/2.9.0/core.wcm.components.examples.ui.content-2.9.0.zip) and [ui.apps](https://repo1.maven.org/maven2/com/adobe/cq/core.wcm.components.examples.ui.apps/2.9.0/core.wcm.components.examples.ui.apps-2.9.0.zip) content packages of the WCM Core Components Library.

You will also need the latest version of the WCM Core Components, the easiest is to install the ["all" content package](https://repo1.maven.org/maven2/com/adobe/cq/core.wcm.components.all/2.9.0/core.wcm.components.all-2.9.0.zip).

Simply download the zip files, and install these three content packages directly in AEM's CRX Package Manager.

## Installation

There are three sub-projects for the CIF Core Components Library:
* **bundle**: this contains a mock GraphQL server that will serve mock responses to all the example components.
* **ui.apps**: this contains some application content for the library, including the OSGi configuration of the services required by the examples.
* **ui.content**: this contains the example pages demonstrating the use of the CIF Components.

You can install all 3 artifacts by running `mvn clean install -PautoInstallPackage`

_Note that the `ui.apps` examples content package depends on the same version of the `ui.apps` content package of the CIF components. This means that a developer working on the SNAPSHOT version of the library must ensure that the same SNAPSHOT version of the components `ui.apps` library is installed on AEM._

## Required configuration

The mock GraphQL server can only serve content via HTTPS because our GraphQL client does not support non-secure connections for security reasons. This means you have to enable HTTPS on your AEM instance if you want to install and use the mock server. To do this, simply follow the [following documentation](https://docs.adobe.com/content/help/en/experience-manager-65/administering/security/ssl-by-default.html).
If the self-signed certificate gets rejected by the browser, try adding it to the OS keychain and mark it as trusted.

You must also enable anonymous access to the mock GraphQL server which will by defaut receive its requests on `https://localhost:8443/apps/cif-components-examples/graphql`. To do that, do the following:
* In the AEM system configuration console, look for `Apache Sling Authentication Service`
* Add the following line at the bottom of the "Authentication Requirements" property: `-/apps/cif-components-examples/graphql`

## How does it work?

When everything is installed, you should find the `com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl~examples` and `com.adobe.cq.commerce.graphql.magento.GraphqlDataServiceImpl~examples` configurations in the AEM configuration console. These configure the GraphQL client and data service to point to the mock GraphQL server.

The components are configured to use the CIF configuration defined at `/conf/core-components-examples/settings/cloudconfigs/commerce`. This is configured via the usual `cq:conf` property defined at `/content/core-components-examples/library/commerce/jcr:content`.

All the CIF components used on the CIF library pages issue GraphQL requests to the mock GraphQL server which responds with mocked JSON responses that contain the data and links to images to be rendered by the component. We use a mock GraphQL server to avoid having any dependency on a pre-installed Magento instance with sample data.

When everything is correctly installed, you should be able to open the library page at [http://localhost:4502/content/core-components-examples/library.html](http://localhost:4502/content/core-components-examples/library.html) and see the "Commerce" at the bottom of the left-side panel and at the bottom of the page content.

## Layout / design

**Note**: This is only useful for the developers of this components library.

The layout/design of the examples is currently "borrowed" from the [Venia theme](https://github.com/adobe/aem-cif-project-archetype/tree/master/src/main/archetype/ui.apps/src/main/content/jcr_root/apps/__appsFolderName__/clientlibs/theme) available in the CIF archetype. To avoid having any project dependency on the venia sample data, we generate the [venia.css](ui.apps/src/main/content/jcr_root/apps/cif-components-examples/clientlibs/venia-theme/venia.css) file "offline", based on the css files of the archetype sample data. This is done in 3 steps:

The `css.txt` file of the Venia theme is converted into a (css) `less` master file:

`sed "s/^#.*//;/^$/d;s/^.*$/@import (less) \"&\";/" css.txt`

The output of that command is copied into the placeholder in [venia.less.template](ui.apps/src/main/content/jcr_root/apps/cif-components-examples/clientlibs/venia-theme/venia.less.template) that you can save into a file called `venia.less`.

With the less compiler (install it with `npm install -g less`), execute the following command:

`lessc --verbose --math=strict venia.less venia.css`

This generates the `venia.css` file that we use for the layout/design of the examples.
