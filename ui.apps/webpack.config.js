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

const JCR_ROOT = './src/main/content/jcr_root/';
const LIB = {
    COMMON: 'apps/core/cif/clientlibs/common',
    MINICART: 'apps/core/cif/components/commerce/minicart/v1/minicart/clientlib',
    PRODUCT: 'apps/core/cif/components/commerce/product/v1/product/clientlib',
    PRODUCTCAROUSEL: 'apps/core/cif/components/commerce/productcarousel/v1/productcarousel/clientlibs',
    PRODUCTLIST: 'apps/core/cif/components/commerce/productlist/v1/productlist/clientlibs',
    HEADER: 'apps/core/cif/components/structure/header/v1/header/clientlibs',
    NAVIGATION: 'apps/core/cif/components/structure/navigation/v1/navigation/clientlibs'
};

module.exports = {
    entry: {
        [LIB.COMMON]: ['@babel/polyfill', ...glob.sync(JCR_ROOT + LIB.COMMON + '/js/**/*.js')],
        [LIB.MINICART]: glob.sync(JCR_ROOT + LIB.MINICART + '/js/**/*.js'),
        [LIB.PRODUCT]: glob.sync(JCR_ROOT + LIB.PRODUCT + '/js/**/*.js'),
        [LIB.PRODUCTCAROUSEL]: glob.sync(JCR_ROOT + LIB.PRODUCTCAROUSEL + '/js/**/*.js'),
        [LIB.PRODUCTLIST]: glob.sync(JCR_ROOT + LIB.PRODUCTLIST + '/js/**/*.js'),
        [LIB.HEADER]: glob.sync(JCR_ROOT + LIB.HEADER + '/js/**/*.js'),
        [LIB.NAVIGATION]: glob.sync(JCR_ROOT + LIB.NAVIGATION + '/js/**/*.js')
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
    externals: {
        handlebars: 'Handlebars'
    },
    devtool: 'source-map',
    target: 'web'
};