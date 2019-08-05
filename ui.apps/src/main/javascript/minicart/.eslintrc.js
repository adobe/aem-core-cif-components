const headerBlock = [
    '******************************************************************************',
    ' *',
    ' *    Copyright 2019 Adobe. All rights reserved.',
    ' *    This file is licensed to you under the Apache License, Version 2.0 (the "License");',
    ' *    you may not use this file except in compliance with the License. You may obtain a copy',
    ' *    of the License at http://www.apache.org/licenses/LICENSE-2.0',
    ' *',
    ' *    Unless required by applicable law or agreed to in writing, software distributed under',
    ' *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS',
    ' *    OF ANY KIND, either express or implied. See the License for the specific language',
    ' *    governing permissions and limitations under the License.',
    ' *',
    ' *****************************************************************************'
];

module.exports = {
    env: {
        browser: true,
        es6: true
    },
    extends: ['eslint:recommended', 'plugin:react/recommended'],
    globals: {
        Atomics: 'readonly',
        SharedArrayBuffer: 'readonly'
    },
    parserOptions: {
        ecmaFeatures: {
            jsx: true
        },
        ecmaVersion: 2018,
        sourceType: 'module'
    },
    plugins: ['react', 'react-hooks', 'header'],
    rules: {
        'no-undef': 'warn',
        'no-unused-vars': 'warn',
        'no-console': 'off',
        'header/header': [2, 'block', headerBlock],
        'no-var': 'error',
        'one-var': ['error', 'never'],
        // override the default which is more restrictive
        'react/prop-types': 'warn',
        strict: ['error', 'global']
    }
};
