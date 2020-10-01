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

# Product Carousel (v1)

Product Carousel component is a server-side component written in HTL, allowing to display a list of featured products in a carousel style.
The products are retrieved from Magento via GraphQL. This component can be used on any experience page.

## Features

- Display a list of featured products
- Carousel navigation via next/previous indicators
- Style System support.

### Use Object

The Product Carousel component uses the `com.adobe.cq.commerce.core.components.models.productcarousel.ProductCarousel` Sling model as its Use-object.

### Component Policy Configuration Properties
The following configuration properties are used:

1. `./type` - defines the default HTML heading element type (`h1` - `h6`) this component will use for its rendering

### Edit Dialog Properties

The following properties are written to JCR for this component and are expected to be available as `Resource` properties:

1. `./product` - stores the product SKUs or JCR paths of the products to be displayed
2. `./jcr:title` - Optional title text
3. `./titleType` - will store the HTML heading element type which will be used for rendering; if no value is defined, the component will fallback
to the `type` value defined by the component's policy. The property of the policy is called `type` so we can reuse the `core/wcm/components/commons/datasources/allowedheadingelements/v1` Servlet from the WCM components.

We also use a `SlingPostProcessor` in order to support "drag and drop" in the AEM Sites Editor, so that it is possible to easily add multiple products to the carousel. The custom processor only processes POST parameter keys starting with `./dropTarget->` so make sure that this doesn't collide with any other custom processor you might implement.

## BEM Description

```
BLOCK productcarousel
    ELEMENT productcarousel__btn
    ELEMENT productcarousel__btn--next
    ELEMENT productcarousel__btn--prev
    ELEMENT productcarousel__cardscontainer
    ELEMENT productcarousel__container
    ELEMENT productcarousel__parent
    ELEMENT productcarousel__root
    ELEMENT productcarousel__title

BLOCK product
    ELEMENT product__card
    ELEMENT product__card__image
    ELEMENT product__image
    ELEMENT product__price
```

## Information

- **Vendor**: Adobe
- **Version**: v1
- **Compatibility**: AEM 6.4 / 6.5
- **Status**: production-ready
