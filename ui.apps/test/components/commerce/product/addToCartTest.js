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

import AddToCart from '../../../../src/main/content/jcr_root/apps/core/cif/components/commerce/product/v1/product/clientlib/js/addToCart.js';

describe('Product', () => {
    describe('AddToCart', () => {
        let productRoot;
        let addToCartRoot;
        let pageRoot;

        before(() => {
            let body = document.querySelector('body');
            pageRoot = document.createElement('div');
            body.appendChild(pageRoot);
        });

        after(() => {
            pageRoot.parentNode.removeChild(pageRoot);
        });

        beforeEach(() => {
            while (pageRoot.firstChild) {
                pageRoot.removeChild(pageRoot.firstChild);
            }
            pageRoot.insertAdjacentHTML(
                'afterbegin',
                `<div data-cmp-is="product">
                    <div class="productFullDetail__details">
                        <span role="sku">my-sample-sku</span>
                    </div>
                    <div class="productFullDetail__cartActions">
                        <button>
                    </div>
                    <div class="productFullDetail__quantity">
                        <select data-product-sku="my-sample-sku">
                            <option value="5" selected></option>
                        </select>
                    </div>
                </div>`
            );

            addToCartRoot = pageRoot.querySelector(AddToCart.selectors.self);
            productRoot = pageRoot.querySelector(AddToCart.selectors.product);
        });

        it('initializes an AddToCart component for a configurable product', () => {
            productRoot.dataset.configurable = true;

            let addToCart = new AddToCart({ element: addToCartRoot, product: productRoot });

            assert.isTrue(addToCart._state.configurable);
            assert.isNull(addToCart._state.sku);
            assert.isTrue(addToCartRoot.disabled);
        });

        it('initializes an AddToCart component for a simple product', () => {
            let addToCart = new AddToCart({ element: addToCartRoot, product: productRoot });

            assert.isFalse(addToCart._state.configurable);
            assert.equal(addToCart._state.sku, 'my-sample-sku');
            assert.isFalse(addToCartRoot.disabled);
        });

        it('is disabled on invalid variant', () => {
            productRoot.dataset.configurable = true;
            let addToCart = new AddToCart({ element: addToCartRoot, product: productRoot });

            // Send event
            let changeEvent = new CustomEvent(AddToCart.events.variantChanged, {
                bubbles: true,
                detail: {}
            });
            productRoot.dispatchEvent(changeEvent);

            assert.isTrue(addToCartRoot.disabled);
        });

        it('reacts to a variantchanged event', () => {
            productRoot.dataset.configurable = true;
            let addToCart = new AddToCart({ element: addToCartRoot, product: productRoot });

            // Send event
            let changeEvent = new CustomEvent(AddToCart.events.variantChanged, {
                bubbles: true,
                detail: {
                    variant: {
                        sku: 'variant-sku'
                    },
                    attributes: {
                        color: 'red'
                    }
                }
            });
            productRoot.dispatchEvent(changeEvent);

            assert.equal(addToCart._state.sku, 'variant-sku');
            assert.deepEqual(addToCart._state.attributes, { color: 'red' });
            assert.isFalse(addToCartRoot.disabled);
        });

        it('dispatches an event on click', () => {
            let spy = sinon.spy();
            let _originalDispatch = document.dispatchEvent;
            document.dispatchEvent = spy;
            let addToCart = new AddToCart({ element: addToCartRoot, product: productRoot });
            addToCartRoot.click();
            sinon.assert.calledOnce(spy);
            assert.equal(spy.getCall(0).args[0].type, 'aem.cif.add-to-cart');
            assert.equal(spy.getCall(0).args[0].detail[0].sku, addToCart._state.sku);
            assert.equal(spy.getCall(0).args[0].detail[0].quantity, 5);
            assert.isFalse(spy.getCall(0).args[0].detail[0].virtual);
            document.dispatchEvent = _originalDispatch;
        });

        it('dispatches a virtual add to cart event', () => {
            while (pageRoot.firstChild) {
                pageRoot.removeChild(pageRoot.firstChild);
            }
            pageRoot.insertAdjacentHTML(
                'afterbegin',
                `<div data-cmp-is="product" data-virtual>
                    <div class="productFullDetail__details">
                        <span role="sku">my-sample-sku</span>
                    </div>
                    <div class="productFullDetail__cartActions">
                        <button>
                    </div>
                    <div class="productFullDetail__quantity">
                        <select data-product-sku="my-sample-sku">
                            <option value="4" selected></option>
                        </select>
                    </div>
                </div>`
            );

            addToCartRoot = pageRoot.querySelector(AddToCart.selectors.self);
            productRoot = pageRoot.querySelector(AddToCart.selectors.product);

            let spy = sinon.spy();
            let _originalDispatch = document.dispatchEvent;
            document.dispatchEvent = spy;
            new AddToCart({ element: addToCartRoot, product: productRoot });
            addToCartRoot.click();
            sinon.assert.calledOnce(spy);
            assert.equal(spy.getCall(0).args[0].type, 'aem.cif.add-to-cart');
            assert.equal(spy.getCall(0).args[0].detail[0].quantity, 4);
            assert.isTrue(spy.getCall(0).args[0].detail[0].virtual);
            document.dispatchEvent = _originalDispatch;
        });

        it('dispatches a virtual giftcard add to cart event', () => {
            while (pageRoot.firstChild) {
                pageRoot.removeChild(pageRoot.firstChild);
            }
            pageRoot.insertAdjacentHTML(
                'afterbegin',
                `<div data-cmp-is="product" data-giftcard>
                <div class="productFullDetail__details">
                   <span role="sku">my-sample-sku</span>
                </div>
                <div class="productFullDetail__cartActions">
                   <button>
                </div>
                <div class="productFullDetail__quantity">
                   <select data-product-sku="my-sample-sku">
                      <option value="3" selected></option>
                   </select>
                   <fieldset>
                      <div id="giftcard-amount-box">
                         <label>
                            <span>Amount</span>
                            <input type="hidden" id="giftcard_amount_input_uid" name="giftcard_amount_input_uid" value="Z2lmdGNhcmQvY3VzdG9tX2dpZnRjYXJkX2Ftb3VudA==">
                            <input type="hidden" id="giftcard_amount_input_min" name="giftcard_amount_input_min" value="$10.0">
                            <input type="hidden" id="giftcard_amount_input_max" name="giftcard_amount_input_max" value="$20.0">
                         </label>
                         <div class="control">
                            <input type="text" id="giftcard_amount_input" name="giftcard_amount_input" value="14.0">
                         </div>
                      </div>
                   </fieldset>
                   <div data-giftcardtype="VIRTUAL" id="giftcard_form">
                      <fieldset>
                         <div>
                            <label>
                                <span>Sender Name</span>
                                <input type="hidden" id="giftcard_sender_name_uid" name="giftcard_sender_name_uid" value="Z2lmdGNhcmQvZ2lmdGNhcmRfc2VuZGVyX25hbWU=">
                            </label>
                            <div>
                               <input type="text" id="giftcard_sender_name" name="giftcard_sender_name" value="Tester 103">
                            </div>
                         </div>
                         <div>
                           <label>
                           <span>Sender Email</span>
                           <input type="hidden" id="giftcard_sender_email_uid" name="giftcard_sender_email_uid" value="Z2lmdGNhcmQvZ2lmdGNhcmRfc2VuZGVyX2VtYWls">
                           </label>
                           <div>
                              <input type="email" id="giftcard_sender_email" name="giftcard_sender_email" value="test@example.com">
                           </div>
                         </div>
                         <div>
                            <label>
                               <span>Recipient Name</span>
                               <input type="hidden" id="giftcard_recipient_name_uid" name="giftcard_recipient_name_uid" value="Z2lmdGNhcmQvZ2lmdGNhcmRfcmVjaXBpZW50X25hbWU=">
                            </label>
                            <div>
                               <input type="text" id="giftcard_recipient_name" name="giftcard_recipient_name" value="Tester 104">
                            </div>
                         </div>
                         <div>
                           <label>
                           <span>Recipient Email</span>
                           <input type="hidden" id="giftcard_recipient_email_uid" name="giftcard_recipient_email_uid" value="Z2lmdGNhcmQvZ2lmdGNhcmRfcmVjaXBpZW50X2VtYWls">
                           </label>
                           <div>
                              <input type="email" id="giftcard_recipient_email" name="giftcard_recipient_email" value="test@example.com">
                           </div>
                         </div>
                         <div>
                            <label>
                                <span>Message</span>
                                <input type="hidden" id="giftcard_message_uid" name="giftcard_message_uid" value="Z2lmdGNhcmQvZ2lmdGNhcmRfbWVzc2FnZQ==">
                            </label>
                            <div>
                               <textarea name="giftcard_message" id="giftcard_message"></textarea>
                            </div>
                         </div>
                      </fieldset>
                   </div>
                </div>
             </div>`
            );

            addToCartRoot = pageRoot.querySelector(AddToCart.selectors.self);
            productRoot = pageRoot.querySelector(AddToCart.selectors.product);

            let spy = sinon.spy();
            let _originalDispatch = document.dispatchEvent;
            document.dispatchEvent = spy;
            new AddToCart({ element: addToCartRoot, product: productRoot });
            addToCartRoot.click();
            sinon.assert.calledOnce(spy);
            assert.equal(spy.getCall(0).args[0].type, 'aem.cif.add-to-cart');
            assert.equal(spy.getCall(0).args[0].detail[0].quantity, 3);
            assert.isTrue(spy.getCall(0).args[0].detail[0].giftcard.is_giftcard);
            assert.equal(spy.getCall(0).args[0].detail[0].giftcard.type, 'VIRTUAL');
            assert.equal(spy.getCall(0).args[0].detail[0].giftcard.entered_options.length, 6);
            document.dispatchEvent = _originalDispatch;
        });

        it('dispatches a combined giftcard add to cart event', () => {
            while (pageRoot.firstChild) {
                pageRoot.removeChild(pageRoot.firstChild);
            }
            pageRoot.insertAdjacentHTML(
                'afterbegin',
                `<div data-cmp-is="product" data-giftcard>
                <div class="productFullDetail__details">
                   <span role="sku">my-sample-sku</span>
                </div>
                <div class="productFullDetail__cartActions">
                   <button>
                </div>
                <div class="productFullDetail__quantity">
                   <select data-product-sku="my-sample-sku">
                      <option value="3" selected></option>
                   </select>
                   <fieldset>
                      <div id="giftcard-amount-box">
                         <label>
                            <span>Amount</span>
                            <input type="hidden" id="giftcard_amount_input_uid" name="giftcard_amount_input_uid" value="Z2lmdGNhcmQvY3VzdG9tX2dpZnRjYXJkX2Ftb3VudA==">
                            <input type="hidden" id="giftcard_amount_input_min" name="giftcard_amount_input_min" value="$10.0">
                            <input type="hidden" id="giftcard_amount_input_max" name="giftcard_amount_input_max" value="$20.0">
                         </label>
                         <div class="control">
                            <input type="text" id="giftcard_amount_input" name="giftcard_amount_input" value="14.0">
                         </div>
                      </div>
                   </fieldset>
                   <div data-giftcardtype="COMBINED" id="giftcard_form">
                      <fieldset>
                         <div>
                            <label>
                                <span>Sender Name</span>
                                <input type="hidden" id="giftcard_sender_name_uid" name="giftcard_sender_name_uid" value="Z2lmdGNhcmQvZ2lmdGNhcmRfc2VuZGVyX25hbWU=">
                            </label>
                            <div>
                               <input type="text" id="giftcard_sender_name" name="giftcard_sender_name" value="Tester 103">
                            </div>
                         </div>
                         <div>
                           <label>
                           <span>Sender Email</span>
                           <input type="hidden" id="giftcard_sender_email_uid" name="giftcard_sender_email_uid" value="Z2lmdGNhcmQvZ2lmdGNhcmRfc2VuZGVyX2VtYWls">
                           </label>
                           <div>
                              <input type="email" id="giftcard_sender_email" name="giftcard_sender_email" value="test@example.com">
                           </div>
                         </div>
                         <div>
                            <label>
                               <span>Recipient Name</span>
                               <input type="hidden" id="giftcard_recipient_name_uid" name="giftcard_recipient_name_uid" value="Z2lmdGNhcmQvZ2lmdGNhcmRfcmVjaXBpZW50X25hbWU=">
                            </label>
                            <div>
                               <input type="text" id="giftcard_recipient_name" name="giftcard_recipient_name" value="Tester 104">
                            </div>
                         </div>
                         <div>
                           <label>
                           <span>Recipient Email</span>
                           <input type="hidden" id="giftcard_recipient_email_uid" name="giftcard_recipient_email_uid" value="Z2lmdGNhcmQvZ2lmdGNhcmRfcmVjaXBpZW50X2VtYWls">
                           </label>
                           <div>
                              <input type="email" id="giftcard_recipient_email" name="giftcard_recipient_email" value="test@example.com">
                           </div>
                         </div>
                         <div>
                            <label>
                                <span>Message</span>
                                <input type="hidden" id="giftcard_message_uid" name="giftcard_message_uid" value="Z2lmdGNhcmQvZ2lmdGNhcmRfbWVzc2FnZQ==">
                            </label>
                            <div>
                               <textarea name="giftcard_message" id="giftcard_message"></textarea>
                            </div>
                         </div>
                      </fieldset>
                   </div>
                </div>
             </div>`
            );

            addToCartRoot = pageRoot.querySelector(AddToCart.selectors.self);
            productRoot = pageRoot.querySelector(AddToCart.selectors.product);

            let spy = sinon.spy();
            let _originalDispatch = document.dispatchEvent;
            document.dispatchEvent = spy;
            new AddToCart({ element: addToCartRoot, product: productRoot });
            addToCartRoot.click();
            sinon.assert.calledOnce(spy);
            assert.equal(spy.getCall(0).args[0].type, 'aem.cif.add-to-cart');
            assert.equal(spy.getCall(0).args[0].detail[0].quantity, 3);
            assert.isTrue(spy.getCall(0).args[0].detail[0].giftcard.is_giftcard);
            assert.equal(spy.getCall(0).args[0].detail[0].giftcard.type, 'COMBINED');
            assert.equal(spy.getCall(0).args[0].detail[0].giftcard.entered_options.length, 6);
            document.dispatchEvent = _originalDispatch;
        });
    });
});
