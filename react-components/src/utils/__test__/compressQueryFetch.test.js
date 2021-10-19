/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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
    const baseUrl = 'http://localhost';

    it.each([[`${baseUrl}/`], ['https://localhost/api/graphql'], ['//localhost/'], ['/api/graphql/'], ['/']])(
        'compresses queries in query params of url %s',
        url => {
            let queryParams = new URLSearchParams();
            queryParams.set('query', sampleQuery);
            const urlWithQuery = `${url}?${queryParams.toString()}`;

            compressQueryFetch(urlWithQuery, { method: 'GET' });

            // Verify called url
            expect(fetch).toHaveBeenCalled();
            const calledUrl = fetch.mock.calls[0][0];
            expect(calledUrl.startsWith(url)).toBe(true);

            // Verify query params
            const compressedUrl = new URL(calledUrl, baseUrl);
            expect(compressedUrl.searchParams.get('query')).toBe(expectedQuery);
        }
    );

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
