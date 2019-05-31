'use strict';

describe('Product', () => {

    let productRoot;

    beforeEach(() => {
        let sku = document.createElement('span');
        sku.setAttribute('role', 'sku');

        let details = document.createElement('div');
        details.classList.add('productFullDetail__details');
        details.appendChild(sku);
        
        productRoot = document.createElement('div');
        productRoot.appendChild(details);
    });

    it('initializes a configurable product component', () => {
        productRoot.dataset.configurable = true;

        let product = productCtx.factory({ element: productRoot });
        assert.isTrue(product._state.configurable);
        assert.isNull(product._state.sku);
    });

    it('initializes a simple product component', () => {
        productRoot.querySelector(productCtx.Product.selectors.sku).innerHTML = 'sample-sku';

        let product = productCtx.factory({ element: productRoot });
        assert.isFalse(product._state.configurable);
        assert.equal(product._state.sku, 'sample-sku');
    });

    it.skip('changes variant when receiving variantchanged event', () => {
        /* let root = document.createElement('div');
        let product = new Product({ element: root });

        console.log("Hello world, I'm a test.");
        console.log(product); */
    });


});