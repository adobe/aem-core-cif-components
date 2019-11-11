const path = require('path');
const target = `${__dirname}/../ui.apps/src/main/content/jcr_root/apps/core/cif/clientlibs/react-components/dist`;

module.exports = {
    entry: path.resolve(__dirname, 'src') + '/index.js',
    output: {
        path: `${target}`,
        filename: 'index.js'
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
            }
        ]
    }
};
