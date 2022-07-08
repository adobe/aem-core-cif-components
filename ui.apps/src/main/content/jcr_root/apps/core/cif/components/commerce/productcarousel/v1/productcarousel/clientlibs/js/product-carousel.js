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

import Carousel from '../../../../../carousel/v1/carousel/clientlibs/js/carousel';

class ProductCarousel extends Carousel {
    static prices$ = null;

    static selectors = {
        self: "[data-comp-is='productcarousel']",
        btnPrev: "[data-carousel-action='prev']",
        btnNext: "[data-carousel-action='next']",
        container: '.productcarousel__cardscontainer',
        root: '.productcarousel__root',
        parent: '.productcarousel__parent',
        card: '.product__card',
        price: '.price'
    };

    constructor(element, allBaseSkus, queryVariants) {
        super(element, ProductCarousel.selectors);
        this.element = element;
        this.loadPrices = window.CIF.enableClientSidePriceLoading;
        this._formatter = new window.CIF.PriceFormatter();

        this.loadPrices && this._fetchPrices(allBaseSkus || [], queryVariants);
    }

    async _fetchPrices(allBaseSkus, queryVariants) {
        if (!ProductCarousel.prices$) {
            if (window.CIF.CommerceGraphqlApi) {
                ProductCarousel.prices$ = window.CIF.CommerceGraphqlApi.getProductPriceModels(
                    allBaseSkus,
                    queryVariants
                );
            } else {
                ProductCarousel.prices$ = Promise.reject(new Error('CommerceGraphqlApi unavailable'));
            }
        }

        // await all prices to be loaded and update
        this._updatePrices(await ProductCarousel.prices$);
    }

    _updatePrices(prices) {
        this.element.querySelectorAll(ProductCarousel.selectors.card).forEach(card => {
            const sku = card.dataset.productSku;
            if (!(sku in prices)) return;
            const price = prices[sku];

            // Only update if prices are available and not null
            if (!price || !price.regularPrice || !price.finalPrice) {
                return;
            }

            const innerHTML = this._formatter.formatPriceAsHtml(price, { showStartingAt: true });
            card.querySelector(ProductCarousel.selectors.price).innerHTML = innerHTML;
        });
    }
}

function onDocumentReady(document) {
    const rootElements = [...document.querySelectorAll(ProductCarousel.selectors.self)];
    const baseSkus = [];
    let queryVariants = false;
    for (let element of rootElements) {
        const cards = element.querySelectorAll(ProductCarousel.selectors.card);
        for (let card of cards) {
            let { productBaseSku, productSku } = card.dataset;
            if (!productBaseSku) {
                productBaseSku = productSku;
            }
            queryVariants = queryVariants || productBaseSku !== productSku;
            baseSkus.push(productBaseSku);
        }
    }

    rootElements.forEach(element => new ProductCarousel(element, baseSkus, queryVariants));
}

export { ProductCarousel, onDocumentReady };
export default ProductCarousel;

(function(document) {
    if (window.CIF) {
        onDocumentReady(document);
    } else {
        document.addEventListener('aem.cif.clientlib-initialized', () => onDocumentReady(document));
    }
})(window.document);
