<!--
Copyright 2022 Adobe Systems Incorporated

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
Product (v3) / Technical Preview
====
The version 3 of the product component extends the v2 product component by extending the v2 GraphQL query with the `uid` field for product variants and variant attributes in Magento 2.4.4 . This hence requires that the Magento backend is at least version 2.4.4 because the query with the `uid` fields will be rejected by Magento versions not having this field in the GraphQL schema and adding bundled products to cart using these `uid` attributes was broken in previous versions of Magento

## Information
* **Vendor**: Adobe
* **Version**: v3
* **Compatibility**: AEM as a Cloud Service / AEM 6.4 / 6.5
* **Status**: technical preview
