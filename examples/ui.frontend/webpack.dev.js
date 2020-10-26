const merge = require('webpack-merge');
const common = require('./webpack.common.js');

module.exports = (env) => {
    return merge(common, {
        mode: 'development',
        devtool: 'inline-source-map',
        performance: {hints: 'warning'},
    });
};
