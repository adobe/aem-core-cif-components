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

/**
 *  Carousel Component.
 */
class Carousel {
    constructor(rootElement, selectors = Carousel.selectors) {
        const cardsContainer = rootElement.querySelector(selectors.container);
        if (!cardsContainer) {
            // the carousel is empty
            return;
        }

        this._cardsContainer = cardsContainer;

        // Re-calculate carousel state when the window size changes
        this._calculate = this._calculate.bind(this);
        window.addEventListener('resize', this._calculate);

        this._currentPos = 0;
        this._currentOffset = 0;
        this._currentRootWidth = 0;
        this._carousel_root = rootElement.querySelector(selectors.root);
        this._carousel_parent = rootElement.querySelector(selectors.parent);
        this._cards = cardsContainer.querySelectorAll(selectors.card);
        this._btnPrev = rootElement.querySelector(selectors.btnPrev);
        this._btnNext = rootElement.querySelector(selectors.btnNext);
        this._direction = getComputedStyle(cardsContainer).direction;

        this._calculate();

        this._btnPrev.addEventListener('click', e => this._goToPrevCard());
        this._btnNext.addEventListener('click', e => this._goToNextCard());
    }

    _calculate() {
        // Only re-calculate when one of the screen size breakpoints changes the size of the component
        if (this._cards.length === 0 || this._carousel_root.offsetWidth == this._currentRootWidth) {
            return;
        }

        const lastCard = this._cards[this._cards.length - 1];
        const lastCardBB = lastCard.getBoundingClientRect();
        const lastCardStyle = getComputedStyle(lastCard);
        const firstCard = this._cards[0];
        const firstCardBB = firstCard.getBoundingClientRect();
        const firstCardStyle = getComputedStyle(firstCard);
        const contentWidth =
            this._direction === 'ltr'
                ? lastCardBB.right +
                  parseInt(lastCardStyle.marginRight) -
                  (firstCardBB.left + parseInt(firstCardStyle.marginLeft))
                : firstCardBB.right +
                  parseInt(firstCardStyle.marginRight) -
                  (lastCardBB.left + parseInt(lastCardStyle.marginLeft));

        if (this._carousel_parent.offsetWidth >= contentWidth) {
            // Hide buttons if all fit on the screen
            this._btnNext.style.display = 'none';
            this._btnPrev.style.display = 'none';
        } else {
            this._btnNext.style.display = 'block';
            this._btnPrev.style.display = 'block';
        }

        // Reset carousel to first item
        this._goToCard(0);
        this._currentRootWidth = this._carousel_root.offsetWidth;
    }

    // click event handler for Next Button
    _goToNextCard() {
        if (this._btnNext.disabled === false) {
            this._goToCard(this._currentPos + 1);
        }
    }

    // Click event handler for Prev Button
    _goToPrevCard() {
        if (this._btnPrev.disabled === false) {
            this._goToCard(this._currentPos - 1);
        }
    }

    // create card and transition
    _goToCard(nextPos) {
        if (nextPos < 0) {
            // index out of bounds
            return;
        }

        const lastCard = this._cards[this._cards.length - 1];
        const lastCardBB = lastCard.getBoundingClientRect();
        const carouselParentBB = this._carousel_parent.getBoundingClientRect();

        if (
            nextPos > this._currentPos &&
            ((this._direction === 'ltr' && lastCardBB.right <= carouselParentBB.right) ||
                (this._direction === 'rtl' && lastCardBB.left >= carouselParentBB.left))
        ) {
            return;
        }

        const firstCard = this._cards[0];
        const firstCardBB = firstCard.getBoundingClientRect();
        const currentCard = this._cards[this._currentPos];
        const currentCardBB = currentCard.getBoundingClientRect();
        const targetCard = this._cards[nextPos];
        const targetCardBB = targetCard.getBoundingClientRect();

        // difference that needs to be added on the margin-left
        let offsetDiff;
        let newOffset;
        let diffCarouselToLast;
        let diffCarouselToFirst;

        if (this._direction === 'rtl') {
            offsetDiff = currentCardBB.right - targetCardBB.right;
            diffCarouselToLast = carouselParentBB.left - lastCardBB.left;
            diffCarouselToFirst = carouselParentBB.right - firstCardBB.right;
        } else {
            offsetDiff = currentCardBB.left - targetCardBB.left;
            diffCarouselToLast = carouselParentBB.right - lastCardBB.right;
            diffCarouselToFirst = carouselParentBB.left - firstCardBB.left;
        }

        if (nextPos > this._currentPos && Math.abs(diffCarouselToLast) < Math.abs(offsetDiff)) {
            // navigating forward to the last card (fractional)
            offsetDiff = diffCarouselToLast;
        } else if (nextPos < this._currentPos && Math.abs(diffCarouselToFirst) < Math.abs(offsetDiff)) {
            // navigating backward to the second-last card (fractional)
            offsetDiff = diffCarouselToFirst;
        }

        if (this._direction === 'ltr') {
            newOffset = this._currentOffset + offsetDiff;
            this._cardsContainer.style.marginLeft = newOffset + 'px';
        } else {
            newOffset = this._currentOffset - offsetDiff;
            this._cardsContainer.style.marginRight = newOffset + 'px';
        }

        this._currentPos = nextPos;
        this._currentOffset = newOffset;

        // disable _btnNext when the last card is in the carousel parent
        if (this._direction === 'ltr') {
            this._btnNext.disabled = lastCardBB.right <= carouselParentBB.right - offsetDiff;
        } else {
            this._btnNext.disabled = lastCardBB.left >= carouselParentBB.left - offsetDiff;
        }
        // disable _btnPrev when the we are at the first card
        this._btnPrev.disabled = this._currentPos == 0;
    }
}

Carousel.selectors = {
    self: '.carousel__container',
    btnPrev: "[data-carousel-action='prev']",
    btnNext: "[data-carousel-action='next']",
    container: '.carousel__cardscontainer',
    root: '.carousel__cardsroot',
    parent: '.carousel__parent',
    card: '.carousel__card'
};

export default Carousel;
