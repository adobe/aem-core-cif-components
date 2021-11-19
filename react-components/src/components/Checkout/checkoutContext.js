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
import React, { createContext, useContext, useReducer } from 'react';
import { bool, func, object, shape, string } from 'prop-types';

/**
 * @deprecated replace with peregrine backed component, will be removed with CIF 3.0 latest
 */
export const initialCheckoutState = {
    flowState: 'cart',
    order: null,
    editing: null,
    shippingAddress: null,
    billingAddress: null,
    billingAddressSameAsShippingAddress: true,
    isEditingNewAddress: false,
    shippingMethod: null,
    paymentMethod: null,
    braintreeToken: false
};

export const checkoutReducer = (state, action) => {
    switch (action.type) {
        case 'beginCheckout':
            return {
                ...state,
                flowState: 'form'
            };
        case 'cancelCheckout':
            return {
                ...state,
                flowState: 'cart'
            };
        case 'placeOrder':
            return {
                ...state,
                order: action.order,
                flowState: 'receipt',
                editing: 'receipt'
            };
        case 'reset':
            return {
                ...initialCheckoutState
            };
        case 'setEditing':
            return {
                ...state,
                editing: action.editing
            };
        case 'endEditing':
            return {
                ...state,
                editing: null,
                isEditingNewAddress: false
            };
        case 'setShippingAddress':
            return {
                ...state,
                shippingAddress: action.shippingAddress,
                editing: null,
                isEditingNewAddress: false
            };
        case 'setShippingAddressEmail':
            return {
                ...state,
                shippingAddress: {
                    ...state.shippingAddress,
                    email: action.email
                }
            };
        case 'setBillingAddress':
            return {
                ...state,
                billingAddress: action.billingAddress,
                editing: null,
                isEditingNewAddress: false
            };
        case 'setBillingAddressEmail':
            return {
                ...state,
                billingAddress: {
                    ...state.billingAddress,
                    email: action.email
                }
            };
        case 'setBillingAddressSameAsShippingAddress':
            return {
                ...state,
                billingAddressSameAsShippingAddress: action.same
            };
        case 'setIsEditingNewAddress':
            return {
                ...state,
                isEditingNewAddress: action.editing
            };
        case 'setShippingMethod':
            return {
                ...state,
                shippingMethod: action.shippingMethod,
                editing: null
            };
        case 'setPaymentMethod':
            return {
                ...state,
                paymentMethod: action.paymentMethod,
                editing: null
            };
        case 'setBraintreeToken':
            return {
                ...state,
                braintreeToken: action.token
            };

        default:
            return state;
    }
};

export const CheckoutContext = createContext();

export const CheckoutProvider = props => {
    const reducer = props.reducer || checkoutReducer;
    const initialState = props.initialState || initialCheckoutState;
    return (
        <CheckoutContext.Provider value={useReducer(reducer, initialState)}>{props.children}</CheckoutContext.Provider>
    );
};

CheckoutProvider.propTypes = {
    reducer: func,
    initialState: shape({
        flowState: string.isRequired,
        order: object,
        editing: string,
        shippingAddress: object,
        billingAddress: object,
        billingAddressSameAsShippingAddress: bool,
        isEditingNewAddress: bool,
        shippingMethod: object,
        paymentMethod: object
    })
};

export const useCheckoutState = () => useContext(CheckoutContext);
