/*
 *  Copyright 2021 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 * WDIO Testrunner Configuration - See https://webdriver.io/docs/configurationfile.html
 */
const conf = require('./lib/config');
const commons = require('./lib/commons');
const wdioDiagnostics = require('./lib/wdio.diagnostics');
const HtmlReporter = require('@rpii/wdio-html-reporter').HtmlReporter;
const path = require('path');
const log4js = require('log4js');

exports.config = {
    runner: 'local',

    // Tests
    specs: [
        './specs/**/*.js'
    ],

    // Use WDIO_LOG_LEVEL=trace for full WebDriver protocol logs in CI.
    logLevel: process.env.WDIO_LOG_LEVEL || 'info',

    bail: 0,

    baseUrl: conf.aem.author.base_url,

    sync: true,

    waitforTimeout: 60000,
    connectionRetryTimeout: 120000,
    connectionRetryCount: 3,

    framework: 'mocha',

    // Location of the WDIO/Selenium logs
    outputDir: conf.reports_path,

    // Reporters
    reporters: [
        'spec',
        ['junit', {
            outputDir: path.join(conf.reports_path, 'junit'),
            outputFileFormat: function(options) {
                return `results-${options.cid}.${options.capabilities.browserName}.xml`;
            }
        }],
        [HtmlReporter, {
            debug: true,
            outputDir: path.join(path.relative(process.cwd(), conf.reports_path), 'html/'),
            filename: 'report.html',
            reportTitle: 'UI Testing Basic Tests',
            showInBrowser: false,
            useOnAfterCommandForScreenshot: true,
            LOG: log4js.getLogger('default')
        }],
    ],

    // Mocha parameters
    mochaOpts: {
        ui: 'bdd',
        timeout: 120000
    },

    // Gets executed before test execution begins
    before: function() {
        // Init custom WDIO commands (ex. AEMLogin)
        require('./lib/wdio.commands');
        wdioDiagnostics.ensureLogPath();
    },

    beforeSuite: function (suite) {
        wdioDiagnostics.logSuiteStart(suite);
    },

    afterSuite: function (suite) {
        wdioDiagnostics.logSuiteEnd(suite);
    },

    beforeTest: function (test) {
        wdioDiagnostics.logTestStart(test);
    },

    // WDIO Hook executed after each test — logs END + final URL for every test; full DOM/console on failure
    afterTest: function (test, context, hookResult) {
        // Take a screenshot that will be attached in the HTML report
        commons.takeScreenshot(browser);
        const r = hookResult || {};
        let duration;
        if (typeof r.duration === 'number') {
            duration = r.duration;
        } else if (test && typeof test.duration === 'number') {
            duration = test.duration;
        } else {
            duration = undefined;
        }
        wdioDiagnostics.logTestComplete(browser, test, {
            passed: r.passed,
            duration: duration,
            error: r.error
        });
    },

    // Gets executed after each WDIO command
    beforeCommand: function (commandName) {
        // For WDIO commands which can lead into page navigation
        if (['url', 'refresh', 'click', 'call'].includes(commandName)) {
            // AEM Survey: only act when visible. Avoid unconditional refresh — it resets React/GraphQL
            // and was a major source of flaky commerce library tests.
            const survey = $('#omg_surveyContainer');
            if (survey.isExisting() && survey.isDisplayedInViewport()) {
                console.log(
                    '[UI-DIAG] AEM Survey dialog visible; dismissing with Escape (avoid refresh that resets React).'
                );
                browser.keys('Escape');
                try {
                    survey.waitForDisplayed({ reverse: true, timeout: 5000 });
                } catch (e) {
                    console.log('[UI-DIAG] Survey still visible after Escape; refreshing once as fallback.');
                    browser.refresh();
                }
            }
        }
    }
};
