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
const glob = require('glob');

const APPS_ROOT = './src/main/content/jcr_root/apps';

module.exports = {
    entry: {
        'apps/core/cif/clientlibs/common': ['@babel/polyfill', ...glob.sync(APPS_ROOT + '/core/cif/clientlibs/common/js/**/*.js')],
        'apps/core/cif/components/commerce/minicart/v1/minicart/clientlib': glob.sync(APPS_ROOT + '/core/cif/components/commerce/minicart/v1/minicart/clientlib/js/**/*.js'),
        'apps/core/cif/components/commerce/product/v1/product/clientlib': glob.sync(APPS_ROOT + '/core/cif/components/commerce/product/v1/product/clientlib/js/**/*.js'),
        'apps/core/cif/components/commerce/productcarousel/v1/productcarousel/clientlibs': glob.sync(APPS_ROOT + '/core/cif/components/commerce/productcarousel/v1/productcarousel/clientlibs/js/**/*.js'),
        'apps/core/cif/components/structure/header/v1/header/clientlibs': glob.sync(APPS_ROOT + '/core/cif/components/structure/header/v1/header/clientlibs/js/**/*.js'),
        'apps/core/cif/components/structure/navigation/v1/navigation/clientlibs': glob.sync(APPS_ROOT + '/core/cif/components/structure/navigation/v1/navigation/clientlibs/js/**/*.js'),
    },
    output: {
        path: path.resolve(__dirname, "src/main/content/jcr_root"),
        filename: './[name]/dist/index.js'
    },
    module: {
        rules: [
            {
                test: /\.js$/,
                exclude: /(node_modules|bower_components)/,
                use: {
                    loader: 'babel-loader',
                    options: {
                        presets: ['@babel/preset-env']
                    }
                }
            }
        ]
    },
    devtool: 'source-map',
    target: 'web'
};