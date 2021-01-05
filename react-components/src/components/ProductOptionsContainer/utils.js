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

export function createBundleState(bundleItems) {
    return bundleItems.map(item => {
        return {
            option_id: item.option_id,
            required: item.required,
            title: item.title,
            type: item.type,
            quantity: ['checkbox', 'multi'].includes(item.type) ? 1 : item.options.find(o => o.is_default).quantity,
            options: item.options
                .slice() // return a shallow copy of the array. the original is frozen and cannot be sorted as Array.prototype.sort() sorts the elements of the array in place
                .sort((a, b) => a.position - b.position)
                .map(option => {
                    const { id, can_change_quantity, quantity, label, price } = option;

                    return {
                        id,
                        can_change_quantity,
                        quantity,
                        label,
                        price
                    };
                }),
            customization: item.options
                .filter(o => o.is_default)
                .map(o => {
                    const { id, quantity, price } = o;

                    return {
                        id,
                        quantity,
                        price
                    };
                })
        };
    });
}

export function getBundlePrice(bundleState) {
    return bundleState
        ? bundleState.reduce((acc, selection) => {
              return (
                  acc +
                  selection.quantity *
                      selection.customization.reduce((a, c) => {
                          return a + c.price * (['checkbox', 'multi'].includes(selection.type) ? c.quantity : 1);
                      }, 0)
              );
          }, 0)
        : 0;
}

export function getBundleCartOptions(bundleState) {
    if (bundleState) {
        return {
            bundle: true,
            options: bundleState.map(s => {
                return {
                    id: s.option_id,
                    quantity: s.quantity,
                    value: s.customization.map(c => c.id.toString())
                };
            })
        };
    }

    return {};
}

export function isOptionsStateValid(state) {
    return state
        ? state.reduce((acc, option) => {
              return acc && (option.required ? option.customization.length > 0 : true);
          }, true)
        : true;
}

export function createCustomizableOptionsState(options) {
    return options.map(item => {
        let result = {
            option_id: item.option_id,
            required: item.required,
            title: item.title,
            type: item.__typename
        };

        if (
            [
                'CustomizableDropDownOption',
                'CustomizableMultipleOption',
                'CustomizableRadioOption',
                'CustomizableCheckboxOption'
            ].includes(item.__typename)
        ) {
            result.options = item.value__multi
                .slice() // return a shallow copy of the array. the original is frozen and cannot be sorted as Array.prototype.sort() sorts the elements of the array in place
                .sort((a, b) => a.position - b.position)
                .map(o => {
                    return {
                        id: o.option_type_id,
                        label: o.title,
                        price: o.price
                    };
                });

            result.customization = item.required ? [result.options[0]] : [];
        } else {
            result.value = item.value;
            result.customization = '';
        }

        return result;
    });
}

export function getCustomizableOptionsPrice(optionsState) {
    return optionsState
        ? optionsState.reduce((acc, selection) => {
              const { value, customization } = selection;
              return (
                  acc +
                  (value
                      ? customization.length > 0
                          ? value.price
                          : 0
                      : customization.reduce((a, c) => a + c.price, 0))
              );
          }, 0)
        : 0;
}

export function getCustomizableCartOptions(optionsState) {
    if (optionsState) {
        const customizable_options = [];

        optionsState.forEach(selection => {
            const { option_id, value, customization } = selection;
            if (value) {
                if (customization.length > 0) customizable_options.push({ id: option_id, value_string: customization });
            } else {
                customizable_options.push({
                    id: option_id,
                    value_string: customization.map(c => c.id.toString()).join(',')
                });
            }
        });

        if (customizable_options.length > 0)
            return {
                customizable_options
            };
    }

    return {};
}
