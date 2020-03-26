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

    const { subtotal_excluding_tax, subtotal_with_discount_excluding_tax } = cart.prices;

    return (
        <div className={footerClassName}>
            <TotalsSummary
                subtotal={subtotal_excluding_tax}
                subtotalDiscount={subtotal_with_discount_excluding_tax}
                numItems={cart.total_quantity}
            />
            <Checkout />
        </div>
    );
};

export default Footer;
