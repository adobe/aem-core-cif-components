/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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

import ProductCarouselActions from '../../../../src/main/content/jcr_root/apps/core/cif/components/commerce/productcarousel/v1/productcarousel/clientlibs/js/actions';

describe('ProductCarousel', () => {
    let FIRST_ITEM_SELECTOR = '.header__primaryActions aside div.categoryTree__root li:nth-child(1)';
    var body;
    var carouselRoot;

    before(() => {
        body = window.document.querySelector('body');
        carouselRoot = document.createElement('div');
        body.insertAdjacentElement('afterbegin', carouselRoot);
        carouselRoot.insertAdjacentHTML(
            'afterbegin',
            `
                <div class="productcarousel">
                    <div data-comp-is="productcarousel" class="productcarousel__container">
                        <div class="productcarousel__cardsroot">
                            <div class="productcarousel__parent">
                                <div class="productcarousel__cardscontainer">                                    
                                    <div class="card product__card">
                                        <a class="product-card-content">
                                            <div class="product__card-title">Card 1</div>
                                            <div class="product__card-actions">
                                                <button data-action="add-to-cart" data-item-sku="sku-1" class="product__card-button product__card-button--add-to-cart"/>
                                                <button data-item-sku="sku-1" class="product__card-button product__card-button--add-to-wish-list"/>
                                            </div>
                                        </a>                                        
                                    </div>
                                    <div class="card product__card">
                                        <a class="product-card-content">
                                            <div class="product__card-title">Card 2</div>
                                            <div class="product__card-actions">
                                                <button data-action="details" data-item-sku="sku-2" class="product__card-button product__card-button--add-to-cart"/>                                                
                                            </div>
                                        </a>
                                    </div>                                    
                                </div>                                
                            </div>
                        </div>
                    </div>
                </div>
                `
        );
    });

    after(() => {
        body.removeChild(carouselRoot);
    });

    it('triggers the add-to-cart event for the Add to Cart button add-to-cart call to action', () => {
        new ProductCarouselActions(carouselRoot);

        const spy = sinon.spy();
        document.addEventListener('aem.cif.add-to-cart', spy);
        const button = carouselRoot.querySelector('button.product__card-button--add-to-cart');

        button.click();

        assert.isTrue(spy.called);
    });

    it('propagates the click to the parent link for the Add to Cart button details call to action', () => {
        new ProductCarouselActions(carouselRoot);

        const spy = sinon.spy();
        document.addEventListener('aem.cif.add-to-cart', spy);
        const spyLink = sinon.spy();
        const button = carouselRoot.querySelector('button.product__card-button--add-to-cart[data-action="details"]');
        const link = button.closest('a');
        link.addEventListener('click', spyLink);

        button.click();

        assert.isFalse(spy.called);
        assert.isTrue(spyLink.called);
    });

    it('triggers the add-to-wish-list event for the Add to Wish List button', () => {
        new ProductCarouselActions(carouselRoot);

        const spy = sinon.spy();
        document.addEventListener('aem.cif.add-to-wishlist', spy);
        const button = carouselRoot.querySelector('button.product__card-button--add-to-wish-list');

        button.click();

        assert.isTrue(spy.called);
    });
});
