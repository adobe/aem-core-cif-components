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

# Product Search Results (v1)

Product Search Results component is a server-side component written in HTL, displaying product search results in a gallery view. The search query
term is read from request parameters of the page and the data is retrieved from Magento via GraphQL. The main usage of this component would be on a search result page.

## Features

- Support for basic full-text search
- Display products in gallery view, incl. name, thumbnail, and price
- Style System support.

### Use Object

This component uses the `com.adobe.cq.commerce.core.components.models.searchresults.SearchResults` Sling model as its Use-object.

### Selectors & Request Parameters

This component is targeted for a search result display page showing the product results of a full-text search.

1. `search_query` parameter containing the search query term

## BEM Description

```
BLOCK category
    ELEMENT category__root
    ELEMENT category__root-message
    ELEMENT category__title
    ELEMENT category__pagination
    ELEMENT category__placeholder
    ELEMENT category__categoryTitle
    ELEMENT category__image

BLOCK gallery
    ELEMENT gallery__root
    ELEMENT gallery__items

BLOCK item
    ELEMENT item__root
    ELEMENT item__images
    ELEMENT item__image
    ELEMENT item__imagePlaceholder
    ELEMENT item__name
    ELEMENT item__price
```

## Information

- **Vendor**: Adobe
- **Version**: v1
- **Compatibility**: AEM 6.4 / 6.5
- **Status**: production-ready
