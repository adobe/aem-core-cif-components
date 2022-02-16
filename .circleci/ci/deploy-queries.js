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

const ci = new (require('./ci.js'))();

const repo = 'commerce-cif-graphql-integration-reference';
const path = `actions/resources/components`;

ci.stage('Push changes to reference repo');

// Parse POM to get version
const { version } = ci.parsePom();

// Checkout public rep
ci.checkout(`https://github.com/herzog31/${repo}.git`);

// Commit and push
ci.dir(repo, () => {
    // Copy query file
    const fileName = `components-queries-${version}.log`;
    ci.sh(`mkdir -p ${path}`);
    ci.sh(`cp -r ../bundles/core/src/test/resources/test-queries/graphql-requests.log ${path}/${fileName}`);
    ci.sh(`ls -aslh ${path}`);

    ci.sh(`git add actions/resources/components/${fileName}`);
    ci.sh('git status');

    // Abort early if there aren't any changes staged
    const status = ci.sh('git status', true, false);
    if (status.indexOf('Changes to be committed') === -1) {
        console.log('No changes to commit.');
        return;
    }

    // Commit and push changes
    ci.gitImpersonate('CircleCI', 'no-reply@adobe.com', () => {
        ci.sh('git commit -m "releng - Update CIF Core Components Queries"');
        ci.sh(`git push --set-upstream origin master`);
    });
});
