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

/**
 * Add to cart button component.
 */
class AddToCart {
    constructor(config) {
        this._element = config.element;
        // Get configuration from product reference
        let configurable = config.product.dataset.configurable !== undefined;
        let virtual = config.product.dataset.virtual !== undefined;
        let grouped = config.product.dataset.grouped !== undefined;
        let giftcard = config.product.dataset.giftcard !== undefined;
        let sku = !configurable ? config.product.querySelector(AddToCart.selectors.sku).innerHTML : null;

        this._state = {
            sku,
            attributes: {},
            configurable,
            virtual,
            grouped,
            giftcard: { is_giftcard: giftcard, type: '', entered_options: [] }
        };

        // Disable add to cart if configurable product and no variant was selected
        if (this._state.configurable && !this._state.sku) {
            this._element.disabled = true;
        }

        if (grouped) {
            // init
            this._onQuantityChanged();

            // Disable/enable add to cart based on the selected quantities of a grouped product
            document.querySelectorAll(AddToCart.selectors.quantity).forEach(selection => {
                selection.addEventListener('change', this._onQuantityChanged.bind(this));
            });
        }

        // Listen to variant updates on product
        config.product.addEventListener(AddToCart.events.variantChanged, this._onUpdateVariant.bind(this));

        // Add click handler to add to cart button
        this._element.addEventListener('click', this._onAddToCart.bind(this));
    }

    /**
     * Variant changed event handler.
     */
    _onUpdateVariant(event) {
        const variant = event.detail.variant;

        // Disable add to cart button if no valid variant is available
        if (!variant) {
            this._element.disabled = true;
            return;
        }

        // Update sku attribute in select element
        document.querySelector(AddToCart.selectors.quantity).setAttribute('data-product-sku', variant.sku);
        document.querySelector(AddToCart.selectors.quantity).setAttribute('data-product-id', variant.id);

        // Update internal state
        this._state.sku = variant.sku;
        this._state.attributes = event.detail.attributes;
        this._element.disabled = false;
    }

    _onQuantityChanged() {
        const selections = Array.from(document.querySelectorAll(AddToCart.selectors.quantity));
        const item = selections.find(selection => parseInt(selection.value) > 0);
        this._element.disabled = item == null || !this._state.sku;
    }

    /**
     * Click event handler for add to cart button.
     */
    _onAddToCart() {
        const items = this._getEventDetail();
        if (items.length > 0 && window.CIF) {
            const customEvent = new CustomEvent(AddToCart.events.addToCart, {
                detail: items
            });
            document.dispatchEvent(customEvent);
        }
    }

    _getEventDetail() {
        if (!this._state.sku) {
            return [];
        }
        // To support grouped products where multiple products can be put in the cart in one single click,
        // the sku of each product is now read from the 'data-product-sku' attribute of each select element
        const selections = Array.from(document.querySelectorAll(AddToCart.selectors.quantity)).filter(selection => {
            return parseInt(selection.value) > 0;
        });

        if (this._state.giftcard.is_giftcard) {
            let giftcardtype = document.querySelector(AddToCart.giftcard.selectors.form).dataset.giftcardtype;
            this._state.giftcard.type = giftcardtype;

            if (this._validateGiftcardForm()) {
                this._giftcardOptions();
            } else {
                return;
            }
        }

        const items = selections.map(selection => {
            return {
                productId: selection.dataset.productId,
                sku: selection.dataset.productSku,
                virtual: this._state.grouped ? selection.dataset.virtual !== undefined : this._state.virtual,
                giftcard: this._state.giftcard,
                quantity: selection.value
            };
        });

        return items;
    }

    _validateGiftcardForm() {
        this._clearErrors();

        let valid = true;

        valid = this._validAmountField(document.querySelector(AddToCart.giftcard.selectors.amount_input)) && valid;
        valid = this._validTextField(document.querySelector(AddToCart.giftcard.selectors.sender_name)) && valid;
        valid = this._validTextField(document.querySelector(AddToCart.giftcard.selectors.recipient_name)) && valid;

        if (this._state.giftcard.type === 'VIRTUAL' || this._state.giftcard.type === 'COMBINED') {
            valid = this._validEmailField(document.querySelector(AddToCart.giftcard.selectors.sender_email)) && valid;
            valid =
                this._validEmailField(document.querySelector(AddToCart.giftcard.selectors.recipient_email)) && valid;
        }

        return valid;
    }

    _clearErrors() {
        const removeElements = elms => elms.forEach(el => el.remove());
        removeElements(document.querySelectorAll('.giftcardProduct__field div.error[generated]'));
    }

    _isValidEmail(email) {
        let valid = true;
        var mailformat = /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*$/;
        if (email.match(mailformat) === null) {
            valid = false;
        }
        return valid;
    }

