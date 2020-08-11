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
import classes from './bundleProductOptions.css'

const Radio = props => {
    const { item, customization, sortedOptions, handleSelectionChange } = props;
    const canChangeQuantity = sortedOptions.find(o => o.id === customization[0].id).can_change_quantity;

    const onChange = event => {
        const { value } = event.target;
        const newCustomization = sortedOptions.filter(o => o.id == value).map(o => { return { id: o.id, quantity: o.quantity } });
        handleSelectionChange(item.option_id, newCustomization);
    }

    const onQuantityChange = event => {
        handleSelectionChange(item.option_id, [{ ...customization[0], quantity: parseInt(event.target.value) }])
    }

    return <>
        {!item.required &&
            <div>
                <label>
                    <input type="radio" name={item.option_id} value="" onChange={onChange} checked={customization.length === 0} /> None
                </label>
            </div>
        }
        {sortedOptions.map(
            o => <div key={`option-${item.option_id}-${o.id}`}>
                <label>
                    <input type="radio" name={item.option_id} value={o.id} onChange={onChange} checked={customization.findIndex(c => c.id === o.id) > -1} /> {o.label}
                </label>
            </div>
        )}
        <h2 className={classes.option_quantity_title}>
            <span>Quantity</span>
        </h2>
        <input type="number" className={classes.option_quantity_input} disabled={!canChangeQuantity} value={customization[0]?.quantity} onChange={onQuantityChange} />
    </>
}

export default Radio

