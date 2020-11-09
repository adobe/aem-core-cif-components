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

const path = require('path');

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
            test: /\.graphql$/,
            exclude: /node_modules/,
            use: ['graphql-tag/loader']
        });
        config.resolve.alias = {
            ...config.resolve.alias,
            'react-i18next': path.resolve('./node_modules/react-i18next'),
            'core-js': path.resolve('./node_modules/core-js'),
            Queries: path.resolve('../../react-components/src/queries/')
        };

        return config;
    }
};
