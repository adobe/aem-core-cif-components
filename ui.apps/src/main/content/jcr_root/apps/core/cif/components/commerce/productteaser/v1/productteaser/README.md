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
2. `./enableAddToWishList` - enables/disables the add to wish list button, defaults to disabled

### Edit Dialog Properties

The following properties are written to JCR for this component and are expected to be available as `Resource` properties:

1. `./selection` - identifies the product to be displayed.
2. `./cta` - defines the call to action of the product teaser, may one of `add-to-card`, `details` or empty.
3. `./ctaText` - defines a text that overwrites the default call to action label.
4. `./linkTarget` - defines the link target of the links generated for the component.
5. `./id` - defines the component HTML ID attribute.

### Accessibility and Template Overrides

The image and title remain separate links to preserve existing markup, styling, JavaScript hooks and mouse interaction.
The core `productteaser.html` passes `titleLinked=true` to `imageTpl.actions`, because the core title template always renders a link.
This adds `tabindex="-1"` and `aria-hidden="true"` to the image anchor, leaving the title as the single product-page link for keyboard and screen-reader navigation.
The image remains clickable with a mouse.

The image template's optional `titleLinked` parameter defaults to false when omitted.
Custom main templates that omit it retain the previous image-link behavior, with neither attribute rendered.
Pass `titleLinked=true` only when a separate, accessible title link is present.
If an override of `title.html` removes the title link, also override `productteaser.html` to omit `titleLinked` or pass `titleLinked=false`; otherwise the product has no accessible image or title link.
Projects using the core templates through proxy components are also affected if they hide the title with CSS: `display:none` or `visibility:hidden` has the same effect as removing the title link.
Use a visually-hidden (clip) style that keeps the title link accessible instead, or override `productteaser.html` to pass `titleLinked=false`.

HTL ignores call arguments that a template does not declare.
Overrides of only `image.html` that do not declare `titleLinked` therefore keep the previous behavior: nothing breaks, but they do not get the accessibility fix.
To adopt the fix, declare the parameter and render the conditional attributes while preserving the override's existing image markup:

```html
<template data-sly-template.actions="${@ product, titleLinked}">
    <a class="item__images" href="${product.url}" title="${product.name}" target="${product.linkTarget}"
       tabindex="${titleLinked ? '-1' : null}" aria-hidden="${titleLinked ? 'true' : null}">
        <img class="item__image" width="100%" height="100%" src="${product.image}" alt="${product.imageAlt}" />
    </a>
</template>
```

Both image and title links honor `./linkTarget`: `_blank` opens either link in a new tab, while the default `_self` is normalized by the model so neither anchor renders a `target` attribute.
Existing teasers with "open in new tab" (`./linkTarget=_blank`) enabled now open the title link in a new tab as well.
This is a visible behavior change on existing pages; the option was originally introduced for the CTA in [#904](https://github.com/adobe/aem-core-cif-components/pull/904), so authors may not expect it to affect the title.
Projects such as Venia with their own main and title templates must opt in separately; reusing the core image template alone does not change their behavior.

Automated tests using role-based queries such as `getByRole('link')` or `getByRole('img')` exclude `aria-hidden` elements by default.
When `titleLinked=true`, this includes both the image anchor and its child image; adjust queries that previously expected them to be exposed.

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
