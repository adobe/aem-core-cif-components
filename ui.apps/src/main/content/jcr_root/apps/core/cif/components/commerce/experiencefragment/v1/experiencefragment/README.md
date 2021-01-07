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

# Commerce Experience Fragment (v1)

The Commerce Experience Fragement component is a server-side component written in HTL, allowing to dynymically display an experience fragment based on:
* the product or category identifier defined in the page URL
* the product or category identifier defined in each experience fragment
* the target location defined in each experience fragment
 

## Features

- Searches and displays an experience fragment


### Edit Dialog Properties

The following properties are written to JCR for this component and are expected to be available as `Resource` properties:

- `fragmentLocation` - an optional location name that must match the location name defined in experience fragments that should be displayed in that component.


## BEM Description

```
BLOCK cmp-experiencefragment
  MOD cmp-experiencefragment--<name>  
```

## Information

- **Vendor**: Adobe
- **Version**: v1
- **Compatibility**: AEM 6.4 / 6.5
- **Status**: production-ready
