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

const Checkbox = props => {
    const { item, customization, sortedOptions, handleSelectionChange } = props;

    const onChange = event => {
        const { checked, value } = event.target;
        let newCustomization;

        if (checked) {
            newCustomization = customization;
            const { id, quantity } = sortedOptions.find(o => o.id == value);
            newCustomization.push({ id, quantity });
        } else {
            newCustomization = customization.filter(c => c.id != value);
        };

        handleSelectionChange(item.option_id, newCustomization);
    }

    return <>
        {sortedOptions.map(
            o => <div key={`option-${item.option_id}-${o.id}`}>
                <label>
                    <input type="checkbox" checked={customization.findIndex(c => c.id === o.id) > -1} onChange={onChange} value={o.id} /> {o.label}
                </label>
            </div>
        )}
    </>
}

export default Checkbox
