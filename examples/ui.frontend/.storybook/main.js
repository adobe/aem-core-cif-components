/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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
const path = require('path');
const custom = require('../webpack.common.js');
const peregrine = require('@magento/babel-preset-peregrine');

module.exports = {
    stories: ['../src/**/*.stories.mdx', '../src/**/*.stories.@(js|jsx|ts|tsx)'],
    addons: [
        '@storybook/addon-links',
        { name: '@storybook/addon-essentials', options: { backgrounds: false } },
        '@storybook/addon-queryparams',
        '@storybook/addon-storysource'
    ],
    webpackFinal: async (config, { configType }) => {
        config.module.rules.push({
            test: /\.scss$/,
            use: ['style-loader', 'css-loader', 'sass-loader'],
            include: path.resolve(__dirname, '../')
        });
        config.module.rules.push({
            test: /\.jsx?$/,
            exclude: /node_modules\/(?!@magento\/)/,
            use: ['babel-loader'],
        });
        config.module.rules.push({
            test: /\.graphql$/,
            exclude: /node_modules/,
            use: ['graphql-tag/loader']
        });
        config.resolve.alias = {
            ...config.resolve.alias,
            ...custom.resolve.alias,
            'core-js': path.resolve('./node_modules/core-js'),
            Queries: path.resolve('../../react-components/src/queries/')
        };

        return config;
    },
    babel: async () => ({
        "plugins": [
            [
                "@babel/plugin-proposal-class-properties"
            ],
            [
                "@babel/plugin-proposal-object-rest-spread"
            ],
            [
                "@babel/plugin-syntax-dynamic-import"
            ],
            [
                "@babel/plugin-syntax-jsx"
            ],
            [
                "@babel/plugin-transform-react-jsx"
            ],
            [
                "babel-plugin-graphql-tag"
            ],
            [
                "formatjs",
                {
                    "ast": true
                }
            ]
        ],
        "presets": [
            [
                "@babel/preset-env",
                {
                    "modules": false,
                    "targets": "last 2 Chrome versions"
                }
            ]
        ],
        "sourceType": "unambiguous"
    })
};
