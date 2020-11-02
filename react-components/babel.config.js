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
// eslint-disable-next-line no-undef
const plugins = [
    /**
     * See:
     * https://babeljs.io/docs/en/babel-plugin-proposal-class-properties
     * https://babeljs.io/docs/en/babel-plugin-proposal-object-rest-spread
     * https://babeljs.io/docs/en/next/babel-plugin-syntax-dynamic-import.html
     * https://babeljs.io/docs/en/next/babel-plugin-syntax-jsx.html
     * https://babeljs.io/docs/en/babel-plugin-transform-react-jsx
     * https://www.npmjs.com/package/babel-plugin-graphql-tag
     */
    ['@babel/plugin-proposal-class-properties'],
    ['@babel/plugin-proposal-object-rest-spread'],
    ['@babel/plugin-syntax-dynamic-import'],
    ['@babel/plugin-syntax-jsx'],
    ['@babel/plugin-transform-react-jsx'],
    ['babel-plugin-graphql-tag'],
    ['@babel/plugin-proposal-optional-chaining']
];

// eslint-disable-next-line no-undef
module.exports = function(api) {
    const envConfigs = {
        development: {
            plugins,
            presets: [['@babel/preset-env', { modules: false, targets: 'last 2 Chrome versions' }]]
        },
        test: {
            plugins: [...plugins, ['babel-plugin-dynamic-import-node']],
            presets: [['@babel/preset-env', { modules: 'commonjs', targets: 'node 10' }]]
        }
    };

    return envConfigs[api.env() || 'development'];
};
