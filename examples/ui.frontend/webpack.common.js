'use strict';

const path = require('path');
const webpack = require('webpack');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const TSConfigPathsPlugin = require('tsconfig-paths-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const {CleanWebpackPlugin} = require('clean-webpack-plugin');

const SOURCE_ROOT = `${__dirname}/src/main`;

module.exports = {
    entry: {
        site: `${SOURCE_ROOT}/site/index.js`,
    },
    output: {
        filename: 'cif-examples-react/[name].js',
        path: path.resolve(__dirname, 'dist'),
    },
    module: {
        rules: [
            {
                test: /\.js$/,
                exclude: /node_modules/,
                loader: ['babel-loader', 'eslint-loader'],
            },
            {
                test: /\.scss$/,
                use: [
                    MiniCssExtractPlugin.loader,
                    {
                        loader: 'css-loader',
                        options: {
                            url: false,
                        },
                    },
                    {
                        loader: 'postcss-loader',
                        options: {
                            plugins() {
                                return [require('autoprefixer')];
                            },
                        },
                    },
                    {
                        loader: 'sass-loader',
                        options: {
                            url: false,
                        },
                    },
                    {
                        loader: 'webpack-import-glob-loader',
                        options: {
                            url: false,
                        },
                    },
                ],
            },
        ],
    },
    plugins: [
        new CleanWebpackPlugin(),
        new MiniCssExtractPlugin({
            filename: 'cif-examples-react/[name].css',
        }),
        new CopyWebpackPlugin([
            {
                from: path.resolve(
                    __dirname,
                    'node_modules/@adobe/aem-core-cif-react-components/i18n'
                ),
                to: './cif-examples-react/i18n',
            },
        ]),
    ],
    optimization: {
        noEmitOnErrors: true,
    },
    stats: {
        assetsSort: 'chunks',
        builtAt: true,
        children: false,
        chunkGroups: true,
        chunkOrigins: true,
        colors: false,
        errors: true,
        errorDetails: true,
        env: true,
        modules: false,
        performance: true,
        providedExports: false,
        source: false,
        warnings: true,
    },
};
