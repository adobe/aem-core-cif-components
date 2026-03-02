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
const { CleanWebpackPlugin } = require('clean-webpack-plugin');
const pkg = require('./package.json');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');

const libraryName = pkg.name;
const externals = Object.keys(pkg.peerDependencies)
    .reduce((obj, key) => ({ ...obj, [key]: `commonjs ${key}`}), {});

const entriesDir = path.resolve(__dirname, 'src', 'entries');
const componentEntries = {
    index: path.resolve(__dirname, 'src', 'index.js'),
    CommerceApp: path.join(entriesDir, 'CommerceApp.js'),
    AuthBar: path.join(entriesDir, 'AuthBar.js'),
    Cart: path.join(entriesDir, 'Cart.js'),
    CartTrigger: path.join(entriesDir, 'CartTrigger.js'),
    AccountContainer: path.join(entriesDir, 'AccountContainer.js'),
    AddressBook: path.join(entriesDir, 'AddressBook.js'),
    BundleProductOptions: path.join(entriesDir, 'BundleProductOptions.js'),
    GiftCardOptions: path.join(entriesDir, 'GiftCardOptions.js'),
    Portal: path.join(entriesDir, 'Portal.js'),
    PortalPlacer: path.join(entriesDir, 'PortalPlacer.js'),
    ResetPassword: path.join(entriesDir, 'ResetPassword.js'),
    Price: path.join(entriesDir, 'Price.js'),
    Trigger: path.join(entriesDir, 'Trigger.js'),
    LoadingIndicator: path.join(entriesDir, 'LoadingIndicator.js'),
    ConfigContext: path.join(entriesDir, 'ConfigContext.js'),
    UserContext: path.join(entriesDir, 'UserContext.js'),
    CheckoutProvider: path.join(entriesDir, 'CheckoutProvider.js'),
    AccountDetails: path.join(entriesDir, 'AccountDetails.js'),
    MyAccount: path.join(entriesDir, 'MyAccount.js'),
    authUtils: path.join(entriesDir, 'authUtils.js'),
    hooks: path.join(entriesDir, 'hooks.js'),
    createProductPageUrl: path.join(entriesDir, 'createProductPageUrl.js'),
    useDataLayerEvents: path.join(entriesDir, 'useDataLayerEvents.js'),
    useAddToCart: path.join(entriesDir, 'useAddToCart.js'),
    useAddToWishlistEvent: path.join(entriesDir, 'useAddToWishlistEvent.js'),
    useCustomUrlEvent: path.join(entriesDir, 'useCustomUrlEvent.js'),
    useReferrerEvent: path.join(entriesDir, 'useReferrerEvent.js'),
    usePageEvent: path.join(entriesDir, 'usePageEvent.js'),
    dataLayerUtils: path.join(entriesDir, 'dataLayerUtils.js')
};

module.exports = {
    entry: componentEntries,
    output: {
        path: path.resolve(__dirname, 'dist'),
        filename: '[name].js',
        libraryTarget: 'umd',
        library: libraryName,
        umdNamedDefine: true,
        publicPath: '/dist/'
    },
    optimization: {
        minimize: true,
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
            },
            {
                test: /\.css/,
                use: [
                    MiniCssExtractPlugin.loader,
                    {
                        loader: 'css-loader',
                        options: {
                            modules: {
                                localIdentName: 'cmp-[folder]__[name]__[local]'
                            }
                        }
                    }
                ]
            },
            {
                test: /\.svg$/,
                use: [
                    {
                        loader: 'file-loader',
                        options: {
                            outputPath: 'resources',
                            name: '[name].[ext]'
                        }
                    }
                ]
            }
        ]
    },
    plugins: [
        new CleanWebpackPlugin(),
        new MiniCssExtractPlugin({
            filename: '[name].css',
            chunkFilename: '[id].css'
        })
    ],
    devtool: 'source-map',
    mode: 'production',
    externals: [
        externals,
        // custom handling for pergrine deep imports
        function(_context, request, callback) {
            if (/@magento\/peregrine\//.test(request)) {
                return callback(null, 'commonjs ' + request);
            }
            return callback();
        }
    ]
};
