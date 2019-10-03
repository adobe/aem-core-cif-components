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

import React, { createContext, useContext, useReducer } from 'react';
import { object, func } from 'prop-types';

export const initialState = {
    flowState: 'cart',
    order: null
};

export const reducer = (state, action) => {
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
                flowState: 'receipt'
            };
        case 'reset':
            return {
                ...initialState
            };

        default:
            return state;
    }
};

export const CheckoutContext = createContext();

export const CheckoutProvider = ({ reducer, initialState, children }) => {
    return <CheckoutContext.Provider value={useReducer(reducer, initialState)}>{children}</CheckoutContext.Provider>;
};

CheckoutProvider.propTypes = {
    reducer: func.isRequired,
    initialState: object.isRequired
};

export const useCheckoutState = () => useContext(CheckoutContext);
