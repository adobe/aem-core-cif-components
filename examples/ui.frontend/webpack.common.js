const path = require('path');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const { CleanWebpackPlugin } = require('clean-webpack-plugin');
const pkg = require('./package.json');

const SOURCE_ROOT = `${__dirname}/src/main`;
const alias = Object.keys(pkg.dependencies)
    .reduce((obj, key) => ({ ...obj, [key]: path.resolve(__dirname, 'node_modules', key) }), {});

module.exports = {
    entry: {
        site: `${SOURCE_ROOT}/site/app/App.js`,
    },
    output: {
        filename: 'cif-examples-react/[name].js',
        chunkFilename: 'cif-examples-react/[name].js',
        path: path.resolve(__dirname, 'dist'),
    },
    module: {
        rules: [
            {
                test: /\.js$/,
                exclude: /node_modules/,
                use: ['babel-loader'],
            },
            {
                test: /\.jsx?$/,
                exclude: /node_modules\/(?!@magento\/)/,
                use: ['babel-loader'],
            },
            {
                test: /\.scss$/,
                use: [
                    MiniCssExtractPlugin.loader,
                    {
                        loader: 'css-loader',
                        options: { url: false },
                    },
                    {
                        loader: 'postcss-loader',
                        options: {
                            postcssOptions: {
                                plugins: [require('autoprefixer')],
                            },
                        },
                    },
                    {
                        loader: 'sass-loader',
                        options: {},
                    },
                    {
                        loader: 'webpack-import-glob-loader',
                        options: {},
                    },
                ],
            },
        ],
    },

    // during development we may have dependencies which are linked in node_modules using either `npm link`
    // or `npm install <file dir>`. Those dependencies will bring *all* their dependencies along, because
    // in that case npm ignores the "devDependencies" setting.
    // In that case, we need to make sure that this project using its own version of React libraries.
    resolve: {
        alias: {
            ...alias,
            // messages are all in ast already, so we can save some bytes like that
            '@formatjs/icu-messageformat-parser': '@formatjs/icu-messageformat-parser/no-parser'
        }
    },
    plugins: [
        new CleanWebpackPlugin(),
        new MiniCssExtractPlugin({
            filename: 'cif-examples-react/[name].css',
        })
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
        modules: true,
        performance: true,
        providedExports: false,
        source: false,
        warnings: true,
    },
};
