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

(function() {
    selectors = {
        self: "[data-comp-is='productcarousel']",
        btnPrev: "[data-carousel-action='prev']",
        btnNext: "[data-carousel-action='next']"
    };

    /**
     *  Product Carousel Component
     */
    class ProductCarousel {
        constructor(rootElement) {
            this._cardsContainer = rootElement.querySelector('.productcarousel__cardscontainer');
            if (!this._cardsContainer) {
                // the carousel is empty
                return;
            }

            // Re-calculate carousel state when the window size changes
            this._calculate = this._calculate.bind(this);
            window.addEventListener('resize', this._calculate);

            this._speed = 300;
            this._delay = 0;
            this._effect = 'linear';
            this._productcarousel_root = rootElement.querySelector('.productcarousel__root');
            this._currentRootWidth = 0;
            this._product_carousel_parent = rootElement.querySelector('.productcarousel__parent');
            this._cards = this._cardsContainer.querySelectorAll('.product__card');
            this._btnPrev = rootElement.querySelector(selectors.btnPrev);
            this._btnNext = rootElement.querySelector(selectors.btnNext);
            this._currentPos = 0;

            this._calculate();

            this._btnPrev.addEventListener('click', e => this._goToPrevProductCard());
            this._btnNext.addEventListener('click', e => this._goToNextProductCard());
        }

        _calculate() {
            // Only re-calculate when one of the screen size breakpoints changes the size of the component
            if (this._productcarousel_root.offsetWidth == this._currentRootWidth) {
                return;
            }

            this._minPos = this._product_carousel_parent.offsetWidth - this._cards.length * this._cards[0].offsetWidth;
            this._cardsContainer.style.width = this._cards[0].offsetWidth * this._cards.length + 'px';
            this._maxPosIndex =
                (this._cardsContainer.offsetWidth - this._product_carousel_parent.offsetWidth) /
                this._cards[0].offsetWidth;

            if (this._minPos >= 0) {
                // Hide buttons if all fit on the screen
                this._btnNext.style.display = 'none';
                this._btnPrev.style.display = 'none';
            } else {
                this._btnNext.style.display = 'block';
                this._btnPrev.style.display = 'block';
                this._btnPrev.disabled = true;
            }

            // Reset carousel to first item
            this._goToProductCard(0, 'reset');
            this._currentRootWidth = this._productcarousel_root.offsetWidth;
        }

        // click event handler for Next Button
        _goToNextProductCard() {
            if (this._btnNext.disabled === false) {
                var newCurrentPos = 0;
                newCurrentPos = this._currentPos + 1;
                this._goToProductCard(newCurrentPos, 'next');
            }
        }

        // Click event handler for Prev Button
        _goToPrevProductCard() {
            if (this._btnPrev.disabled === false) {
                var newCurrentPos = 0;
                newCurrentPos = this._currentPos - 1;
                this._goToProductCard(newCurrentPos, 'prev');
            }
        }

        // create product card and transition
        _goToProductCard(n, dir) {
            var cardwidth = this._cards[0].offsetWidth,
                currentPos = Math.max(-cardwidth * this._currentPos, this._minPos),
                scrollWidth = cardwidth,
                newPos;

            if (dir === 'next') {
                newPos = Math.max(this._minPos, currentPos - scrollWidth);
            } else if (dir === 'prev') {
                newPos = Math.min(0, currentPos + scrollWidth);
            } else {
                newPos = 0;
            }

            this._cardsContainer.style.transition =
                'margin-left ' + this._speed + 'ms' + ' ' + this._effect + ' ' + this._delay + 'ms';
            this._cardsContainer.style.marginLeft = newPos == 0 && this._minPos >= 0 ? 'auto' : newPos + 'px';
            this._currentPos = n;

            this._btnNext.disabled = false;
            this._btnPrev.disabled = false;

            if (this._currentPos >= this._maxPosIndex) {
                this._btnNext.disabled = true;
                this._btnPrev.disabled = false;
            }

            if (this._currentPos <= 0) {
                this._btnPrev.disabled = true;
                this._btnNext.disabled = false;
            }
        }
    }

    function onDocumentReady() {
        const productCmp = document.querySelectorAll(selectors.self);
        if (productCmp) {
            productCmp.forEach(function(element) {
                new ProductCarousel(element);
            });
        }
    }

    if (document.readyState !== 'loading') {
        onDocumentReady();
    } else {
        document.addEventListener('DOMContentLoaded', onDocumentReady);
    }
})();
