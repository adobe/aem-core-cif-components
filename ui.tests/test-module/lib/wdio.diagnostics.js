/*
 *  Copyright 2026 Adobe Systems Incorporated
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

/**
 * Structured diagnostics for UI tests. Writes to console and reports/ui-test-diagnostics.log.
 *
 * Timeline per run: SUITE START → START (test) → END (status, durationMs, URL, title, readyState)
 * On failure: ERROR + DOM snapshot + browser console tail.
 * Set UI_TEST_DIAG_VERBOSE=1 to include DOM snapshot + console on passed tests too.
 */
'use strict';

const fs = require('fs');
const path = require('path');
const conf = require('./config');

const LOG_NAME = 'ui-test-diagnostics.log';
let logPathResolved;

/** When UI_TEST_DIAG_VERBOSE=1, also capture DOM snapshot + browser console on passed tests. */
function isVerbose() {
    return process.env.UI_TEST_DIAG_VERBOSE === '1';
}

function ensureLogPath() {
    if (!logPathResolved) {
        const dir = path.resolve(conf.reports_path);
        if (!fs.existsSync(dir)) {
            fs.mkdirSync(dir, { recursive: true });
        }
        logPathResolved = path.join(dir, LOG_NAME);
    }
    return logPathResolved;
}

/**
 * @param {string} line
 */
function diag(line) {
    const msg = `[${new Date().toISOString()}] ${line}`;
    console.log(msg);
    try {
        fs.appendFileSync(ensureLogPath(), msg + '\n');
    } catch (e) {
        console.log('[UI-DIAG] appendFile failed:', e.message);
    }
}

/**
 * Step-by-step log from a spec file (console + ui-test-diagnostics.log), same format as diag.
 * @param {string} specId short id, e.g. 'product', 'product-bundle'
 * @param {string} message
 */
function logSpecStep(specId, message) {
    diag(`[spec:${specId}] ${message}`);
}

/**
 * @param {Mocha.Context} test
 */
function logTestStart(test) {
    const title = test.fullTitle ? test.fullTitle() : String(test);
    diag(`START ${title}`);
}

// Script strings run in the browser; `document` must not appear as a Node identifier (eslint no-undef).
const SCRIPT_READY_STATE = 'return document.readyState';
const SCRIPT_DOM_SNAPSHOT = [
    'return ({',
    'h1Count: document.querySelectorAll("h1").length,',
    'productRoots: document.querySelectorAll(".cmp-examples-demo__top .product").length,',
    'productFullDetail: document.querySelectorAll(".cmp-examples-demo__top .product .productFullDetail__root").length,',
    'carouselNext: document.querySelectorAll(".cmp-examples-demo__top .productcarousel .productcarousel__btn--next").length,',
    'teaserRoot: document.querySelectorAll(".cmp-examples-demo__top .productteaser .item__root").length,',
    'jsonLdScripts: document.querySelectorAll(\'script[type="application/ld+json"]\').length,',
    'surveyContainers: document.querySelectorAll("#omg_surveyContainer").length',
    '});'
].join('');

function collectPageSummary(browser) {
    try {
        diag(`URL ${browser.getUrl()}`);
    } catch (e) {
        diag(`URL (unavailable) ${e.message}`);
    }
    try {
        diag(`TITLE ${browser.getTitle()}`);
    } catch (e) {
        diag(`TITLE (unavailable) ${e.message}`);
    }
    try {
        const ready = browser.execute(SCRIPT_READY_STATE);
        diag(`document.readyState ${ready}`);
    } catch (e) {
        diag(`readyState (unavailable) ${e.message}`);
    }
}

function collectDomAndConsole(browser) {
    try {
        const snapshot = browser.execute(SCRIPT_DOM_SNAPSHOT);
        diag(`DOM snapshot ${JSON.stringify(snapshot)}`);
    } catch (e) {
        diag(`DOM snapshot (unavailable) ${e.message}`);
    }
    try {
        const logs = browser.getLogs('browser');
        const tail = Array.isArray(logs) ? logs.slice(-25) : [];
        diag(`browser console tail (${tail.length} entries) ${JSON.stringify(tail)}`);
    } catch (e) {
        diag(`browser logs (unavailable) ${e.message}`);
    }
}

/**
 * End-of-test line for every run (pass or fail): duration + final page state.
 * Failures also get ERROR + full DOM + console. Set UI_TEST_DIAG_VERBOSE=1 to capture
 * DOM + console on passes as well.
 *
 * @param {*} browser WDIO browser
 * @param {*} test Mocha test
 * @param {{ passed?: boolean, duration?: number, error?: Error }} hookResult
 */
function logTestComplete(browser, test, hookResult) {
    const title = test.fullTitle ? test.fullTitle() : String(test);
    const err = hookResult && hookResult.error;
    const duration = hookResult && typeof hookResult.duration === 'number' ? hookResult.duration : undefined;
    let passed = hookResult && hookResult.passed;
    if (passed === undefined) {
        passed = !err;
    }

    diag(
        `END ${title} status=${passed ? 'PASSED' : 'FAILED'} durationMs=${duration !== undefined ? duration : 'n/a'}`
    );

    collectPageSummary(browser);

    if (err) {
        if (err.message) {
            diag(`ERROR ${err.message}`);
        }
        collectDomAndConsole(browser);
    } else if (isVerbose()) {
        collectDomAndConsole(browser);
    }
}

function logSuiteStart(suite) {
    const file = suite && suite.file ? suite.file : '';
    const name = suite && suite.title ? suite.title : String(suite);
    diag(`SUITE START name=${name}${file ? ` file=${file}` : ''}`);
}

function logSuiteEnd(suite) {
    const file = suite && suite.file ? suite.file : '';
    const name = suite && suite.title ? suite.title : String(suite);
    diag(`SUITE END name=${name}${file ? ` file=${file}` : ''}`);
}

module.exports = {
    diag,
    logSpecStep,
    logTestStart,
    logTestComplete,
    logSuiteStart,
    logSuiteEnd,
    ensureLogPath
};
