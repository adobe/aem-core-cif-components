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

import CategoryCarousel from '../../../../src/main/content/jcr_root/apps/core/cif/components/commerce/featuredcategorylist/v1/featuredcategorylist/clientlibs/categorycarousel/js/category-carousel';

describe('CategoryCarousel', () => {
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
                <div class="featuredcategorylist aem-GridColumn aem-GridColumn--default--12">
                    <div class="cmp-categorylist">
                        <div class="cmp-categorylist__header">
                            <h2 class="cmp-categorylist__title">Shop by category</h2>
                        </div>
                        <button data-carousel-action='prev' class="cmp-categorylist__btn cmp-categorylist__btn--prev" type="button" style="display: none;"></button>
                        <div class="cmp-categorylist__cardsroot">
                            <div class="cmp-categorylist__parent">
                                <div class="cmp-categorylist__content">
                                    <div role="displaycard" class="card cmp-categorylist__card">
                                        <a class="cmp-categorylist__anchor" href="/content/venia/us/en/products/category-page.11.html">
                                            <span class="cmp-categorylist__imagewrapper" style="background-image: url(/content/dam/core-components-examples/library/adobe-logo.svg/jcr:content/renditions/original);">
                                                <img class="cmp-categorylist__image" src="/content/dam/core-components-examples/library/adobe-logo.svg/_jcr_content/renditions/original" alt="Biking">
                                            </span>
                                            <span class="cmp-categorylist__name">Biking</span>
                                        </a>
                                    </div>
                                    <div role="displaycard" class="card cmp-categorylist__card">
                                        <a class="cmp-categorylist__anchor" href="/content/venia/us/en/products/category-page.10.html">
                                            <span class="cmp-categorylist__imagewrapper" style="background-image: url(/content/dam/core-components-examples/library/github-logo.svg/jcr:content/renditions/original);">
                                                <img class="cmp-categorylist__image" src="/content/dam/core-components-examples/library/github-logo.svg/_jcr_content/renditions/original" alt="Hiking">
                                            </span>
                                            <span class="cmp-categorylist__name">Hiking</span>
                                        </a>
                                    </div>
                                    <div role="displaycard" class="card cmp-categorylist__card">
                                        <a class="cmp-categorylist__anchor" href="/content/venia/us/en/products/category-page.6.html">
                                            <span class="cmp-categorylist__imagewrapper" style="background-image: url(/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/camp-tent.jpg/jcr:content/renditions/original);">
                                                <img class="cmp-categorylist__image" src="/content/dam/we-retail/en/experiences/arctic-surfing-in-lofoten/camp-tent.jpg/_jcr_content/renditions/original" alt="Running">
                                            </span>
                                            <span class="cmp-categorylist__name">Running</span>
                                        </a>
                                    </div>
                                    <div role="displaycard" class="card cmp-categorylist__card">
                                        <a class="cmp-categorylist__anchor" href="/content/venia/us/en/products/category-page.12.html">
                                            <span class="cmp-categorylist__imagewrapper"></span>
                                            <span class="cmp-categorylist__name">Surfing</span>
                                        </a>
                                    </div>
                                    <div role="displaycard" class="card cmp-categorylist__card">
                                        <a class="cmp-categorylist__anchor" href="/content/venia/us/en/products/category-page.13.html">
                                            <span class="cmp-categorylist__imagewrapper"></span>
                                            <span class="cmp-categorylist__name">Water Sports</span>
                                        </a>
                                    </div>
                                    <div role="displaycard" class="card cmp-categorylist__card">
                                        <a class="cmp-categorylist__anchor" href="/content/venia/us/en/products/category-page.14.html">
                                            <span class="cmp-categorylist__imagewrapper"></span>
                                            <span class="cmp-categorylist__name">Winter Sport</span>
                                        </a>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <button data-carousel-action='next' class="cmp-categorylist__btn cmp-categorylist__btn--next" type="button" style="display: none;"></button>
                    </div>
                </div>
                `
        );
    });

    after(() => {
        body.removeChild(carouselRoot);
    });

    it('initializes the CategoryCarousel component', () => {
        var carousel = new CategoryCarousel(carouselRoot);

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
        var carousel = new CategoryCarousel(carouselRoot);

        assert.isTrue(carousel._btnPrev.disabled);
        assert.isFalse(carousel._btnNext.disabled);

        carousel._goToNextCard();
        assert.equal(1, carousel._currentPos);

        assert.isFalse(carousel._btnPrev.disabled);
        assert.isTrue(carousel._btnNext.disabled);
    });

    it('goes to the previous card', () => {
        var carousel = new CategoryCarousel(carouselRoot);

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
