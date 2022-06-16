/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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
'use strict';

const { readFileSync, writeFileSync } = require('fs');

const ci = new (require('./ci.js'))();

const repo = 'commerce-cif-graphql-integration-reference';
const path = `schemas/components`;

ci.stage('Push changes to reference repo');

// Parse POM to get version
const { version } = ci.parsePom();

// Checkout public rep
ci.checkout(`https://github.com/adobe/${repo}.git`);

// Commit and push
ci.dir(repo, () => {
    // Copy query file
    const targetFile = `${path}/components-queries.log`;
    ci.sh(`mkdir -p ${path}`);
    ci.sh(`cp -r ../bundles/core/src/test/resources/test-queries/graphql-requests.log ${targetFile}`);
    sortFile(targetFile);
    ci.sh(`ls -aslh ${path}`);

    ci.sh(`git add ${targetFile}`);
    ci.sh('git status');

    // Determine if any changes need to be committed.
    let doCommit = true;
    const status = ci.sh('git status', true, false);
    if (status.indexOf('Changes to be committed') === -1) {
        console.log('No changes to commit.');
        doCommit = false;
    }

    const tagName = `components-queries-${version}`;
    ci.gitImpersonate('CircleCI', 'no-reply@adobe.com', () => {
        if (doCommit) {
            // Commit and push changes
            ci.sh(`git commit -m "releng - Update Queries for CIF Core Components v${version}"`);
            ci.sh(`git push --set-upstream origin master`);
        }
        // Tag latest commit with component release
        ci.sh(`git tag ${tagName} HEAD`);
        ci.sh(`git push origin ${tagName}`);
    });
});

function sortFile(filePath) {
    let data;

    try {
        data = readFileSync(filePath, 'UTF-8');
    } catch (err) {
        console.error(`Could not read query log file at ${filePath}`, err);
    }

    let lines = data.split(/\r?\n/);
    lines = lines
        // Remove empty lines
        .filter(line => line.trim().length > 0)
        // Sort alphabetically
        .sort();
    writeFileSync(filePath, lines.join('\n'), 'UTF-8');
}
