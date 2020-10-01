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

# Related Products (v1)

The Related Products component is a server-side component written in HTL, allowing to display a list of related products in a carousel style. This component actually reuses the Product Carousel component to display the products.
The products are retrieved from Magento via GraphQL. This component can be used on any experience page.

## Features

- Display a list of related products, based on a selected product or the current product of the generic product page.
- Carousel navigation via next/previous indicators
- Style System support.

### Use Object

The Related Products component uses the `com.adobe.cq.commerce.core.components.models.productcarousel.ProductCarousel` Sling model as its Use-object. However, the implementation of the Sling Model is different.

### Component Policy Configuration Properties
The following configuration properties are used:

1. `./type` - defines the default HTML heading element type (`h1` - `h6`) this component will use for its rendering

### Edit Dialog Properties

The following properties are written to JCR for this component and are expected to be available as `Resource` properties:

1. `./product` - an optional product SKU defining the product for which we want to display the related products. If empty, the component will use the URL selector to find the product.
2. `./relationType` - a mandatory relation type, defining the relation between the product and the "related" products. In Magento, there are 3 possible types for this relation: `related_products`, `upsell_products`, and `crosssell_products`.
3. `./jcr:title` - Optional title text
4. `./titleType` - will store the HTML heading element type which will be used for rendering; if no value is defined, the component will fallback
to the `type` value defined by the component's policy. The property of the policy is called `type` so we can reuse the `core/wcm/components/commons/datasources/allowedheadingelements/v1` Servlet from the WCM components.

## BEM Description

See the Product Carousel component.

## Information

- **Vendor**: Adobe
- **Version**: v1
- **Compatibility**: AEM 6.4 / 6.5
- **Status**: production-ready
