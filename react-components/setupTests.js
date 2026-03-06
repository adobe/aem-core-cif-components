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
import 'regenerator-runtime/runtime';
import '@testing-library/jest-dom';

// Mock UUID generation for deterministic form field IDs in snapshots (informed uses uuid or crypto)
let uuidCounter = 0;
const deterministicId = () =>
    `aaaaaaaa-bbbb-cccc-dddd-${String(++uuidCounter)
        .padStart(12, '0')
        .slice(-12)}`;

jest.mock('uuid', () => ({
    v4: () => deterministicId(),
    v1: () => deterministicId()
}));
if (typeof globalThis.crypto === 'undefined') {
    globalThis.crypto = {};
}
globalThis.crypto.randomUUID = deterministicId;
if (typeof global !== 'undefined') {
    global.crypto = global.crypto || {};
    global.crypto.randomUUID = deterministicId;
}
