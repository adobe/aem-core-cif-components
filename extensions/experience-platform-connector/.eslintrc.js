// eslint-disable-next-line header/header
const headerBlock = [
    '~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~',
    {
        pattern: ' ~ Copyright \\d{4} Adobe',
        template: ` ~ Copyright ${new Date().getFullYear()} Adobe`,
    },
    ' ~',
    ' ~ Licensed under the Apache License, Version 2.0 (the "License");',
    ' ~ you may not use this file except in compliance with the License.',
    ' ~ You may obtain a copy of the License at',
    ' ~',
    ' ~     http://www.apache.org/licenses/LICENSE-2.0',
    ' ~',
    ' ~ Unless required by applicable law or agreed to in writing, software',
    ' ~ distributed under the License is distributed on an "AS IS" BASIS,',
    ' ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.',
    ' ~ See the License for the specific language governing permissions and',
    ' ~ limitations under the License.',
    ' ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~',
];

module.exports = {
    settings: {
        react: {
            version: 'detect'
        }
    },
    env: {
        browser: true,
        es6: true,
        'jest/globals': true
    },
    extends: [
        'eslint:recommended',
        'plugin:react/recommended',
        'plugin:jest/recommended'
    ],
    parser: 'babel-eslint',
    globals: {
        Atomics: 'readonly',
        SharedArrayBuffer: 'readonly',
        process: true
    },
    parserOptions: {
        ecmaFeatures: {
            jsx: true
        },
        ecmaVersion: 2018,
        sourceType: 'module'
    },
    plugins: ['react', 'react-hooks', 'header', 'jest'],
    rules: {
        'no-console': ['warn', { allow: ['error', 'warn'] }],
        'no-undef': 'error',
        'no-unused-vars': 'warn',
        'header/header': [2, 'block', headerBlock],
        'no-var': 'error',
        'one-var': ['error', 'never'],
        'react-hooks/rules-of-hooks': 'error', // Checks rules of Hooks
        // override the default which is more restrictive
        'react/prop-types': ['warn', { ignore: ['children'] }],
        strict: ['error', 'global'],
        'jest/valid-describe': 'off'
    }
};
