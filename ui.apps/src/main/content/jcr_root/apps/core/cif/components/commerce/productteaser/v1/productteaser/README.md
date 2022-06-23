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

# Product Teaser (v1)

Product Teaser component is a server side component written in HTL, allowing to display product teasers on any AEM page linking to the product details.
The product data is retrieved from Magento via GraphQL. The component is configured by an author via component dialog or drag & drop.

## Features

* Display product teaser including image, name & price
* Configurable product
* Support for product drag & drop
* Style System support.

### Use Object

The Product Teaser component uses the `com.adobe.cq.commerce.core.components.models.productteaser.ProductTeaser` Sling model as its Use-object.

### Component Policy Configuration Properties

The following configuration properties are used:

1. `./cq:DropTargetConfig` - controls the drag & drop behavior of group "product" items onto this component
2. `./loadClientPrice` - load current prices client side (default `false`)
3. `./enableAddToWishList` - enables/disables the add to wish list button, defaults to disabled

### Edit Dialog Properties

The following properties are written to JCR for this component and are expected to be available as `Resource` properties:

1. `./selection` - identifies the product to be displayed.
2. `./cta` - defines the call to action of the product teaser, may one of `add-to-card`, `details` or empty.
3. `./ctaText` - defines a text that overwrites the default call to action label.
4. `./linkTarget` - defines the link target of the links generated for the component.
5. `./id` - defines the component HTML ID attribute.

## BEM Description

```
BLOCK item
    ELEMENT item__root
    ELEMENT item__images
    ELEMENT item__image
    ELEMENT item__name
    ELEMENT price
    ELEMENT productteaser__cta
    ELEMENT button__root

BLOCK blank
    ELEMENT blank-placeholder
```

## Information

* **Vendor**: Adobe
* **Version**: v1
* **Compatibility**: AEM as a Cloud Service / AEM 6.4 / 6.5
* **Status**: production-ready
