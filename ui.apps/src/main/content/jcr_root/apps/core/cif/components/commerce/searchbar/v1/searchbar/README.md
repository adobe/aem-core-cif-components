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

# Searchbar (v1)

Searchbar component is a server-side component written in HTL, displaying a search input. After submitting the components redirects to the search results page defined by the `cq:cifSearchResultsPage` property and passing the search term as a query parameter.

The visibility of the component can be toggled using any DOM element with the `searchTrigger__root` CSS class assigned.

## Features

- Input field with visibility toggle
- Forwarding to search result page
- Customizable placeholder via `placeholder` property

### Use Object

This component uses the `com.adobe.cq.commerce.core.components.models.searchbar.Searchbar` Sling model as its Use-object.

## BEM Description

```
BLOCK searchbar
    ELEMENT searchBar__root
    searchBar__searchInner
    searchBar__form
    textInput__input
    field__input
    message__root
    searchBar__SearchAutocompleteWrapper
    fieldIcons__root
    fieldIcons__input
    fieldIcons__before
    fieldIcons__after
    icon__root
```

## Information

- **Vendor**: Adobe
- **Version**: v1
- **Compatibility**: AEM 6.4 / 6.5
- **Status**: production-ready
