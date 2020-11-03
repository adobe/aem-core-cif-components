const path = require('path');

module.exports = {
    stories: ['../src/**/*.stories.mdx', '../src/**/*.stories.@(js|jsx|ts|tsx)'],
    addons: [
        '@storybook/addon-links',
        { name: '@storybook/addon-essentials', options: { backgrounds: false } },
        '@storybook/addon-queryparams'
    ],
    webpackFinal: async (config, { configType }) => {
        config.module.rules.push({
            test: /\.scss$/,
            use: ['style-loader', 'css-loader', 'sass-loader'],
            include: path.resolve(__dirname, '../')
        });
        config.resolve.alias = {
            ...config.resolve.alias,
            'react-i18next': path.resolve('./node_modules/react-i18next'),
            'core-js': path.resolve('./node_modules/core-js')
        };

        return config;
    }
};
