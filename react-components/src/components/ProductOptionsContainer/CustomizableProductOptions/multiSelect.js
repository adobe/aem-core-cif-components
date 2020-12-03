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
import { array, func, number, string } from 'prop-types';
import { useTranslation } from 'react-i18next';

const MultiSelect = props => {

    const [t] = useTranslation("common");

    const { option_id, options, customization, currencyCode, handleSelectionChange } = props;

    const onChange = event => {
        const values = Array.from(event.target.options)
            .filter(e => e.selected)
            .map(e => e.value);
        const newCustomization = options
            .filter(o => values.includes(o.id.toString()))
        handleSelectionChange(option_id, newCustomization);
    };

    return (
        <select
            name={option_id}
            multiple="multiple"
            className="product__option field__input"
            size="5"
            value={customization.map(c => c.id)}
            onChange={onChange}>
            {options.map(o => (
                <option key={`option-${option_id}-${o.id}`} value={o.id}>
                    {`${o.label} +${t('common:formattedPrice', { price: { currency: currencyCode, value: o.price } })}`}
                </option>
            ))}
        </select>
    );
};

MultiSelect.propTypes = {
    option_id: number.isRequired,
    customization: array.isRequired,
    options: array.isRequired,
    currencyCode: string.isRequired,
    handleSelectionChange: func.isRequired,
};

export default MultiSelect;
