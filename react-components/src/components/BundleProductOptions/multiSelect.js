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

const MultiSelect = props => {
    const { item, customization, options, handleSelectionChange } = props;

    const onChange = event => {
        const values = Array.from(event.target.options)
            .filter(e => e.selected)
            .map(e => e.value);
        const newCustomization = options
            .filter(o => values.includes(o.id.toString()))
            .map(o => {
                return { id: o.id, quantity: o.quantity, price: o.price };
            });
        handleSelectionChange(item.option_id, 1, newCustomization);
    };

    return (
        <select
            name={item.option_id}
            multiple="multiple"
            className="bundleProduct__option field__input"
            size="5"
            value={customization.map(c => c.id)}
            onChange={onChange}>
            {options.map(o => (
                <option key={`option-${item.option_id}-${o.id}`} value={o.id}>
                    {o.label}
                </option>
            ))}
        </select>
    );
};

MultiSelect.propTypes = {
    item: shape({
        required: bool.isRequired,
        option_id: number.isRequired
    }),
    customization: array.isRequired,
    options: array.isRequired,
    handleSelectionChange: func.isRequired
};

export default MultiSelect;
