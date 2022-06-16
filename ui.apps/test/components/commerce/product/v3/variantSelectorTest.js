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

import VariantSelector from '../../../../../src/main/content/jcr_root/apps/core/cif/components/commerce/product/v3/product/clientlib/js/variantSelector.js';

describe('Product', () => {
    describe('VariantSelector', () => {
        let selectorRoot;
        let selectorRootUid;
        let variantData = [
            {
                name: 'Red Jeans',
                sku: 'red',
                variantAttributes: {
                    color: 'red'
                },
                variantAttributesUid: {
                    color: 'cmVk'
                }
            },
            {
                name: 'Blue Jeans',
                sku: 'blue',
                variantAttributes: {
                    color: 'blue'
                },
                variantAttributesUid: {
                    color: 'Ymx1ZQ=='
                }
            }
        ];

        beforeEach(() => {
            selectorRoot = document.createElement('div');

            selectorRoot.insertAdjacentHTML(
                'afterbegin',
                `
                <div data-cmp-is="product" data-product-sku="sample-sku">
                    <div class="productFullDetail__options">
                        <div class="tileList__root" data-id="color">
                            <button class="swatch__root" data-id="red" />
                            <button data-id="blue" />
                        </div>
                    </div>
                    <div class="productFullDetail__quantity">
                        <select>
                            <option value="1" selected="">1</option>
                            <option value="2">2</option>
                            <option value="3">3</option>
                            <option value="4">4</option>
                        </select>
                    </div>
                </div>
                `
            );

            selectorRoot.querySelector(VariantSelector.selectors.self).dataset.variants = JSON.stringify(variantData);

            selectorRootUid = document.createElement('div');

            selectorRootUid.insertAdjacentHTML(
                'afterbegin',
                `
                <div data-cmp-is="product" data-uid-cart data-product-sku="sample-sku">
                    <div class="productFullDetail__options">
                        <div class="tileList__root" data-id="color">
                            <button class="swatch__root" data-id="cmVk" />
                            <button data-id="Ymx1ZQ==" />
                        </div>
                    </div>
                    <div class="productFullDetail__quantity">
                        <select data-uid-cart>
                            <option value="1" selected="">1</option>
                            <option value="2">2</option>
                            <option value="3">3</option>
                            <option value="4">4</option>
                        </select>
                    </div>
                </div>
                `
            );

            selectorRootUid.querySelector(VariantSelector.selectors.self).dataset.variants = JSON.stringify(
                variantData
            );
        });

        afterEach(() => {
            window.location.hash = '';
        });

        it('initializes a variantselector component', () => {
            let selector = new VariantSelector({
                element: selectorRoot.querySelector(VariantSelector.selectors.self),
                product: selectorRoot.querySelector(VariantSelector.selectors.product)
            });

            assert.deepEqual(selector._state.variantData, variantData);
            assert.equal(selector._state.buttons.length, 2);
        });

        it('initializes variant from a window location hash', () => {
            window.location.hash = '#red';

            let selector = new VariantSelector({
                element: selectorRoot.querySelector(VariantSelector.selectors.self),
                product: selectorRoot.querySelector(VariantSelector.selectors.product)
            });

            assert.equal(selector._state.variant.sku, 'red');
        });

        it('initializes variant from a window location hash (UID)', () => {
            window.location.hash = '#red';

            let selector = new VariantSelector({
                element: selectorRootUid.querySelector(VariantSelector.selectors.self),
                product: selectorRootUid.querySelector(VariantSelector.selectors.product)
            });

            assert.equal(selector._state.variant.sku, 'red');
        });

        it('initializes base product for invalid window location hash', () => {
            window.location.hash = '#purple';

            let selector = new VariantSelector({
                element: selectorRoot.querySelector(VariantSelector.selectors.self),
                product: selectorRoot.querySelector(VariantSelector.selectors.product)
            });

            assert.isNull(selector._state.variant);
        });

        it('initializes base product for invalid window location hash (UID)', () => {
            window.location.hash = '#purple';

            let selector = new VariantSelector({
                element: selectorRootUid.querySelector(VariantSelector.selectors.self),
                product: selectorRootUid.querySelector(VariantSelector.selectors.product)
            });

            assert.isNull(selector._state.variant);
        });

        it('returns the selected variant based on attributes', () => {
            let selector = new VariantSelector({
                element: selectorRoot.querySelector(VariantSelector.selectors.self),
                product: selectorRoot.querySelector(VariantSelector.selectors.product)
            });
            selector._state.attributes['color'] = 'red';

            let selectedVariant = selector._findSelectedVariant();
            assert.equal(selectedVariant.name, variantData[0].name);
        });

        it('returns the selected variant based on attributes (UID)', () => {
            let selector = new VariantSelector({
                element: selectorRootUid.querySelector(VariantSelector.selectors.self),
                product: selectorRootUid.querySelector(VariantSelector.selectors.product)
            });
            selector._state.attributes['color'] = 'cmVk';

            let selectedVariant = selector._findSelectedVariant();
            assert.equal(selectedVariant.name, variantData[0].name);
        });

        it('returns the selected variant based on sku', () => {
            let selector = new VariantSelector({
                element: selectorRoot.querySelector(VariantSelector.selectors.self),
                product: selectorRoot.querySelector(VariantSelector.selectors.product)
            });

            let selectedVariant = selector._findSelectedVariant('blue');
            assert.equal(selectedVariant.name, variantData[1].name);
        });

        it('returns the selected variant based on sku (UID)', () => {
            let selector = new VariantSelector({
                element: selectorRootUid.querySelector(VariantSelector.selectors.self),
                product: selectorRootUid.querySelector(VariantSelector.selectors.product)
            });

            let selectedVariant = selector._findSelectedVariant('blue');
            assert.equal(selectedVariant.name, variantData[1].name);
        });

        it('returns null if no variant can be found', () => {
            let selector = new VariantSelector({
                element: selectorRoot.querySelector(VariantSelector.selectors.self),
                product: selectorRoot.querySelector(VariantSelector.selectors.product)
            });
            selector._state.attributes['color'] = 'purple';

            let selectedVariant = selector._findSelectedVariant();
            assert.isNull(selectedVariant);
        });

        it('returns null if no variant can be found (UID)', () => {
            let selector = new VariantSelector({
                element: selectorRootUid.querySelector(VariantSelector.selectors.self),
                product: selectorRootUid.querySelector(VariantSelector.selectors.product)
            });
            selector._state.attributes['color'] = 'cHVycGxl';

            let selectedVariant = selector._findSelectedVariant();
            assert.isNull(selectedVariant);
        });

        it('dispatches a variantchanged event on changing the variant', () => {
            let selector = new VariantSelector({
                element: selectorRoot.querySelector(VariantSelector.selectors.self),
                product: selectorRoot.querySelector(VariantSelector.selectors.product)
            });
            let spy = sinon.spy();
            selectorRoot.addEventListener(VariantSelector.events.variantChanged, spy);

            // Simulate button click
            selector._onSelectVariant({
                target: selectorRoot.querySelector("[data-id='red']"),
                preventDefault: () => {}
            });

            assert.isTrue(spy.called);
        });

        it('dispatches a variantchanged event on changing the variant (UID)', () => {
            let selector = new VariantSelector({
                element: selectorRootUid.querySelector(VariantSelector.selectors.self),
                product: selectorRootUid.querySelector(VariantSelector.selectors.product)
            });
            let spy = sinon.spy();
            selectorRootUid.addEventListener(VariantSelector.events.variantChanged, spy);

            // Simulate button click
            selector._onSelectVariant({
                target: selectorRootUid.querySelector("[data-id='cmVk']"),
                preventDefault: () => {}
            });

            assert.isTrue(spy.called);
        });

        it('updates the window location hash on changing the variant', () => {
            let selector = new VariantSelector({
                element: selectorRoot.querySelector(VariantSelector.selectors.self),
                product: selectorRoot.querySelector(VariantSelector.selectors.product)
            });

            // Simulate button click
            selector._onSelectVariant({
                target: selectorRoot.querySelector("[data-id='red']"),
                preventDefault: () => {}
            });

            // Verify location hash
            assert.equal(window.location.hash, '#red');
        });

        it('updates the window location hash on changing the variant (UID)', () => {
            let selector = new VariantSelector({
                element: selectorRootUid.querySelector(VariantSelector.selectors.self),
                product: selectorRootUid.querySelector(VariantSelector.selectors.product)
            });

            // Simulate button click
            selector._onSelectVariant({
                target: selectorRootUid.querySelector("[data-id='cmVk']"),
                preventDefault: () => {}
            });

            // Verify location hash
            assert.equal(window.location.hash, '#red');
        });

        it('updates swatch button on receiving a variantchanged event', () => {
            let selector = new VariantSelector({
                element: selectorRoot.querySelector(VariantSelector.selectors.self),
                product: selectorRoot.querySelector(VariantSelector.selectors.product)
            });
            selector._state.attributes['color'] = 'red';

            selector._updateButtonActiveClass();

            // Check that red button is selected
            let redButton = selectorRoot.querySelector("[data-id='red']");
            assert.isTrue(redButton.classList.contains('swatch__root_selected'));
            assert.isFalse(redButton.classList.contains('tile__root_selected'));
        });

        it('updates swatch button on receiving a variantchanged event (UID)', () => {
            let selector = new VariantSelector({
                element: selectorRootUid.querySelector(VariantSelector.selectors.self),
                product: selectorRootUid.querySelector(VariantSelector.selectors.product)
            });
            selector._state.attributes['color'] = 'cmVk';

            selector._updateButtonActiveClass();

            // Check that red button is selected
            let redButton = selectorRootUid.querySelector("[data-id='cmVk']");
            assert.isTrue(redButton.classList.contains('swatch__root_selected'));
            assert.isFalse(redButton.classList.contains('tile__root_selected'));
        });

        it('updates tile button on receiving a variantchanged event', () => {
            let selector = new VariantSelector({
                element: selectorRoot.querySelector(VariantSelector.selectors.self),
                product: selectorRoot.querySelector(VariantSelector.selectors.product)
            });
            selector._state.attributes['color'] = 'blue';

            selector._updateButtonActiveClass();

            // Check that blue button is selected
            let blueButton = selectorRoot.querySelector("[data-id='blue']");
            assert.isTrue(blueButton.classList.contains('tile__root_selected'));
            assert.isFalse(blueButton.classList.contains('swatch__root_selected'));
        });

        it('updates tile button on receiving a variantchanged event (UID)', () => {
            let selector = new VariantSelector({
                element: selectorRootUid.querySelector(VariantSelector.selectors.self),
                product: selectorRootUid.querySelector(VariantSelector.selectors.product)
            });
            selector._state.attributes['color'] = 'Ymx1ZQ==';

            selector._updateButtonActiveClass();

            // Check that blue button is selected
            let blueButton = selectorRootUid.querySelector("[data-id='Ymx1ZQ==']");
            assert.isTrue(blueButton.classList.contains('tile__root_selected'));
            assert.isFalse(blueButton.classList.contains('swatch__root_selected'));
        });
    });
});
