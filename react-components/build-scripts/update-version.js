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
const util = require('util');
const exec = util.promisify(require('child_process').exec);
const semver = require('semver');

//console.log = process.stdout;
let version;
//1. process command line arguments - the version
process.argv.slice(2).forEach(arg => {
    if (arg.startsWith('--version')) {
        version = arg.substr(arg.indexOf('=') + 1, arg.length);
    }
});

if (!version) {
    console.error(`Version argument not supplied, nothing to do`);
    process.exit(1);
}

if (semver.valid(version)) {
    console.log(`Version is valid`);
} else {
    console.error(`${version} is not a valid semver version`);
    process.exit(1);
}

/**
 * Sets the version of this package by calling `npm version` (also updates package-lock.json)
 * @param {String} version
 * @returns a Promise
 */
async function npmVersion(version) {
    const command = `npm version ${version} --allow-same-version`;
    return execCommand(command);
}

/**
 * Performs a Git commit for `package.json` and `package-lock.json`
 * @param {String} version
 */
async function scmCommit(version) {
    const commitMessage = ` @releng [maven-scm] : Update react-componenst to version ${version}`;
    await execCommand(`git add package.json package-lock.json`);
    await execCommand(`git commit -m "${commitMessage}"`);
}

/**
 * Executes a command in the terminal and handles errors (if any)
 *
 * @param {String} command
 * @returns a Promise object
 */
async function execCommand(command) {
    console.log(`Executing ${command}`);
    return await exec(command).catch(({stdout}) => {
        console.error(stdout);
    });
}

//2. Bump the version in package.json. We use 'npm version' because it automatically updates package-lock.json
npmVersion(version).then(() => {
    //3. Add / commit package*.json files
    scmCommit(version);
});
