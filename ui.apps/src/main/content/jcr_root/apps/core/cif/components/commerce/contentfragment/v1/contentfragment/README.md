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

# Commerce Content Fragment (v1)

Commerce Content Fragment component written in HTL that displays the elements of a content fragment or a selection thereof.
The content fragment is selected dynamically based on:
 * the content fragment model configured for the component
 * the category identifier of the category page or the product SKU of the product page
 * the category identifier or product SKU provided in the content fragment 


## Features

 * Displays the elements of a Content Fragment as an HTML description list
 * By default renders all elements of a Content Fragment
 * Can be configured to render a subset of the elements in a specific order

### Use Object

The Content Fragment component uses the `com.adobe.cq.wcm.core.components.models.contentfragment.ContentFragment` 
Sling model ehanced with commerce specific functions as its Use-object.

### Properties

The component has the following JCR properties:

1. `./modelPath` - defines the path to the Content Fragment Model defining the content fragments to be displayed
2. `./linkElement` - defines the element to be used to match the content fragment to the product SKU or category identifier
3. `./elementNames` - multi-valued property defining the elements to be rendered and in which order (optional: if not present, all elements are rendered)
4. `./parentPath` - parent path of the content fragments to be displayed    
5. `./paragraphScope` - defines if all or a range of paragraphs are to be rendered (only used in paragraph mode)
6. `./paragraphRange` - defines the range(s) of paragraphs to be rendered (only used in paragraph mode and if paragraphs are restricted to ranges)
7. `./paragraphHeadings` - defines if headings should count as paragraphs (only used in paragraph mode and if paragraphs are restricted to ranges)
8. `./id` - defines the component HTML ID attribute.

## BEM Description

See the BEM description of the [Content Fragment component](https://github.com/adobe/aem-core-wcm-components/tree/master/content/src/content/jcr_root/apps/core/wcm/components/contentfragment/v1/contentfragment#bem-description) of the AEM WCM Core Components library.

## Information

- **Vendor**: Adobe
- **Version**: v1
- **Compatibility**: AEM 6.5 / AEMaaCS
- **Status**: production-ready
