/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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

describe('Common Clientlib', () => {
    it('fires initialized event', async () => {
        const expected = new Promise(r => document.addEventListener('aem.cif.clientlib-initialized', r));
        const failed = new Promise(r => setTimeout(() => r('failed'), 1000));

        // dispatch the DOMContentLoaded event again
        const event = new Event('DOMContentLoaded');
        window.document.dispatchEvent(event);

        const result = await Promise.race([expected, failed]);

        expect(result).not.equal('failed');
    });

    it('instantiates GQL Client from body data set', async () => {
        document.body.setAttribute('data-graphql-endpoint', 'http://foo.bar/graphql');
        document.body.setAttribute('data-graphql-method', 'GET');
        document.body.setAttribute('data-store-view', 'test');

        // dispatch the DOMContentLoaded event again
        const event = new Event('DOMContentLoaded');
        window.document.dispatchEvent(event);

        expect(window.CIF.CommerceGraphqlApi).not.null;
        expect(window.CIF.CommerceGraphqlApi.endpoint).to.equal('http://foo.bar/graphql');
        expect(window.CIF.CommerceGraphqlApi.method).to.equal('GET');
        expect(window.CIF.CommerceGraphqlApi.storeView).to.equal('test');
    });

    it('instantiates GQL Client from store-config meta element', async () => {
        const meta = document.createElement('meta');
        meta.setAttribute(
            'content',
            JSON.stringify({
                graphqlEndpoint: 'http://bar.foo/graphql',
                graphqlMethod: 'POST',
                storeView: 'default'
            })
        );
        meta.setAttribute('name', 'store-config');
        document.head.insertAdjacentElement('beforeend', meta);

        // dispatch the DOMContentLoaded event again
        const event = new Event('DOMContentLoaded');
        window.document.dispatchEvent(event);

        expect(window.CIF.CommerceGraphqlApi).not.null;
        expect(window.CIF.CommerceGraphqlApi.endpoint).to.equal('http://bar.foo/graphql');
        expect(window.CIF.CommerceGraphqlApi.method).to.equal('POST');
        expect(window.CIF.CommerceGraphqlApi.storeView).to.equal('default');
    });
});
