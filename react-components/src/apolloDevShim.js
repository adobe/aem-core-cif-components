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
// @apollo/client 3.3–3.5 initializes `__DEV__` only as a top-level side effect
// in utilities/globals/DEV.js, but its package.json marks that file side-effect
// free — so consumer webpack tree-shaking drops it and other Apollo modules
// then throw `ReferenceError: __DEV__ is not defined`. Fixed in @apollo/client
// 3.6+. The `typeof globalThis.__DEV__ === 'undefined'` guard makes this a
// no-op when consumers have already defined it.
if (typeof globalThis !== 'undefined' && typeof globalThis.__DEV__ === 'undefined') {
    Object.defineProperty(globalThis, '__DEV__', {
        value: typeof process !== 'undefined' && process.env && process.env.NODE_ENV !== 'production',
        enumerable: false,
        configurable: true,
        writable: true
    });
}
