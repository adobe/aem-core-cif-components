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
# Mini Account (v2)

The Mini Account is a client-side component written in React which renders the sign-in form and account menu options.

This AEM component only renders a container `div` for the [React component](/react-components/src/components/AccountContainer). 
All configurations, either of the component or it's content policy are made available as data attributes to this host element:

- `data-show-wish-list` - will be set if the favourites list is enabled.

## Features

* Allows enabling or disabling the favorites list in the account menu.
* Style System support.

### Use Object

The Mini Account component uses the `com.adobe.cq.commerce.core.components.models.account.MiniAccount` Sling model as its Use-object.

### Component Policy Configuration Properties

The following configuration properties are used:

1. `./enableWishList` - enables/disables the favorites list account menu entry

### Edit Dialog Properties

The following properties are written to JCR for this component and are expected to be available as `Resource` properties:

1. `./id` - defines the component HTML ID attribute.

## BEM Description

```
BLOCK miniaccount
    ELEMENT miniaccount__root
    ELEMENT miniaccount__body    
```

## License information

-   Vendor: Adobe
-   Version: v2
-   Compatibility: AEM as a Cloud Service / 6.5
-   Status: production-ready
