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

# Commerce Teaser (v3)

Commerce Teaser component written in HTL extends core wcm component Teaser, In addition to allowing definition of an image, title, rich text description and actions/links it also supports Call-To-Action to a commerce product or category page.
Teaser variations can include some or all of these elements.

## Features

* Call to action linked to page, product page or category page.
* Combines image, title, rich text description and actions/links.
* Allows disabling of teaser elements through policy configuration.
* Allows control over whether title and description should be inherited from a linked page.
* Style System support.

### Use Object

This component uses (specifically for `action` node processing) `com.adobe.cq.commerce.core.components.models.teaser.CommerceTeaser` Sling Model as it's use Object.
And as CommerceTeaser component is extended from core wcm Teaser component, it further uses the `com.adobe.cq.wcm.core.components.models.Teaser` Sling model as its Use-object,  

### Component Policy Configuration Properties
The following configuration properties are used:

1. `./actionsDisabled` - defines whether or not Call-to-Actions are disabled
2. `./pretitleHidden` - defines whether or not the pretitle is hidden
3. `./titleHidden` - defines whether or not the title is hidden
4. `./descriptionHidden` - defines whether or not the description is hidden
5. `./imageLinkHidden` - defines whether or not the image link is hidden
6. `./titleLinkHidden` - defines whether or not the title link is hidden
7. `./titleType` - stores the value for this title's HTML element type

The following configuration properties are inherited from the image component:

1. `./allowedRenditionWidths` - defines the allowed renditions (as an integer array) that will be generated for the images rendered by this
component; the actual size will be requested by the client device
2. `./disableLazyLoading` - if `true`, the lazy loading of images (loading only when the image is visible on the client
device) is disabled

### Edit Dialog Properties
The following properties are written to JCR for this Teaser component and are expected to be available as `Resource` properties:

1. `./actionsEnabled` - property that defines whether or not the teaser has Call-to-Action elements
2. `./actions` - child node where the Call-to-Action elements are stored as a list of `item` nodes with the following properties
    1. `./link` - property that stores the Call-to-Action link
    2. `./productSku` - property that stores the Call-to-Action link to a product page for selected product
    3. `./categoryID` - property that stores the Call-to-Action to Selected category, it is preferred over `productSku`
    4. `./text` - property that stores the Call-to-Action text
    5. `./linkTarget` - defines the link target of the links generated for the action.
3. `./fileReference` - property or `file` child node - will store either a reference to the image file, or the image file
4. `./linkURL` - link applied to teaser elements. URL or path to a content page
5. `./linkTarget` - defines the link target of the links generated for the title
6. `./pretitle` - defines the value of the teaser pretitle
7. `./jcr:title` - defines the value of the teaser title and HTML `title` attribute of the teaser image
8. `./titleFromPage` - defines whether or not the title value is taken from the linked page
9. `./jcr:description` - defines the value of the teaser description
10. `./descriptionFromPage` - defines whether or not the description value is taken from the linked page
11. `./id` - defines the component HTML ID attribute.
12. `./titleType` - stores the value for this title's HTML element type
13. `./isDecorative` - if set to `true`, then the image will be ignored by assistive technology
14. `./alt` - defines the value of the HTML `alt` attribute (not needed if `./isDecorative` is set to `true`)
15. `./altValueFromDAM` - if `true`, the HTML `alt` attribute is inherited from the DAM asset.
16. `./imageFromPageImage` - if `true`, the image is inherited from the featured image of either the linked page if `./linkURL` is set or the current page.
17. `./altValueFromPageImage` - if `true` and if `./imageFromPageImage` is `true`, the HTML `alt` attribute is inherited from the featured image of either the linked page if `./linkURL` is set or the current page.

## Client Libraries
The component reuses the following editor client library categories that include JavaScript
handling for dialog interaction. They are already included by its edit dialog:
* `core.cif.components.teaser.v3.editor`
* `core.wcm.components.teaser.v1.design`
* `core.wcm.components.teaser.v2.editor`
* `core.wcm.components.image.v3.editor`


### Extending the Teaser Component
When extending the Teaser component by using `sling:resourceSuperType`, developers need to define the `imageDelegate` property for
the proxy component and point it to the designated Image component.

For example:
```
imageDelegate="core/wcm/components/image/v3/image"
```

## BEM Description
```
BLOCK cmp-teaser
    ELEMENT cmp-teaser__image
    ELEMENT cmp-teaser__content
    ELEMENT cmp-teaser__pretitle
    ELEMENT cmp-teaser__title
    ELEMENT cmp-teaser__title-link
    ELEMENT cmp-teaser__description
    ELEMENT cmp-teaser__action-container
    ELEMENT cmp-teaser__action-link
```

## Information

* **Vendor**: Adobe
* **Version**: v3
* **Compatibility**: AEM as a Cloud Service / AEM 6.5
* **Status**: production-ready
