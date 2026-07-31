/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
'use strict';

// Importing the common clientlib registers its DOMContentLoaded handler which is
// responsible for populating the global window.CIF namespace (PriceFormatter,
// CommerceGraphqlApi, etc.).
import '../src/main/content/jcr_root/apps/core/cif/clientlibs/common/js/index.js';

// Root-level hook that runs once before every suite. Depending on the browser and
// the document readyState at bundle execution time, the common clientlib may defer
// its initialization to the DOMContentLoaded event instead of running synchronously.
// In that case window.CIF stays undefined until the event is fired, which breaks any
// suite that runs before the "Common Clientlib" tests (e.g. ProductTeaser and the
// add-to-cart/add-to-wishlist components rely on window.CIF being available). We
// dispatch the event here so window.CIF is deterministically initialized before the
// first suite runs, regardless of the browser (this only manifested in Firefox).
before(() => {
    window.document.dispatchEvent(new Event('DOMContentLoaded'));
});
