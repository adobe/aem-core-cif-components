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

const MultiSelect = props => {
    const { item, customization, sortedOptions, handleSelectionChange } = props;

    const onChange = event => {
        const values = Array.from(event.target.options).filter(e => e.selected).map(e => e.value);
        const newCustomization = sortedOptions.filter(o => values.includes(o.id.toString())).map(o => { return { id: o.id, quantity: o.quantity } });
        handleSelectionChange(item.option_id, newCustomization);
    }

    return <select name={item.option_id} multiple="multiple" className={classes.multiselect_options_root} size="5" value={customization.map(c => c.id)} onChange={onChange}>
        {sortedOptions.map(
            o => <option key={`option-${item.option_id}-${o.id}`} value={o.id}>
                {o.label}
            </option>
        )}
    </select>
}

export default MultiSelect
