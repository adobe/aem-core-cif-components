const path = require('path');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const {CleanWebpackPlugin} = require('clean-webpack-plugin');

const SOURCE_ROOT = `${__dirname}/src/main`;

module.exports = {
    entry: {
        site: `${SOURCE_ROOT}/site/app/App.js`,
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
                loader: ['babel-loader'],
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

    // during development we may have dependencies which are linked in node_modules using either `npm link`
    // or `npm install <file dir>`. Those dependencies will bring *all* their dependencies along, because
    // in that case npm ignores the "devDependencies" setting.
    // In that case, we need to make sure that this project using its own version of React libraries.
    resolve: {
        alias: {
            react: path.resolve('./node_modules/react'),
            'react-dom': path.resolve('./node_modules/react-dom'),
            'react-i18next': path.resolve('./node_modules/react-i18next'),
        },
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
        modules: true,
        performance: true,
        providedExports: false,
        source: false,
        warnings: true,
    },
};
