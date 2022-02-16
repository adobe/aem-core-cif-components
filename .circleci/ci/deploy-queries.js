/**************************************************************************
 * ADOBE CONFIDENTIAL
 * ___________________
 *
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Adobe and its suppliers, if any. The intellectual
 * and technical concepts contained herein are proprietary to Adobe
 * and its suppliers and are protected by all applicable intellectual
 * property laws, including trade secret and copyright laws.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe.
 ***************************************************************************/

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
    ci.gitCredentials(ci.env('GITHUB_TOKEN'), '', () => {
        ci.gitImpersonate('CircleCI', 'no-reply@adobe.com', () => {
            ci.sh('git commit -m "releng - Update CIF Core Components Queries"');
            ci.sh(`git push --set-upstream origin master`);
        });
    });
});
