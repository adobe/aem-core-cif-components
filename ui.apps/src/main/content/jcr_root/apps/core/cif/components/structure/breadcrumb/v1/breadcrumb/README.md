<!--
Copyright 2019 Adobe Systems Incorporated

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Commerce Breadcrumb (v1)

The Commerce Breadcrumb component is a server-side component written in HTL, rendering a combined AEM page and commerce catalog breadcrumb.
The commerce elements of the breadcrumb (i.e. categories and product) are retrieved from Magento via GraphQL. The main usage of this component would be as part of all page templates which include a breadcrumb.

This component is based on the [Breadcrumb component](https://github.com/adobe/aem-core-wcm-components/tree/master/content/src/content/jcr_root/apps/core/wcm/components/breadcrumb/v2/breadcrumb) of the AEM Core Components library.

## Features

- Combines AEM page and commerce elements in the breadcrumb
- Optionally displays/hides the catalog page (like in the Commerce Navigation component)

### Use Object

The component uses the default HTL and Use-object of the Breadcrumb component of the AEM Core Components library.

### Properties

This component has the same properties defined by the [Breadcrumb component](https://github.com/adobe/aem-core-wcm-components/tree/master/content/src/content/jcr_root/apps/core/wcm/components/breadcrumb/v2/breadcrumb) of the AEM Core Components library.

In addition and similar to the Commerce Navigation component, it defines and uses the `./structureDepth` property to control the maximum depth of categories that should be displayed in the breadcrumb.

Like the Commerce Navigation component, it also indirectly references the `./showMainCategories` property of the [catalog page](/ui.apps/src/main/content/jcr_root/apps/core/cif/components/structure/catalogpage/v1/catalogpage) to control whether the catalog page is displayed or not in the breadcrumb.

## BEM Description

Check the BEM description of the [Breadcrumb component](https://github.com/adobe/aem-core-wcm-components/tree/master/content/src/content/jcr_root/apps/core/wcm/components/breadcrumb/v2/breadcrumb) of the AEM Core Components library.

## Information

- **Vendor**: Adobe
- **Version**: v1
- **Compatibility**: AEM 6.4 / 6.5
- **Status**: production-ready
