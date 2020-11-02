/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
 *    This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License. You may obtain a copy
 *    of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under
 *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *    OF ANY KIND, either express or implied. See the License for the specific language
 *    governing permissions and limitations under the License.
 *
 ******************************************************************************/

import compressQueryFetch from '../compressQueryFetch';

describe('compressQueryFetch', () => {
    // eslint-disable-next-line
    global.fetch = jest.fn();

    beforeEach(() => {
        fetch.mockClear();
    });

    const sampleQuery = `{
        categoryList(filters: {ids: {eq: "2"}}) {
            id
            name
        }
    }`;

    const expectedQuery = '{categoryList(filters:{ids:{eq:"2"}}){id name}}';

    it('compresses queries in query params', () => {
        let url = new URL('http://localhost');
        url.searchParams.set('query', sampleQuery);

        compressQueryFetch(url, { method: 'GET' });

        expect(fetch).toHaveBeenCalled();
        const compressedUrl = new URL(fetch.mock.calls[0][0]);
        expect(compressedUrl.searchParams.get('query')).toBe(expectedQuery);
    });

    it('compresses queries in body', () => {
        const body = {
            query: sampleQuery
        };

        compressQueryFetch('', { method: 'POST', body: JSON.stringify(body) });
        expect(fetch).toHaveBeenCalled();
        let compressedBody = JSON.parse(fetch.mock.calls[0][1].body);
        expect(compressedBody.query).toBe(expectedQuery);
    });
});
