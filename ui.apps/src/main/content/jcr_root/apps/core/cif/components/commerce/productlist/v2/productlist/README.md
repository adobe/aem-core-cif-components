<!--
Copyright 2021 Adobe Systems Incorporated

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
Product List (v2)
====
The version 2 of the productlist component extends the v2 of productcollection
component and v1 of productlist component model by extending the v1 GraphQL 
query with the `staged` field introduced in Magento 2.4.2 Enterprise Edition (EE). 
This hence requires that the Magento backend is at least version 2.4.2 EE 
because the query with the `staged` field will be rejected by Magento versions
not having this field in the GraphQL schema.

## BEM Description

In addition to the elements documented for the version 2 of the productcollection component,
version 2 of productlist introduces these extra elements to display the details of a category 
and a "staged" flag on the category itself or its products. 
Note that this is only relevant for AEM author instances.

```
BLOCK category
    ELEMENT category__root
        MOD category__root--staged
    ELEMENT category__header
    ELEMENT category__image
    ELEMENT category__staged
    ELEMENT category__title
```

## Information
* **Vendor**: Adobe
* **Version**: v2
* **Compatibility**: AEM as a Cloud Service / AEM 6.5
* **Status**: production-ready
