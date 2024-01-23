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

# Searchbar (v2)

Searchbar component is a server-side component written in HTL, displaying a search input. After submitting the components redirects to the search results page defined by the `cq:cifSearchResultsPage` property and passing the search term as a query parameter.

The visibility of the component can be toggled using any DOM element with the `searchbar__trigger` CSS class assigned.

> [!NOTE]
> Adobe Commerce customers should use the [Live Search Widget](https://experienceleague.adobe.com/docs/experience-manager-cloud-service/content/content-and-commerce/integrations/live-search-popover.html?lang=en) instead.

## Features

- Input field with visibility toggle
- Forwarding to search result page
- Customizable placeholder via `placeholder` property

### Use Object

This component uses the `com.adobe.cq.commerce.core.components.models.searchbar.Searchbar` Sling model as its Use-object.

## BEM Description

```
BLOCK searchbar
    ELEMENT searchbar__root
    ELEMENT searchbar__trigger
    ELEMENT searchbar__trigger-icon
    ELEMENT searchbar__body
    ELEMENT searchbar__body--open
    ELEMENT searchbar__form-container
    ELEMENT searchbar__form
    ELEMENT searchbar__fields
    ELEMENT searchbar__input-container
    ELEMENT searchbar__input    
    ELEMENT searchbar__input-before
    ELEMENT searchbar__input-after
    ELEMENT searchbar__reset-button    
    ELEMENT searchbar__search-icon    
```

## Information

- **Vendor**: Adobe
- **Version**: v2
- **Compatibility**: AEM as a Cloud Service / 6.5
- **Status**: production-ready