    _validAmountField(input_element) {
        let valid = true;
        var message = '';
        let amount_input_min = document.querySelector(AddToCart.giftcard.selectors.amount_input_min);
        let amount_input_max = document.querySelector(AddToCart.giftcard.selectors.amount_input_max);

        amount_input_min =
            amount_input_min !== null
                ? amount_input_min.value === ''
                    ? 0.0
                    : amount_input_min.value.substring(1)
                : -1.0;
        amount_input_max =
            amount_input_max !== null
                ? amount_input_max.value === ''
                    ? -1.0
                    : amount_input_max.value.substring(1)
                : -1.0;

        var amount = input_element.value;
        if (amount === '') message = 'This is a required field.';
        else if (isNaN(amount)) message = 'Please enter a valid number.';
        else if (amount_input_max !== -1.0 && +amount > amount_input_max)
            message = 'Please enter a value less than or equal to ' + amount_input_max;
        else if (amount_input_min !== -1.0 && +amount < amount_input_min)
            message = 'Please enter a value greater than or equal to ' + amount_input_min;

        if (message !== '') {
            valid = false;
            var error_html = '<div generated="true" class="error" style="display: block;">' + message + '</div>';
            input_element.insertAdjacentHTML('afterend', error_html);
        }

        return valid;
    }

    _validTextField(input_element) {
        let valid = true;
        var message = '';
        var text = input_element.value;
        if (text === '') message = 'This is a required field.';

        if (message !== '') {
            valid = false;
            var error_html = '<div generated="true" class="error" style="display: block;">' + message + '</div>';
            input_element.insertAdjacentHTML('afterend', error_html);
        }

        return valid;
    }

    _validEmailField(input_element) {
        let valid = true;
        var message = '';
        var email = input_element.value;
        if (email === '') message = 'This is a required field.';
        else if (!this._isValidEmail(email)) message = 'Please enter a valid email address (Ex: johndoe@domain.com).';

        if (message !== '') {
            valid = false;
            var error_html = '<div generated="true" class="error" style="display: block;">' + message + '</div>';
            input_element.insertAdjacentHTML('afterend', error_html);
        }

        return valid;
    }

    _giftcardOptions() {
        let giftcard_amount_input = document.querySelector(AddToCart.giftcard.selectors.amount_input).value;
        let giftcard_sender_name = document.querySelector(AddToCart.giftcard.selectors.sender_name).value;
        let giftcard_recipient_name = document.querySelector(AddToCart.giftcard.selectors.recipient_name).value;
        let giftcard_message = document.querySelector(AddToCart.giftcard.selectors.message).value;

        let giftcard_amount_input_uid = document.querySelector(AddToCart.giftcard.selectors.amount_input + '_uid')
            .value;
        let giftcard_sender_name_uid = document.querySelector(AddToCart.giftcard.selectors.sender_name + '_uid').value;
        let giftcard_recipient_name_uid = document.querySelector(AddToCart.giftcard.selectors.recipient_name + '_uid')
            .value;
        let giftcard_message_uid = document.querySelector(AddToCart.giftcard.selectors.message + '_uid').value;

        this._state.giftcard.entered_options = [];

        if (this._state.giftcard.type === 'VIRTUAL' || this._state.giftcard.type === 'COMBINED') {
            let giftcard_sender_email = document.querySelector(AddToCart.giftcard.selectors.sender_email).value;
            let giftcard_recipient_email = document.querySelector(AddToCart.giftcard.selectors.recipient_email).value;
            let giftcard_sender_email_uid = document.querySelector(AddToCart.giftcard.selectors.sender_email + '_uid')
                .value;
            let giftcard_recipient_email_uid = document.querySelector(
                AddToCart.giftcard.selectors.recipient_email + '_uid'
            ).value;

            this._state.giftcard.entered_options.push({
                uid: giftcard_sender_email_uid,
                value: giftcard_sender_email
            });

            this._state.giftcard.entered_options.push({
                uid: giftcard_recipient_email_uid,
                value: giftcard_recipient_email
            });
        }

        this._state.giftcard.entered_options.push({
            uid: giftcard_amount_input_uid,
            value: giftcard_amount_input + ''
        });
        this._state.giftcard.entered_options.push({
            uid: giftcard_sender_name_uid,
            value: giftcard_sender_name
        });

        this._state.giftcard.entered_options.push({
            uid: giftcard_recipient_name_uid,
            value: giftcard_recipient_name
        });

        this._state.giftcard.entered_options.push({
            uid: giftcard_message_uid,
            value: giftcard_message
        });
    }
}

AddToCart.giftcard = {};

AddToCart.giftcard.selectors = {
    form: '#giftcard_form',
    amount_input: '#giftcard_amount_input',
    amount_input_min: '#giftcard_amount_input_min',
    amount_input_max: '#giftcard_amount_input_max',
    sender_name: '#giftcard_sender_name',
    recipient_name: '#giftcard_recipient_name',
    message: '#giftcard_message',
    sender_email: '#giftcard_sender_email',
    recipient_email: '#giftcard_recipient_email'
};

AddToCart.selectors = {
    self: '.productFullDetail__cartActions button',
    sku: '.productFullDetail__details [role=sku]',
    quantity: '.productFullDetail__quantity select',
    product: '[data-cmp-is=product]'
};

AddToCart.events = {
    variantChanged: 'variantchanged',
    addToCart: 'aem.cif.add-to-cart'
};

(function(document) {
    function onDocumentReady() {
        // Initialize AddToCart component
        const productCmp = document.querySelector(AddToCart.selectors.product);
        const addToCartCmp = document.querySelector(AddToCart.selectors.self);
        if (addToCartCmp) new AddToCart({ element: addToCartCmp, product: productCmp });
    }

    if (document.readyState !== 'loading') {
        onDocumentReady();
    } else {
        document.addEventListener('DOMContentLoaded', onDocumentReady);
    }
})(window.document);

export default AddToCart;
