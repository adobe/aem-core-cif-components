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
import { array, shape, func, bool, number, string } from 'prop-types';
import { useTranslation } from 'react-i18next';

const Select = props => {
    const { item, customization, options, currencyCode, handleSelectionChange } = props;
    const { quantity } = item;
    const { can_change_quantity } =
        customization.length > 0 ? options.find(o => o.id === customization[0].id) : { can_change_quantity: false };
    const [t] = useTranslation(['cart', 'common']);

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
            <div className="productOptionSelect__root">
                <span className="fieldIcons__root">
                    <span className="fieldIcons__input">
                        <select
                            aria-label={item.title}
                            className="select__input field__input product__option"
                            name={item.id}
                            value={customization[0]?.id}
                            onChange={onChange}>
                            {!item.required && <option value="">None</option>}
                            {options.map(o => (
                                <option key={`option-${item.option_id}-${o.id}`} value={o.id}>
                                    {`${o.label} +${t('common:formattedPrice', {
                                        price: { currency: currencyCode, value: o.price }
                                    })}`}
                                </option>
                            ))}
                        </select>
                    </span>
                    <span className="fieldIcons__before"></span>
                    <span className="fieldIcons__after">
                        <span className="icon__root">
                            <svg
                                xmlns="http://www.w3.org/2000/svg"
                                width="18"
                                height="18"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2"
                                strokeLinecap="round"
                                strokeLinejoin="round">
                                <polyline points="6 9 12 15 18 9"></polyline>
                            </svg>
                        </span>
                    </span>
                </span>
            </div>

            <h2 className="product__quantityTitle">
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

Select.propTypes = {
    item: shape({
        required: bool.isRequired,
        option_id: number.isRequired,
        quantity: number.isRequired
    }),
    customization: array.isRequired,
    options: array.isRequired,
    currencyCode: string.isRequired,
    handleSelectionChange: func.isRequired
};

export default Select;
