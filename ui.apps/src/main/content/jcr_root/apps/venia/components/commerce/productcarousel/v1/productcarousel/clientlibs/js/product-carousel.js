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

  /**
   *  Product Carousel Component  
   */
  class ProductCarousel {

    // Initialise the variables
    static selectors = {

      self: "[data-comp-is=productcarousel]",
      btnPrev: "[data-carousel-action=prev]",
      btnNext: "[data-carousel-action=next]"

    }

    constructor(rootElement) {
      this._speed = 300;
      this._delay = 0;
      this._effect = 'linear';
      this._product_carousel_parent = rootElement.querySelector('.product-carousel-parent');
      this._cardsContainer = rootElement.querySelector('.cards-container');
      this._cards = this._cardsContainer.querySelectorAll('.product-card');
      this._btnPrev = rootElement.querySelector(ProductCarousel.selectors.btnPrev);
      this._btnNext = rootElement.querySelector(ProductCarousel.selectors.btnNext);
      this._currentPos = 0;

      this._minPos = (this._product_carousel_parent.offsetWidth - (this._cards.length * this._cards[0].offsetWidth));

      this._maxPosIndex = ((this._cardsContainer.offsetWidth - this._product_carousel_parent.offsetWidth) / this._cards[0].offsetWidth)
      this._cardsContainer.style.marginLeft = '0px';
      this._cardsContainer.style.width = (this._cards[0].offsetWidth * this._cards.length) + 'px';

      this._btnPrev.addEventListener("click", e => this._goToPrevProductCard());
      this._btnNext.addEventListener("click", e => this._goToNextProductCard());

      if (this._minPos >= 0) {
        this._btnNext.disabled = true;
        this._btnPrev.disabled = true;

      } else {
        this._btnPrev.disabled = true;
      }
    }

    // click event handler for Next Button
    _goToNextProductCard() {

      if (this._btnNext.disabled === false) {
        var newCurrentPos = 0;
        newCurrentPos = this._currentPos + 1;
        this._goToProductCard(newCurrentPos, 'next');
      }
    };

    // Click event handler for Prev Button  
    _goToPrevProductCard() {

      if (this._btnPrev.disabled === false) {
        var newCurrentPos = 0;
        newCurrentPos = this._currentPos - 1;
        this._goToProductCard(newCurrentPos, 'prev');
      }
    };


    // create product card and transition
    _goToProductCard(n, dir) {

      var cardwidth = this._cards[0].offsetWidth,
        currentPos = Math.max(-cardwidth * this._currentPos, this._minPos),
        scrollWidth = cardwidth,
        newPos;

      newPos = dir === 'next' ? Math.max(this._minPos, currentPos - scrollWidth) : Math.min(0, currentPos + scrollWidth);
      this._cardsContainer.style.transition = 'margin-left ' + this._speed + 'ms' + ' ' + this._effect + ' ' + this._delay + 'ms';
      this._cardsContainer.style.marginLeft = newPos + 'px';
      this._currentPos = n;

      this._btnNext.disabled = false;
      this._btnPrev.disabled = false;


      if (this._currentPos === this._maxPosIndex) {
        this._btnNext.disabled = true;
        this._btnPrev.disabled = false;
      }

      if (this._currentPos === 0) {
        this._btnPrev.disabled = true;
        this._btnNext.disabled = false;
      }
    }; // end of function

  } // end of class


  function onDocumentReady() {

    const productCmp = document.querySelectorAll(ProductCarousel.selectors.self);
    if (productCmp) {

      productCmp.forEach(function(element) {
        new ProductCarousel(element);
      });

    }
  }


  if (document.readyState !== "loading") {
    onDocumentReady()
  } else {
    document.addEventListener("DOMContentLoaded", onDocumentReady);
  }

})();