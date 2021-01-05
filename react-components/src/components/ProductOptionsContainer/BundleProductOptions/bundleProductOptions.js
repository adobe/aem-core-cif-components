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
import Checkbox from './checkbox';
import Radio from './radio';
import Select from './select';
import MultiSelect from './multiSelect';

const BundleProductOptions = props => {
    const { currencyCode, data, handleBundleChange } = props;

    const handleSelectionChange = (option_id, quantity, customization) => {
        const selectionIndex = data.findIndex(s => s.option_id === option_id);
        const selection = data[selectionIndex];
        data.splice(selectionIndex, 1, { ...selection, quantity, customization });

        handleBundleChange(data);
    };

    const renderItemOptions = item => {
        const { option_id, required, quantity, options, type, customization } = item;
        const otherProps = { currencyCode, options, customization, handleSelectionChange };

        switch (type) {
            case 'checkbox': {
                return <Checkbox item={{ option_id, required }} {...otherProps} />;
            }
            case 'radio': {
                return <Radio item={{ option_id, required, quantity }} {...otherProps} />;
            }
            case 'select': {
                return <Select item={{ option_id, required, quantity }} {...otherProps} />;
            }
            case 'multi': {
                return <MultiSelect item={{ option_id, required }} {...otherProps} />;
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
                        <span>{e.title}</span> {e.required && <span className="required"> *</span>}
                    </h3>
                    <div>{renderItemOptions(e)}</div>
                </section>
            ))}
        </>
    );
};

BundleProductOptions.propTypes = {
    currencyCode: string.isRequired,
    data: array.isRequired,
    handleBundleChange: func.isRequired
};

export default BundleProductOptions;
