/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
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

'use strict';

describe('Product', () => {
    describe('VariantSelector', () => {
        let selectorRoot;
        let variantData = [
            {
                name: 'Red Jeans',
                variantAttributes: {
                    color: 'red'
                }
            },
            {
                name: 'Blue Jeans',
                variantAttributes: {
                    color: 'blue'
                }
            }
        ];

        beforeEach(() => {
            selectorRoot = document.createElement('div');
            selectorRoot.dataset.variants = JSON.stringify(variantData);

            selectorRoot.insertAdjacentHTML(
                'afterbegin',
                `<div class="productFullDetail__options">
                    <div class="tileList__root" data-id="color">
                        <button class="swatch__root" data-id="red" />
                        <button data-id="blue" />
                    </div>
                </div>`
            );
        });

        it('initializes a variantselector component', () => {
            let selector = variantSelectorCtx.factory({ element: selectorRoot });

            assert.deepEqual(selector._state.variantData, variantData);
            assert.equal(selector._state.buttons.length, 2);
        });

        it('returns the selected variant', () => {
            let selector = variantSelectorCtx.factory({ element: selectorRoot });
            selector._state.attributes['color'] = 'red';

            let selectedVariant = selector._findSelectedVariant();
            assert.equal(selectedVariant.name, variantData[0].name);
        });

        it('returns null if no variant can be found', () => {
            let selector = variantSelectorCtx.factory({ element: selectorRoot });
            selector._state.attributes['color'] = 'purple';

            let selectedVariant = selector._findSelectedVariant();
            assert.isNull(selectedVariant);
        });

        it('dispatches a variantchanged event on changing the variant', () => {
            let selector = variantSelectorCtx.factory({ element: selectorRoot });
            let spy = sinon.spy();
            selectorRoot.addEventListener(variantSelectorCtx.VariantSelector.events.variantChanged, spy);

            // Simulate button click
            selector._onSelectVariant({
                target: selectorRoot.querySelector("[data-id='red']"),
                preventDefault: () => {}
            });

            assert.isTrue(spy.called);
        });

        it('updates swatch button on receiving a variantchanged event', () => {
            let selector = variantSelectorCtx.factory({ element: selectorRoot });
            selector._state.attributes['color'] = 'red';

            selector._updateButtonActiveClass();

            // Check that red button is selected
            let redButton = selectorRoot.querySelector("[data-id='red']");
            assert.isTrue(redButton.classList.contains('swatch__root_selected'));
            assert.isFalse(redButton.classList.contains('tile__root_selected'));
        });

        it('updates tile button on receiving a variantchanged event', () => {
            let selector = variantSelectorCtx.factory({ element: selectorRoot });
            selector._state.attributes['color'] = 'blue';

            selector._updateButtonActiveClass();

            // Check that blue button is selected
            let blueButton = selectorRoot.querySelector("[data-id='blue']");
            assert.isTrue(blueButton.classList.contains('tile__root_selected'));
            assert.isFalse(blueButton.classList.contains('swatch__root_selected'));
        });
    });
});
