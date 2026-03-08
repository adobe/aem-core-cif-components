/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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
import '@testing-library/jest-dom';

// Suppress jsdom getComputedStyle not implemented errors (from @testing-library/dom)
const originalError = console.error;
console.error = (...args) => {
    const msg = String(args[0] || '');
    if (msg.includes('Not implemented: window.computedStyle') || msg.includes('computedStyle(elt, pseudoElt)')) {
        return;
    }
    originalError.apply(console, args);
};

const UUID_REGEX = /[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}/g;

// Replace dynamic UUIDs in snapshots with placeholder for stable test output
expect.addSnapshotSerializer({
    test: (val) => {
        if (typeof val === 'string') return /[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}/.test(val);
        if (val && typeof val === 'object' && val.nodeType === 11) return true; // DocumentFragment
        return false;
    },
    serialize: (val, config, indentation, depth, refs, printer) => {
        let str;
        if (typeof val === 'string') {
            str = val;
        } else if (val && val.nodeType === 11) {
            const div = document.createElement('div');
            div.appendChild(val.cloneNode(true));
            str = div.innerHTML;
        } else {
            str = printer(val, config, indentation, depth, refs);
        }
        return str.replace(UUID_REGEX, '[uuid]');
    }
});
