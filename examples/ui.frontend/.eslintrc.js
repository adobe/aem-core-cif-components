module.exports = {
    extends: ['eslint:recommended', 'plugin:react/recommended'],
    parserOptions: {
        ecmaVersion: 2018, // Allows for the parsing of modern ECMAScript features
        sourceType: 'module', // Allows for the use of imports
    },
    env: {
        browser: true,
    },
    settings: {
        react: {
            version: 'detect',
        },
    },
    rules: {
        curly: 1,
        'ordered-imports': [0],
        'object-literal-sort-keys': [0],
        'new-parens': 1,
        'no-bitwise': 1,
        'no-cond-assign': 1,
        'no-trailing-spaces': 0,
        'eol-last': 1,
        'func-style': ['error', 'declaration', {allowArrowFunctions: true}],
        semi: 1,
        'no-var': 0,
    },
};
