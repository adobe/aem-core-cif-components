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
import React from 'react';
import Checkout from '../Checkout';
import classes from './footer.css';
import TotalsSummary from './totalsSummary';
import { useCartState } from './cartContext';

const Footer = () => {
    const [{ isOpen, cart }] = useCartState();
    const footerClassName = isOpen ? classes.root_open : classes.root;

    const { currency, value: totalPrice } = cart.prices.grand_total;

    return (
        <div className={footerClassName}>
            <TotalsSummary currencyCode={currency} numItems={cart.items.length} subtotal={totalPrice} />
            <Checkout />
        </div>
    );
};

export default Footer;
