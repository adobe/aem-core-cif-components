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

const webpackConfig = require('./webpack.config.js');

module.exports = function(config) {
  config.set({

    // base path that will be used to resolve all patterns (eg. files, exclude)
    basePath: '',


    // frameworks to use
    // available frameworks: https://npmjs.org/browse/keyword/karma-adapter
    frameworks: ['mocha', 'chai', 'sinon'],


    // list of files / patterns to load in the browser
    files: [
      'node_modules/@babel/polyfill/dist/polyfill.js',
      'src/main/content/jcr_root/apps/core/cif/components/commerce/product/**/js/*.js',
      'src/main/content/jcr_root/apps/core/cif/components/commerce/carousel/**/js/*.js',
      'src/main/content/jcr_root/apps/core/cif/components/structure/navigation/**/js/*.js',
      'src/main/content/jcr_root/apps/core/cif/components/content/teaser/**/js/*.js',

      'src/main/content/jcr_root/apps/core/cif/clientlibs/common/js/PriceFormatter.js',
      'src/main/content/jcr_root/apps/core/cif/clientlibs/common/js/CommerceGraphqlApi.js',

      'test/**/*Test.js'
    ],


    // list of files / patterns to exclude
    exclude: [
    ],


    // preprocess matching files before serving them to the browser
    // available preprocessors: https://npmjs.org/browse/keyword/karma-preprocessor
    preprocessors: {
      // source files, that you wanna generate coverage for
      // do not include tests or libraries
      // (these files will be instrumented by Istanbul)
      'test/**/*.js': ['webpack'],
      'src/main/content/**/*.js': ['webpack']
    },

    webpack: webpackConfig({ karma: true }),

    webpackMiddleware: {
      stats: 'errors-only',
    },

    // test results reporter to use
    // possible values: 'dots', 'progress'
    // available reporters: https://npmjs.org/browse/keyword/karma-reporter
    reporters: ['spec', 'junit', 'coverage'],

    // the default configuration
    junitReporter: {
      outputDir: './karma-junit', // results will be saved as $outputDir/$browserName.xml
      useBrowserName: true, // add browser name to report and classes names
    },

    // optionally, configure the reporter
    coverageReporter: {
      includeAllSources: true,
      type: 'lcov',
      dir: './coverage/',
      check: {
        global: {
          statements: 90,
          branches: 70
        },
        each: {
          statements: 80,
          branches: 65,
          excludes: []
        }
      }
    },

    specReporter: {
      suppressSkipped: false,
      showSpecTiming: true,
    },

    // web server port
    port: 9876,


    // enable / disable colors in the output (reporters and logs)
    colors: true,


    // level of logging
    // possible values: config.LOG_DISABLE || config.LOG_ERROR || config.LOG_WARN || config.LOG_INFO || config.LOG_DEBUG
    logLevel: config.LOG_INFO,


    // enable / disable watching file and executing tests whenever any file changes
    autoWatch: false,


    // start these browsers
    // available browser launchers: https://npmjs.org/browse/keyword/karma-launcher
    browsers: ['ChromeHeadless', 'Firefox'],


    // Continuous Integration mode
    // if true, Karma captures browsers, runs the tests and exits
    singleRun: false,

    // Concurrency level
    // how many browser should be started simultaneous
    concurrency: Infinity,

    customLaunchers: {
      FirefoxHeadless: {
        base: 'Firefox',
        flags: ['-headless'],
      },
    },
  })
}
