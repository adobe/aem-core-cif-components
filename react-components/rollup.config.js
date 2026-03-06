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
const resolve = require('@rollup/plugin-node-resolve').default;
const commonjs = require('@rollup/plugin-commonjs').default;
const babel = require('@rollup/plugin-babel').default;
const postcss = require('rollup-plugin-postcss');
const pkg = require('./package.json');

const peerDeps = Object.keys(pkg.peerDependencies || {});

function resolveGraphQL() {
    return {
        resolveId(source, importer) {
            if (source.endsWith('.graphql') && importer) {
                return this.resolve(source + '.js', importer, { skipSelf: true }).then(
                    (r) => (r && r.id) || null
                );
            }
        }
    };
}

module.exports = {
    input: 'src/index.js',
    output: [
        {
            dir: 'dist/esm',
            format: 'esm',
            preserveModules: true,
            preserveModulesRoot: 'src',
            entryFileNames: '[name].js',
            chunkFileNames: '[name].js',
            sourcemap: true
        },
        {
            dir: 'dist/cjs',
            format: 'cjs',
            preserveModules: true,
            preserveModulesRoot: 'src',
            entryFileNames: '[name].js',
            chunkFileNames: '[name].js',
            sourcemap: true
        }
    ],
    external: [...peerDeps, /^@magento\/peregrine\/./],
    plugins: [
        resolveGraphQL(),
        resolve({ extensions: ['.js', '.jsx'], mainFields: ['module', 'main'] }),
        babel({
            babelHelpers: 'bundled',
            exclude: 'node_modules/**',
            extensions: ['.js', '.jsx'],
            presets: [
                ['@babel/preset-env', { modules: false }],
                '@babel/preset-react'
            ],
            plugins: ['@babel/plugin-proposal-optional-chaining']
        }),
        commonjs({ include: /node_modules/ }),
        postcss({
            modules: { localIdentName: 'cmp-[folder]__[name]__[local]' },
            inject: true,
            extract: false,
            minimize: true
        })
    ]
};
