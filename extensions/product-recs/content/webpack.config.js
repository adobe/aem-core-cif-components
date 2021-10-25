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

const path = require('path');
const glob = require('glob');

const JCR_ROOT = './src/main/content/jcr_root/';
const LIB = {
    EVENTS_COLLECTOR: 'apps/core/cif/extensions/product-recs/clientlibs/storefront-events-collector',
};

function generateBaseConfig() {
    return {
        entry: {
            // Map of clientlib base paths and a corresponding array of JavaScript files that should be packed. We use the
            // key to specify the target destination of the packed code and the glob module to generate a list of JavaScript
            // files matching the given glob expression.
            [LIB.EVENTS_COLLECTOR]: ['@adobe/magento-storefront-event-collector', ...glob.sync(JCR_ROOT + LIB.EVENTS_COLLECTOR + '/js/**/*.js')]
        },
        output: {
            path: path.resolve(__dirname, "src/main/content/jcr_root"),
            // [name] will be replaced by the base path of the clientlib (key of the entry map).
            filename: './[name]/dist/index.js'
        },
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
                            presets: ['@babel/preset-env']
                        }
                    }
                }
            ]
        },
        devtool: 'source-map',
        target: 'web'
    };
}

module.exports = function(env, argv) {
    return generateBaseConfig();
}
