/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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
// eslint-disable-next-line no-undef
const plugins = [
    /**
     * See:
     *  https://babeljs.io/docs/en/babel-plugin-proposal-optional-chaining
     */
    ['@babel/plugin-proposal-optional-chaining']
];

const presets = [['@babel/preset-react']];
// eslint-disable-next-line no-undef
module.exports = function(api) {
    const envConfigs = {
        development: {
            plugins,
            presets: [...presets, ['@babel/preset-env', { modules: false, targets: 'last 2 Chrome versions' }]]
        },
        test: {
            plugins: [...plugins, ['babel-plugin-dynamic-import-node']],
            presets: [...presets, ['@babel/preset-env', { modules: 'commonjs', targets: 'node 10' }]]
        }
    };

    return envConfigs[Object.keys(envConfigs).indexOf(api.env()) !== -1 ? api.env() : 'development'];
};
