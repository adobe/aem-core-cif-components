const { merge } = require('webpack-merge');
const common = require('./webpack.common.js');
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer')
    .BundleAnalyzerPlugin;
module.exports = (env) => {
    return merge(common, {
        mode: 'development',
        devtool: 'eval-cheap-source-map',
        performance: {hints: 'warning'},
        plugins: [
            new BundleAnalyzerPlugin({
                analyzerMode: 'static',
                generateStatsFile: true,
                openAnalyzer: false,
            }),
        ],
    });
};
