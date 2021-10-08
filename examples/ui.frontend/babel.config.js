/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe Systems Incorporated
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
const peregrine = require('@magento/babel-preset-peregrine');

module.exports = (api, opts = {}) => {
    const config = {
        ...peregrine(api, opts),
        // important as some in combiation with babel-runtime and umd/esm mixed
        // module the default value will cause the imports from the umd
        // libraries (@adobe/) to be not recognized anymore
        sourceType: 'unambiguous'
    }

    // Remove react-refresh/babel as this causes issues with the modules
    // extracted using the mini-css-extract-plugin. sources suggest to exclude
    // node_modules from babel which we cannot do because of peregrine.
    // We should fix this in peregrine (make it configureable)
    config.plugins = config.plugins.filter(plugin => plugin !== 'react-refresh/babel');
    config.plugins.push(
        ['formatjs', { ast: true }]
    )

    return config;
}
