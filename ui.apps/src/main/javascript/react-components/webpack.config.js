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
const target = `${__dirname}/../../content/jcr_root/apps/core/cif/clientlibs/react-components/dist`;

module.exports = {
    entry: path.resolve(__dirname, 'src') + '/index.js',
    output: {
        path: `${target}`,
        filename: 'index.js'
    },
    module: {
        rules: [
            {
                test: /\.js$/,
                exclude: /(node_modules|dist)/,
                use: {
                    loader: 'babel-loader',
                    options: {
                        presets: ['@babel/preset-react']
                    }
                }
            }
        ]
    },
    // we have to make sure our app uses *our* version of React and not one added by some dependency
    resolve: {
        alias: {
            react: path.resolve('./node_modules/react'),
            'react-i18next': path.resolve('./node_modules/react-i18next')
        }
    },
    devtool: 'source-map',
    mode: 'development'
};
