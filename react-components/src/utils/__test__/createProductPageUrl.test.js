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
import { createProductPageUrl } from '../createProductPageUrl';

describe('createProductPageUrl', () => {
    beforeEach(() => {
        delete document.body.dataset.storeRootUrl;
    });

    it('returns null if storeRootUrl is not set', () => {
        const url = createProductPageUrl('my-product');

        expect(url).toBeNull();
    });

    it('creates a product page url for a product sku', () => {
        document.body.dataset.storeRootUrl = '/content/venia/us/en.html';

        const url = createProductPageUrl('my-product');

        expect(url).toEqual('http://localhost/content/venia/us/en.cifproductredirect.html/my-product');
    });
});
