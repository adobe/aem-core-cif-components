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
import LocalStorageMock from '../mocks/mockLocalStorage';

describe('authUtils', () => {
    window.localStorage = new LocalStorageMock();

    jest.mock('../cookieUtils', () => ({
        checkCookie: jest.fn(),
        cookieValue: jest.fn()
    }));

    beforeEach(() => {
        localStorage.clear();
        jest.clearAllMocks();
    });

    it('gets empty token', async () => {
        const { checkCookie } = await import('../cookieUtils');
        checkCookie.mockReturnValue(false);

        const { getAuthToken } = await import('../authUtils');
        expect(getAuthToken()).toEqual('');
    });

    it('gets the token from cookie', async () => {
        const { checkCookie, cookieValue } = await import('../cookieUtils');
        checkCookie.mockReturnValue(true);
        cookieValue.mockReturnValue('token-1');

        const { getAuthToken } = await import('../authUtils');
        expect(getAuthToken()).toEqual('token-1');
    });

    it('gets the token from localStorage', async () => {
        const { checkCookie } = await import('../cookieUtils');
        checkCookie.mockReturnValue(false);

        const jsonToken = {
            ttl: 3600,
            value: '"token-2"',
            timeStored: new Date().getTime()
        };

        localStorage.setItem('M2_VENIA_BROWSER_PERSISTENCE__signin_token', JSON.stringify(jsonToken));

        const { getAuthToken } = await import('../authUtils');
        expect(getAuthToken()).toEqual('token-2');
    });

    it('gets the token from localStorage with cookie set', async () => {
        const { checkCookie, cookieValue } = await import('../cookieUtils');
        checkCookie.mockReturnValue(true);
        cookieValue.mockReturnValue('token-1');

        const jsonToken = {
            ttl: 3600,
            value: '"token-2"',
            timeStored: new Date().getTime()
        };

        localStorage.setItem('M2_VENIA_BROWSER_PERSISTENCE__signin_token', JSON.stringify(jsonToken));

        const { getAuthToken } = await import('../authUtils');
        expect(getAuthToken()).toEqual('token-2');
    });
});
