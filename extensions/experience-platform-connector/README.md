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

# Experience Platform Connector

This Javascript library collects Storefront events and forwards them to AEP.

It exports the `useEventsCollector` React hook which subscribes to all storefront events.
This hook requires the Peregrine user context. So it must be used in a React component that is wrapped inside that context.
The hook takes the configuration as an argument:

```javascript
useEventsCollector({
    aep: { orgId: 'IMS ORG ID', datastreamId: 'The ID of the datastream used in AEP' },
    acds: false
});
```
