/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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

const methods = {
    context: {},
    publish: {}
};

const getMock = (prop, method) => {
    if (methods[prop][method]) {
        return methods[prop][method];
    }
    let mock = jest.fn();
    methods[prop][method] = mock;
    return mock;
};

const mockClear = () => {
    Object.keys(methods.context).forEach(k => methods.context[k].mockClear());
    Object.keys(methods.publish).forEach(k => methods.publish[k].mockClear());
};

const propHandler = {
    get: (target, prop) => {
        if (['context', 'publish'].includes(prop)) {
            const methodHandler = {
                get: (t, method) => {
                    return getMock(prop, method);
                }
            };
            return new Proxy({}, methodHandler);
        } else if (prop === 'mockClear') {
            return mockClear;
        }
    }
};

export default new Proxy({}, propHandler);
