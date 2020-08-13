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

import classes from './bundleProductOptions.css';

const Select = props => {
    const { item, customization, sortedOptions, handleSelectionChange } = props;
    const canChangeQuantity = sortedOptions.find(o => o.id === customization[0].id).can_change_quantity;
    const [t] = useTranslation('cart');

    const onChange = event => {
        const { value } = event.target;
        const newCustomization = sortedOptions
            .filter(o => o.id == value)
            .map(o => {
                return { id: o.id, quantity: o.quantity, price: o.price };
            });
        handleSelectionChange(item.option_id, newCustomization);
    };

    const onQuantityChange = event => {
        try {
            const quantity = parseInt(event.target.value);
            if (quantity > 0) {
                handleSelectionChange(item.option_id, [
                    { ...customization[0], quantity: parseInt(event.target.value) }
                ]);
            }
        } catch {
            throw new Error('Invalid quantity entered');
        }
    };

    return (
        <>
            <div className={classes.select_options_root}>
                <span className="fieldIcons__root">
                    <span className="fieldIcons__input">
                        <select
                            aria-label={item.title}
                            className="select__input field__input"
                            name={item.id}
                            value={customization[0]?.id}
                            onChange={onChange}>
                            {!item.required && <option value="">None</option>}
                            {sortedOptions.map(o => (
                                <option key={`option-${item.option_id}-${o.id}`} value={o.id}>
                                    {o.label}
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

            <h2 className={classes.option_quantity_title}>
                <span>{t('cart:quantity', 'Quantity')}</span>
            </h2>
            <input
                type="number"
                min="1"
                className={classes.option_quantity_input}
                disabled={!canChangeQuantity}
                value={customization[0]?.quantity}
                onChange={onQuantityChange}
            />
        </>
    );
};

Select.propTypes = {
    item: shape({
        required: bool.isRequired,
        option_id: number.isRequired
    }),
    customization: array.isRequired,
    sortedOptions: array.isRequired,
    handleSelectionChange: func.isRequired
};

export default Select;
