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

// Global test bootstrap: make sure window.CIF is initialized before any suite runs.
// The common clientlib builds window.CIF on DOMContentLoaded, and newer browser
// versions can run the suites before that event fires. So we import the clientlib
// and fire DOMContentLoaded once, up front, so window.CIF is always ready first.
// This keeps the tests passing on newer browser versions (e.g. Firefox 153+).
import '../src/main/content/jcr_root/apps/core/cif/clientlibs/common/js/index.js';

before(() => {
    window.document.dispatchEvent(new Event('DOMContentLoaded'));
});
