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

const os = require('os');
const path = require('path');
const glob = require('glob');

const JCR_ROOT = './src/main/content/jcr_root/';
const LIB = {
    COMMON: 'apps/core/cif/clientlibs/common',
    STOREFRONT_EVENTS: 'apps/core/cif/clientlibs/storefront-events',
    PRODUCT: 'apps/core/cif/components/commerce/product/v1/product/clientlib',
    PRODUCTCAROUSEL: 'apps/core/cif/components/commerce/productcarousel/v1/productcarousel/clientlibs',
    PRODUCTCOLLECTION: 'apps/core/cif/components/commerce/productcollection/v1/productcollection/clientlibs',
    PRODUCTCOLLECTION_V2: 'apps/core/cif/components/commerce/productcollection/v2/productcollection/clientlibs',
    PRODUCTLIST_EDITOR_V2: 'apps/core/cif/components/commerce/productlist/v2/productlist/clientlibs/editor',
    SEARCHBAR: 'apps/core/cif/components/commerce/searchbar/v1/searchbar/clientlibs',
    SEARCHBAR_V2: 'apps/core/cif/components/commerce/searchbar/v2/searchbar/clientlibs',
    NAVIGATION: 'apps/core/cif/components/structure/navigation/v1/navigation/clientlibs',
    PRODUCTTEASER: 'apps/core/cif/components/commerce/productteaser/v1/productteaser/clientlibs',
    CONTENTTEASER_EDITOR: 'apps/core/cif/components/content/teaser/v1/teaser/clientlib/editor',
    CONTENTTEASER_EDITOR_V2: 'apps/core/cif/components/content/teaser/v2/teaser/clientlib/editor',
    CONTENTTEASER_EDITOR_V3: 'apps/core/cif/components/content/teaser/v3/teaser/clientlib/editor',
    CAROUSEL: 'apps/core/cif/components/commerce/carousel/v1/carousel/clientlibs',
};

function generateBaseConfig(bablePlugins = []) {
    return {
        module: {
            rules: [
                // Transpile .js files with babel. Babel will by default pick up the browserslist definition in the 
                // package.json file.
                {
                    test: /\.js$/,
                    exclude: /(node_modules|bower_components)/,
                    use: {
                        loader: 'babel-loader',
                        options: {
                            presets: ['@babel/preset-env'],
                            plugins: bablePlugins
                        }
                    }
                }
            ]
        },
        devtool: 'source-map',
        target: 'web'
    };
}

function applyKarmaOptions() {
    let karma = generateBaseConfig();

    // Disable minification
    karma.mode = 'development';

    
    return karma;
}

module.exports = function (env, argv) {
    // Return karma specific configuration
    if (env.karma) {
        return {
            ...generateBaseConfig(['istanbul']),
            mode: 'development',
            // output into a temp directory
            output: {
                path: path.join(os.tmpdir(), '_karma_webpack_') + Math.floor(Math.random() * 1000000),
            },
        }
    } else {
        return {
            ...generateBaseConfig(),
            entry: {
                // Map of clientlib base paths and a corresponding array of JavaScript files that should be packed. We use the
                // key to specify the target destination of the packed code and the glob module to generate a list of JavaScript
                // files matching the given glob expression.
                [LIB.COMMON]: ['@babel/polyfill', ...glob.sync(JCR_ROOT + LIB.COMMON + '/js/**/*.js')],
                [LIB.STOREFRONT_EVENTS]: ['@adobe/magento-storefront-events-sdk', ...glob.sync(JCR_ROOT + LIB.STOREFRONT_EVENTS + '/js/**/*.js')],
                [LIB.PRODUCT]: glob.sync(JCR_ROOT + LIB.PRODUCT + '/js/**/*.js'),
                [LIB.CAROUSEL]: glob.sync(JCR_ROOT + LIB.CAROUSEL + '/js/**/*.js'),
                [LIB.PRODUCTCAROUSEL]: glob.sync(JCR_ROOT + LIB.PRODUCTCAROUSEL + '/js/**/*.js'),
                [LIB.PRODUCTCOLLECTION]: glob.sync(JCR_ROOT + LIB.PRODUCTCOLLECTION + '/js/**/*.js'),
                [LIB.PRODUCTCOLLECTION_V2]: glob.sync(JCR_ROOT + LIB.PRODUCTCOLLECTION_V2 + '/js/**/*.js'),
                [LIB.PRODUCTLIST_EDITOR_V2]: glob.sync(JCR_ROOT + LIB.PRODUCTLIST_EDITOR_V2 + '/js/**/*.js'),
                [LIB.SEARCHBAR]: glob.sync(JCR_ROOT + LIB.SEARCHBAR + '/js/**/*.js'),
                [LIB.SEARCHBAR_V2]: glob.sync(JCR_ROOT + LIB.SEARCHBAR_V2 + '/js/**/*.js'),
                [LIB.NAVIGATION]: glob.sync(JCR_ROOT + LIB.NAVIGATION + '/js/**/*.js'),
                [LIB.PRODUCTTEASER]: glob.sync(`${JCR_ROOT}${LIB.PRODUCTTEASER}/js/**/*.js`),
                [LIB.CONTENTTEASER_EDITOR]: glob.sync(`${JCR_ROOT}${LIB.CONTENTTEASER_EDITOR}/js/**/*.js`),
                [LIB.CONTENTTEASER_EDITOR_V2]: glob.sync(`${JCR_ROOT}${LIB.CONTENTTEASER_EDITOR_V2}/js/**/*.js`),
                [LIB.CONTENTTEASER_EDITOR_V3]: glob.sync(`${JCR_ROOT}${LIB.CONTENTTEASER_EDITOR_V3}/js/**/*.js`)
            },
            output: {
                path: path.resolve(__dirname, "src/main/content/jcr_root"),
                // [name] will be replaced by the base path of the clientlib (key of the entry map).
                filename: './[name]/dist/index.js'
            }
        };
    }
}