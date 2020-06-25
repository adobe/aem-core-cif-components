/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
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

class SubArray extends Array {
    on() {}
    off() {}
    adaptTo(api) {
        switch (api) {
            case 'foundation-field': {
                return {
                    getValue: function() {},
                    setValue: function(val) {},
                    setDisabled: function(val) {}
                };
            }
            case 'foundation-util-htmlparser': {
                return {
                    parse: function() {
                        return Promise.resolve({});
                    }
                };
            }
            case 'foundation-picker': {
                return {
                    attach: function() {},
                    pick: function() {
                        return { then: function() {} };
                    },
                    focus: function() {}
                };
            }
        }
    }
}

function jQuery(obj) {
    return new SubArray(obj);
}

export default jQuery;
