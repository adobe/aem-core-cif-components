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
import { array, func, bool, number, string } from 'prop-types';

const Select = props => {

    const { title, required, option_id, options, customization, handleSelectionChange } = props;

    const onChange = event => {
        const { value } = event.target;
        const newCustomization = options
            .filter(o => o.id == value);

        handleSelectionChange(option_id, newCustomization);
    };

    return (
        <div className="productOptionSelect__root">
            <span className="fieldIcons__root">
                <span className="fieldIcons__input">
                    <select
                        name={option_id}
                        aria-label={title}
                        className="select__input field__input product__option"
                        value={customization[0]?.id}
                        onChange={onChange}>
                        {!required && <option value="">None</option>}
                        {options.map(o => (
                            <option key={`option-${option_id}-${o.id}`} value={o.id}>
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
    );
};

Select.propTypes = {
    title: string.isRequired,
    required: bool.isRequired,
    option_id: number.isRequired,
    customization: array.isRequired,
    options: array.isRequired,
    currencyCode: string.isRequired,
    handleSelectionChange: func.isRequired,
};

export default Select;
