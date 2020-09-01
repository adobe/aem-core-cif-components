/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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
import { array, shape, func, bool, number } from 'prop-types';
import { useTranslation } from 'react-i18next';

import Price from '../Price';

const Radio = props => {
    const { item, customization, options, handleSelectionChange } = props;
    const { quantity } = item;
    const { can_change_quantity } = options.find(o => o.id === customization[0].id);
    const [t] = useTranslation('cart');

    const onChange = event => {
        const { value } = event.target;
        let newQuantity = 1;
        const newCustomization = options
            .filter(o => o.id == value)
            .map(o => {
                newQuantity = o.quantity;
                return { id: o.id, quantity: newQuantity, price: o.price };
            });

        handleSelectionChange(item.option_id, newQuantity, newCustomization);
    };

    const onQuantityChange = event => {
        try {
            const newQuantity = parseInt(event.target.value);
            if (newQuantity > 0) {
                handleSelectionChange(item.option_id, newQuantity, customization);
            }
        } catch {
            throw new Error('Invalid quantity entered');
        }
    };

    return (
        <>
            {!item.required && (
                <div className="bundleProduct__option">
                    <label>
                        <input
                            type="radio"
                            name={item.option_id}
                            value=""
                            onChange={onChange}
                            checked={customization.length === 0}
                        />{' '}
                        None
                    </label>
                </div>
            )}
            {options.map(o => (
                <div key={`option-${item.option_id}-${o.id}`} className="bundleProduct__options">
                    <label>
                        <input
                            type="radio"
                            name={item.option_id}
                            value={o.id}
                            onChange={onChange}
                            checked={customization.findIndex(c => c.id === o.id) > -1}
                        />{' '}
                        {`${o.label} +`}
                        <b>
                            <Price currencyCode={o.currency} value={o.price} />
                        </b>
                    </label>
                </div>
            ))}
            <h2 className="option__title productFullDetail__quantityTitle">
                <span>{t('cart:quantity', 'Quantity')}</span>
            </h2>
            <input
                type="number"
                min="1"
                className="option__quantity"
                disabled={!can_change_quantity}
                value={quantity}
                onChange={onQuantityChange}
            />
        </>
    );
};

Radio.propTypes = {
    item: shape({
        required: bool.isRequired,
        option_id: number.isRequired,
        quantity: number.isRequired
    }),
    customization: array.isRequired,
    options: array.isRequired,
    handleSelectionChange: func.isRequired
};

export default Radio;
