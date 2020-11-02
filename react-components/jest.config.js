/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
 *    This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License. You may obtain a copy
 *    of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under
 *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *    OF ANY KIND, either express or implied. See the License for the specific language
 *    governing permissions and limitations under the License.
 *
 ******************************************************************************/
// eslint-disable-next-line no-undef
module.exports = {
    collectCoverage: true,
    moduleDirectories: ['node_modules', 'src/utils'],
    coverageDirectory: '<rootDir>/coverage',
    coverageReporters: ['json', 'lcov'],
    coveragePathIgnorePatterns: ['<rootDir>/src/queries', '\\.(gql|graphql)$'],
    testPathIgnorePatterns: ['<rootDir>/node_modules/'],
    reporters: ['default', ['jest-junit', { outputDirectory: './test-results' }]],
    transform: {
        '\\.(gql|graphql)$': 'jest-transform-graphql',
        '.+\\.json': './__mocks__/jsonTransform.js',
        '.+\\.(js|jsx|ts|tsx)$': 'babel-jest'
    },
    moduleNameMapper: {
        '\\.css$': 'identity-obj-proxy',
        '.+\\.(jpg|jpeg|png|gif|eot|otf|webp|svg|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga)$':
            '<rootDir>/__mocks__/fileMock.js'
    },
    transformIgnorePatterns: ['node_modules/(?!@magento/)']
};
