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
import * as localStorageUtils from '../localStorageUtils';
import LocalStorageMock from '../mocks/mockLocalStorage';

describe('localStorageUtils', () => {
    window.localStorage = new LocalStorageMock();

    beforeEach(() => {
        localStorage.clear();
    });

    it('checks if items are present', () => {
        localStorage.setItem('test-1', 'test');
        expect(localStorageUtils.checkItem('test-1')).toBe(true);
        expect(localStorageUtils.checkItem('test-2')).toBe(false);
    });

    it('gets a string value', () => {
        localStorage.setItem('test-1', 'test');
        expect(localStorageUtils.getPlainItem('test-1')).toEqual('test');
        expect(localStorageUtils.getPlainItem('test-2')).toBeNull();
    });

    it('gets a JSON value', () => {
        const jsonItem = {
            foo: 'bar'
        };

        localStorage.setItem('test-1', JSON.stringify(jsonItem));

        expect(localStorageUtils.getJsonItem('test-1')).toEqual(jsonItem);
        expect(localStorageUtils.getJsonItem('test-2')).toBeNull();
    });
});
