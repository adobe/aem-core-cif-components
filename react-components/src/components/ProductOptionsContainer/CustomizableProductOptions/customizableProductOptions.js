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
import { func, array, string } from 'prop-types';

import Price from '../../Price';
import Radio from './radio';
import Checkbox from './checkbox';
import TextField from './textfield';
import Select from './select';
import MultiSelect from './multiSelect';

const CustomizableProductOptions = props => {
    const { currencyCode, data, handleOptionsChange } = props;

    const handleSelectionChange = (option_id, customization) => {
        const selectionIndex = data.findIndex(s => s.option_id === option_id);
        const selection = data[selectionIndex];
        data.splice(selectionIndex, 1, { ...selection, customization });

        handleOptionsChange(data);
    };

    const renderItemOptions = item => {
        const { option_id, required, options, type, customization, title } = item;
        const itemProps = { option_id, required, currencyCode, customization, handleSelectionChange };
        switch (type) {
            case "CustomizableFieldOption": {
                return <TextField max_characters={item.value.max_characters} {...itemProps} />;
            }

            case "CustomizableAreaOption": {
                return <TextField textarea max_characters={item.value.max_characters} {...itemProps} />;
            }

            case "CustomizableDropDownOption": {
                return <Select options={options} title={title} {...itemProps} />
            }

            case "CustomizableRadioOption": {
                return <Radio options={options} {...itemProps} />;
            }

            case "CustomizableCheckboxOption": {
                return <Checkbox options={options} {...itemProps} />;
            }

            case "CustomizableMultipleOption": {
                return <MultiSelect options={options} {...itemProps} />;
            }
        }
    };

    return (
        <>
            {data.map(e => (
                <section
                    key={`item-${e.option_id}`}
                    className="productFullDetail__section productFullDetail__productOption">
                    <h3 className="option__title">
                        <span>{e.title}</span>
                        {e?.value && <span>
                            {' + '}
                            <Price currencyCode={currencyCode} value={e.value.price} />
                        </span>}
                        {e.required && <span className="required"> *</span>}
                    </h3>
                    <div>{renderItemOptions(e)}</div>
                </section>
            ))}

        </>
    );
}

CustomizableProductOptions.propTypes = {
    currencyCode: string.isRequired,
    data: array.isRequired,
    handleOptionsChange: func.isRequired
};

export default CustomizableProductOptions;
