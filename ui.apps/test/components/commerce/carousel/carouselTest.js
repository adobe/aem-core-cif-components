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
'use strict';

import Carousel from '../../../../src/main/content/jcr_root/apps/core/cif/components/commerce/carousel/v1/carousel/clientlibs/js/carousel';

describe('Carousel', () => {
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
                <div class="carousel aem-GridColumn aem-GridColumn--default--12">
                    <div data-comp-is="carousel" class="carousel__container">
                        <h2 class="carousel__title">Shop by category</h2>
                        <button data-carousel-action="prev" class="carousel__btn carousel__btn--prev" type="button" style="display: block;"disabled=""></button>
                        <div class="carousel__cardsroot">
                            <div class="carousel__parent">
                                <div class="carousel__cardscontainer" style="width: 1440px; transition: margin-left 300ms linear 0ms; margin-left: 0px;">
                                    <div role="displaycard" class="card carousel__card">
                                        <div>Card 1</div>
                                    </div>
                                    <div role="displaycard" class="card carousel__card">
                                        <div>Card 2</div>
                                    </div>
                                    <div role="displaycard" class="card carousel__card">
                                        <div>Card 3</div>
                                    </div>
                                    <div role="displaycard" class="card carousel__card">
                                        <div>Card 4</div>
                                    </div>
                                    <div role="displaycard" class="card carousel__card">
                                        <div>Card 5</div>
                                    </div>
                                    <div role="displaycard" class="card carousel__card">
                                        <div>Card 6</div>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <button data-carousel-action="next" class="carousel__btn carousel__btn--next" type="button" style="display: block;"></button>
                    </div>
                </div>
                `
        );
    });

    after(() => {
        body.removeChild(carouselRoot);
    });

    it('initializes the Carousel component', () => {
        var carousel = new Carousel(carouselRoot);

        assert.isNotNull(carousel._cardsContainer);
        assert.isNotNull(carousel._carousel_root);
        assert.isNotNull(carousel._carousel_parent);
        assert.isNotNull(carousel._cards);
        assert.isNotNull(carousel._currentPos);
        assert.equal(0, carousel._currentPos);

        assert.isTrue(carousel._btnPrev.disabled);
        assert.isFalse(carousel._btnNext.disabled);
    });

    it('goes to the next card', () => {
        var carousel = new Carousel(carouselRoot);

        assert.isTrue(carousel._btnPrev.disabled);
        assert.isFalse(carousel._btnNext.disabled);

        carousel._goToNextCard();
        assert.equal(1, carousel._currentPos);

        assert.isFalse(carousel._btnPrev.disabled);
        assert.isTrue(carousel._btnNext.disabled);
    });

    it('goes to the previous card', () => {
        var carousel = new Carousel(carouselRoot);

        assert.isTrue(carousel._btnPrev.disabled);
        assert.isFalse(carousel._btnNext.disabled);

        carousel._goToNextCard();
        assert.equal(1, carousel._currentPos);

        assert.isFalse(carousel._btnPrev.disabled);
        assert.isTrue(carousel._btnNext.disabled);

        carousel._goToPrevCard();
        assert.equal(0, carousel._currentPos);

        assert.isTrue(carousel._btnPrev.disabled);
        assert.isFalse(carousel._btnNext.disabled);
    });
});
