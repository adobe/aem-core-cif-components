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
'use strict';

import Carousel from '../../../../src/main/content/jcr_root/apps/core/cif/components/commerce/carousel/v1/carousel/clientlibs/js/carousel';

describe('Carousel', () => {
    let FIRST_ITEM_SELECTOR = '.header__primaryActions aside div.categoryTree__root li:nth-child(1)';
    var body;
    var carouselRoot;

    afterEach(() => {
        body.removeChild(carouselRoot);
    });

    it('initializes the Carousel component with carousel', () => {
        body = window.document.querySelector('body');
        carouselRoot = document.createElement('div');
        body.insertAdjacentElement('afterbegin', carouselRoot);
        carouselRoot.insertAdjacentHTML(
            'afterbegin',
            `
                <div class="carousel aem-GridColumn aem-GridColumn--default--12">
                </div>
            `
        );
        var carousel = new Carousel(carouselRoot);

        assert.isUndefined(carousel._cardsContainer);
        assert.isUndefined(carousel._carousel_root);
        assert.isUndefined(carousel._carousel_parent);
        assert.isUndefined(carousel._cards);
        assert.isUndefined(carousel._currentPos);
        assert.isUndefined(carousel._currentOffset);
        assert.isUndefined(carousel._btnPrev);
        assert.isUndefined(carousel._btnNext);
    });

    it('initializes the Carousel component with no cards', () => {
        body = window.document.querySelector('body');
        carouselRoot = document.createElement('div');
        body.insertAdjacentElement('afterbegin', carouselRoot);
        carouselRoot.insertAdjacentHTML(
            'afterbegin',
            `
                <div class="carousel aem-GridColumn aem-GridColumn--default--12">
                    <div data-comp-is="carousel" class="carousel__container">
                        <h2 class="carousel__title">Shop by category</h2>
                        <button data-carousel-action="prev" class="carousel__btn carousel__btn--prev" type="button" title="Show previous" aria-label="Show previous" style="display: none;"></button>
                        <button data-carousel-action="next" class="carousel__btn carousel__btn--next" type="button" title="Show next" aria-label="Show next" style="display: none;"></button>
                        <div class="carousel__cardsroot" style="width: 300px; overflow: hidden;">
                            <div class="carousel__parent">
                                <div class="carousel__cardscontainer" style="width: 120000px;">
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            `
        );
        var carousel = new Carousel(carouselRoot);

        assert.isNotNull(carousel._cardsContainer);
        assert.isNotNull(carousel._carousel_root);
        assert.isNotNull(carousel._carousel_parent);
        assert.isNotNull(carousel._cards);
        assert.equal(0, carousel._currentPos);
        assert.equal(0, carousel._currentOffset);
        assert.equal(window.getComputedStyle(carousel._btnPrev).display, 'none');
        assert.equal(window.getComputedStyle(carousel._btnNext).display, 'none');
    });

    it('initializes the Carousel component with only a few cards', () => {
        body = window.document.querySelector('body');
        carouselRoot = document.createElement('div');
        body.insertAdjacentElement('afterbegin', carouselRoot);
        carouselRoot.insertAdjacentHTML(
            'afterbegin',
            `
                <div class="carousel aem-GridColumn aem-GridColumn--default--12">
                    <div data-comp-is="carousel" class="carousel__container">
                        <h2 class="carousel__title">Shop by category</h2>
                        <button data-carousel-action="prev" class="carousel__btn carousel__btn--prev" type="button" title="Show previous" aria-label="Show previous" style="display: none;"></button>
                        <button data-carousel-action="next" class="carousel__btn carousel__btn--next" type="button" title="Show next" aria-label="Show next" style="display: none;"></button>
                        <div class="carousel__cardsroot" style="width: 300px; overflow: hidden;">
                            <div class="carousel__parent">
                                <div class="carousel__cardscontainer" style="width: 120000px;">
                                        <div class="card carousel__card" style="float: left; width: 50px; margin-right: 8px; border: 1px solid grey;">
                                            <div>Card 1</div>
                                        </div>
                                        <div class="card carousel__card" style="float: left; width: 50px; margin-right: 8px; border: 1px solid grey;">
                                            <div>Card 2</div>
                                        </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            `
        );

        var carousel = new Carousel(carouselRoot);

        assert.isNotNull(carousel._cardsContainer);
        assert.isNotNull(carousel._carousel_root);
        assert.isNotNull(carousel._carousel_parent);
        assert.isNotNull(carousel._cards);
        assert.equal(0, carousel._currentPos);
        assert.equal(0, carousel._currentOffset);
        assert.equal(window.getComputedStyle(carousel._btnPrev).display, 'none');
        assert.equal(window.getComputedStyle(carousel._btnNext).display, 'none');
    });

    ['ltr', 'rtl'].forEach(dir => {
        describe(`with many cards dir=${dir}`, () => {
            beforeEach(() => {
                const float = dir === 'ltr' ? 'left' : 'right';
                const margin = dir === 'ltr' ? 'margin-right' : 'margin-left';
                // set html dir=${dir}
                window.document.querySelector('html').setAttribute('dir', dir);
                body = window.document.querySelector('body');
                carouselRoot = document.createElement('div');
                body.insertAdjacentElement('afterbegin', carouselRoot);
                carouselRoot.insertAdjacentHTML(
                    'afterbegin',
                    `
                    <div class="carousel aem-GridColumn aem-GridColumn--default--12">
                        <div data-comp-is="carousel" class="carousel__container">
                            <h2 class="carousel__title">Shop by category</h2>
                            <button data-carousel-action="prev" class="carousel__btn carousel__btn--prev" type="button" title="Show previous" aria-label="Show previous" style="display: block;"disabled=""></button>
                            <button data-carousel-action="next" class="carousel__btn carousel__btn--next" type="button" title="Show next" aria-label="Show next" style="display: block;"></button>
                            <div class="carousel__cardsroot" style="width: 300px; overflow: hidden;">
                                <div class="carousel__parent">
                                    <div class="carousel__cardscontainer" style="width: 120000px;">
                                        <div class="card carousel__card" style="float: ${float}; width: 50px; ${margin}: 8px; border: 1px solid grey;">
                                            <div>Card 1</div>
                                        </div>
                                        <div class="card carousel__card" style="float: ${float}; width: 50px; ${margin}: 8px; border: 1px solid grey;">
                                            <div>Card 2</div>
                                        </div>
                                        <div class="card carousel__card" style="float: ${float}; width: 50px; ${margin}: 8px; border: 1px solid grey;">
                                            <div>Card 3</div>
                                        </div>
                                        <div class="card carousel__card" style="float: ${float}; width: 50px; ${margin}: 8px; border: 1px solid grey;">
                                            <div>Card 4</div>
                                        </div>
                                        <div class="card carousel__card" style="float: ${float}; width: 50px; ${margin}: 8px; border: 1px solid grey;">
                                            <div>Card 5</div>
                                        </div>
                                        <div class="card carousel__card" style="float: ${float}; width: 50px; border: 1px solid grey;">
                                            <div>Card 6</div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    `
                );
            });

            it('initializes the Carousel component', () => {
                var carousel = new Carousel(carouselRoot);

                assert.isNotNull(carousel._cardsContainer);
                assert.isNotNull(carousel._carousel_root);
                assert.isNotNull(carousel._carousel_parent);
                assert.isNotNull(carousel._cards);
                assert.equal(0, carousel._currentPos);
                assert.equal(0, carousel._currentOffset);
                assert.equal(window.getComputedStyle(carousel._btnPrev).display, 'block');
                assert.equal(window.getComputedStyle(carousel._btnNext).display, 'block');
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

            it('goes not outside of bounds', async () => {
                var carousel = new Carousel(carouselRoot);

                assert.isTrue(carousel._btnPrev.disabled);
                assert.isFalse(carousel._btnNext.disabled);

                // prev button disabled
                carousel._goToPrevCard();

                assert.equal(0, carousel._currentPos);
                assert.equal(0, carousel._currentOffset);
                assert.isTrue(carousel._btnPrev.disabled);
                assert.isFalse(carousel._btnNext.disabled);

                // forced -1
                carousel._goToCard(-1);

                assert.equal(0, carousel._currentPos);
                assert.equal(0, carousel._currentOffset);
                assert.isTrue(carousel._btnPrev.disabled);
                assert.isFalse(carousel._btnNext.disabled);

                carousel._goToNextCard();

                assert.equal(1, carousel._currentPos);
                assert.isFalse(carousel._btnPrev.disabled);
                assert.isTrue(carousel._btnNext.disabled);

                const oldOffset = carousel._currentOffset;
                // next button disabled
                carousel._goToNextCard();
                assert.equal(oldOffset, carousel._currentOffset);
                assert.equal(1, carousel._currentPos);
                assert.isFalse(carousel._btnPrev.disabled);
                assert.isTrue(carousel._btnNext.disabled);

                // forced +1 (wait for the previous transition to complete)
                await new Promise(resolve => setTimeout(resolve, 300));
                carousel._goToCard(2);
                assert.equal(oldOffset, carousel._currentOffset);
                assert.equal(1, carousel._currentPos);
                assert.isFalse(carousel._btnPrev.disabled);
                assert.isTrue(carousel._btnNext.disabled);
            });
        });
    });
});
