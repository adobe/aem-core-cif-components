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

1. `./productCount` - defines the maximum number of products displayed for a category when category selection is used
2. `./enableAddToCart` - displays the 'Add to Cart' button on the products (default `false`) 
3. `./enableAddToWishList` - displays the 'Add to Wish List' button on the products (default `false`)
4. `./type` - defines the default HTML heading element type (`h1` - `h6`) this component will use for its rendering


### Edit Dialog Properties

The following properties are written to JCR for this component and are expected to be available as `Resource` properties:

1. `./product` - stores the product SKUs or JCR paths of the products to be displayed when the selection type is `product`
2. `./jcr:title` - Optional title text
3. `./titleType` - will store the HTML heading element type which will be used for rendering; if no value is defined, the component will fallback
to the `type` value defined by the component's policy. The property of the policy is called `type` so we can reuse the `core/wcm/components/commons/datasources/allowedheadingelements/v1` Servlet from the WCM components.
4. `./linkTarget` - defines the link target of the links generated for the component.
5. `./enableAddToCart` - displays the 'Add to Cart' button on the products (default `false`) 
6. `./enableAddToWishList` - displays the 'Add to Wish List' button on the products (default `false`) 
7. `./selectionType` - when set to `product`, the component displays the products defined by the `./product` property; when set to `category`, the products are displayed from the category defined by the `./category` property 
8. `./category` - stores the UID of the category used for displaying the products when the selection type is `category`
9. `./productCount` - defines the maximum number of products displayed for a category when the selection type is `category` (default value is 10, minimum 1)
10. `./id` - defines the component HTML ID attribute

We also use a `SlingPostProcessor` in order to support "drag and drop" in the AEM Sites Editor, so that it is possible to easily add multiple products to the carousel. The custom processor only processes POST parameter keys starting with `./dropTarget->` so make sure that this doesn't collide with any other custom processor you might implement.

## BEM Description

```
BLOCK productcarousel
    ELEMENT productcarousel__btn
        MOD productcarousel__btn--next
        MOD productcarousel__btn--prev
    ELEMENT productcarousel__cardscontainer
    ELEMENT productcarousel__container
    ELEMENT productcarousel__parent
    ELEMENT productcarousel__root
    ELEMENT productcarousel__title

BLOCK product
    ELEMENT product__card
    ELEMENT product__card-content
    ELEMENT product__card-actions
    ELEMENT product__card-button
        MOD product__card-button--add-to-cart
        MOD product__card-button--add-to-wish-list
    ELEMENT product__card-button-content
    ELEMENT product__card__image
    ELEMENT product__card-title
    ELEMENT product__image
```

## Information

- **Vendor**: Adobe
- **Version**: v1
- **Compatibility**: AEM as a Cloud Service / AEM 6.4 / 6.5
- **Status**: production-ready
