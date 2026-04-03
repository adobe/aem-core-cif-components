/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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

const { execFileSync } = require('child_process');
const { URLSearchParams } = require('url');
const ci = new (require('./ci.js'))();

/** Same as ui.tests configureGraphqlClient + it/http CommerceTestBase — mock servlet must be allowed through SlingAuthenticator. */
function postSlingAuthenticatorForExamplesGraphql() {
    const params = new URLSearchParams();
    params.append('apply', 'true');
    params.append('action', 'ajaxConfigManager');
    params.append('_charset_', 'utf-8');
    params.append('propertylist', [
        'auth.sudo.cookie',
        'auth.sudo.parameter',
        'auth.annonymous',
        'sling.auth.requirements',
        'sling.auth.anonymous.user',
        'sling.auth.anonymous.password',
        'auth.http',
        'auth.http.realm',
        'auth.uri.suffix'
    ].join(','));
    params.append('auth.sudo.cookie', 'sling.sudo');
    params.append('auth.sudo.parameter', 'sudo');
    params.append('auth.annonymous', 'false');
    ['+/', '-/libs/granite/core/content/login', '-/etc.clientlibs', '-/etc/clientlibs/granite',
        '-/libs/dam/remoteassets/content/loginerror', '-/apps/cif-components-examples/graphql'
    ].forEach((entry) => params.append('sling.auth.requirements', entry));
    params.append('sling.auth.anonymous.user', '');
    params.append('sling.auth.anonymous.password', 'unmodified');
    params.append('auth.http', 'preemptive');
    params.append('auth.http.realm', 'Sling+(Development)');
    params.append('auth.uri.suffix', '/j_security_check');

    const body = params.toString();
    execFileSync('curl', [
        '-sS', '-u', 'admin:admin',
        '-X', 'POST',
        'http://localhost:4502/system/console/configMgr/org.apache.sling.engine.impl.auth.SlingAuthenticator',
        '-H', 'Content-Type: application/x-www-form-urlencoded; charset=UTF-8',
        '-H', 'Origin: http://localhost:4502',
        '--data-binary', body
    ], { stdio: 'inherit' });
}

ci.context();

ci.stage('Project Configuration');
const config = ci.restoreConfiguration();
console.log(config);
const qpPath = '/home/circleci/cq';
const buildPath = '/home/circleci/build';
const { TYPE, BROWSER, AEM } = process.env;

try {
    ci.stage(`AEM IT (${TYPE}, AEM=${AEM})`);
    let wcmVersion = ci.sh('mvn help:evaluate -Dexpression=core.wcm.components.version -q -DforceStdout', true);
    let magentoGraphqlVersion = ci.sh('mvn help:evaluate -Dexpression=magento.graphql.version -q -DforceStdout', true);
    let excludedCategory = AEM === 'classic' ? 'junit.category.IgnoreOn65' : 'junit.category.IgnoreOnCloud';

    // TODO: Remove when https://jira.corp.adobe.com/browse/ARTFY-6646 is resolved
    let aemCifSdkApiVersion = "2025.12.04.00";


    ci.dir(qpPath, () => {
        // Connect to QP
        ci.sh('./qp.sh -v bind --server-hostname localhost --server-port 55555');
        
        // Download latest add-on release from artifactory
        let extras = '';
        const downloadArtifact = (artifactId, type, outputFileName, version = 'LATEST', classifier = '') => {
            const classifierOption = classifier ? `-Dclassifier=${classifier}` : '';
            ci.sh(`mvn -s ${buildPath}/.circleci/settings.xml com.googlecode.maven-download-plugin:download-maven-plugin:1.6.3:artifact -Partifactory-cloud -DgroupId=com.adobe.cq.cif -DartifactId=${artifactId} -Dversion=${version} -Dtype=${type} ${classifierOption} -DoutputDirectory=${buildPath} -DoutputFileName=${outputFileName}`);
        };

        if (AEM === 'classic') {
            extras += ` --install-file ${buildPath}/addon.zip`;
            downloadArtifact('commerce-addon-aem-650-all', 'zip', 'addon.zip', aemCifSdkApiVersion);
            extras += ` --bundle com.adobe.cq:core.wcm.components.all:${wcmVersion}:zip`;
        } else if (AEM === 'lts') {
            extras += ` --install-file ${buildPath}/addon.zip`;
            downloadArtifact('commerce-addon-aem-660-all', 'zip', 'addon.zip', aemCifSdkApiVersion);
            extras += ` --bundle com.adobe.cq:core.wcm.components.all:${wcmVersion}:zip`;
        } else if (AEM === 'addon') {
            extras += ` --install-file ${buildPath}/addon.far`;
            downloadArtifact('cif-cloud-ready-feature-pkg', 'far', 'addon.far', 'LATEST', 'cq-commerce-addon-authorfar');
        }

        // LTS needs more heap than 6.5, but very large heap + metaspace starves Chrome/selenium on typical CI RAM (8 GiB).
        const jvmHeap = AEM === 'lts' ? '-Xmx2048m' : '-Xmx1536m';
        // LTS loads more bundles than 6.5; 640m reduces metaspace churn vs 512m without matching a huge -Xmx that starves Chrome.
        const maxMetaspace = AEM === 'lts' ? '-XX:MaxMetaspaceSize=640m' : '-XX:MaxPermSize=256m';
        // Start CQ
        ci.sh(`./qp.sh -v start --id author --runmode author --port 4502 --qs-jar /home/circleci/cq/author/cq-quickstart.jar \
            --bundle org.apache.sling:org.apache.sling.junit.core:1.0.23:jar \
            --bundle com.adobe.commerce.cif:magento-graphql:${magentoGraphqlVersion}:jar \
            --bundle com.adobe.cq:core.wcm.components.examples.ui.config:${wcmVersion}:zip \
            --bundle com.adobe.cq:core.wcm.components.examples.ui.apps:${wcmVersion}:zip \
            --bundle com.adobe.cq:core.wcm.components.examples.ui.content:${wcmVersion}:zip \
            ${extras} \
            ${ci.addQpFileDependency(config.modules['core-cif-components-apps'])} \
            ${ci.addQpFileDependency(config.modules['core-cif-components-config'])} \
            ${ci.addQpFileDependency(config.modules['core-cif-components-core'])} \
            ${ci.addQpFileDependency(config.modules['core-cif-components-examples-bundle'])} \
            ${ci.addQpFileDependency(config.modules['core-cif-components-examples-apps'])} \
            ${ci.addQpFileDependency(config.modules['core-cif-components-examples-config'])} \
            ${ci.addQpFileDependency(config.modules['core-cif-components-examples-content'])} \
            ${ci.addQpFileDependency(config.modules['core-cif-components-it-tests-content'])} \
            --vm-options \\\"${jvmHeap} ${maxMetaspace} -Djava.awt.headless=true -javaagent:${process.env.JACOCO_AGENT}=destfile=crx-quickstart/jacoco-it.exec\\\"`);
    });

    // LTS (6.6): login.html can return 200 while commerce OSGi / GraphQL / example bundles are still starting — a single
    // fast 200 (attempt 1) is a common flake pattern. Require two consecutive OK polls, then a fixed settle before UI tests.
    if (AEM === 'lts') {
        ci.stage('Wait for stable AEM HTTP (LTS)');
        const waitLogin = [
            'ok=0',
            'i=0',
            'while [ "$i" -lt 72 ]; do',
            '  i=$((i + 1))',
            '  code=$(curl -s -o /dev/null -w "%{http_code}" -u admin:admin http://localhost:4502/libs/granite/core/content/login.html) || code=000',
            '  if [ "$code" = "200" ] || [ "$code" = "302" ]; then',
            '    ok=$((ok + 1))',
            '    echo "AEM HTTP $code (consecutive OK: $ok, poll $i/72)"',
            '    if [ "$ok" -ge 2 ]; then echo "AEM login endpoint stable"; break; fi',
            '  else',
            '    ok=0',
            '  fi',
            '  sleep 10',
            'done',
            'if [ "$ok" -lt 2 ]; then echo "AEM login did not stabilize in time"; exit 1; fi',
            'echo "LTS settle: OSGi / commerce / GraphQL (90s)..."',
            'sleep 90'
        ].join('\n');
        execFileSync('bash', ['-lc', waitLogin], { stdio: 'inherit' });
    }

    // Match ui.tests commons.configureExamplesGraphqlClient — http:// localhost requires allowHttpProtocol on recent graphql-client.
    const formData = {
        apply: true,
        factoryPid: 'com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl',
        action: 'ajaxConfigManager',
        identifier: 'examples',
        url: 'http://localhost:4502/apps/cif-components-examples/graphql',
        httpMethod: 'GET',
        allowHttpProtocol: 'true',
        acceptSelfSignedCertificates: 'true',
        propertylist: 'identifier,url,httpMethod,allowHttpProtocol,acceptSelfSignedCertificates'
    };

    ci.sh(`curl -sS 'http://localhost:4502/system/console/configMgr/com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl~examples' \
        -H 'Content-Type: application/x-www-form-urlencoded; charset=UTF-8' \
        -H 'Origin: http://localhost:4502' \
        -u 'admin:admin' \
        --data-raw '${Object.entries(formData)
        .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
        .join('&')}'`);

    ci.stage('SlingAuthenticator (mock GraphQL path)');
    postSlingAuthenticatorForExamplesGraphql();

    if (AEM === 'lts') {
        execFileSync('bash', ['-lc', 'echo "Post OSGi (GraphQL + Authenticator) settle (60s)..."; sleep 60'], { stdio: 'inherit' });
    }

    // LTS: HTTP 200 on login is not enough — commerce pages compile HTL that references core Sling Models. If the core
    // bundle is not fully active, the first requests throw SightlyException (e.g. StoreConfigExporter unresolved) and ITs fail.
    if (AEM === 'lts') {
        ci.stage('Wait for commerce example page (HTL / core bundle ready)');
        const waitCommercePage = [
            'i=0',
            'while [ "$i" -lt 24 ]; do',
            '  i=$((i + 1))',
            '  code=$(curl -s -o /dev/null -w "%{http_code}" -u admin:admin "http://localhost:4502/content/core-components-examples/library/commerce/productteaser.html") || code=000',
            '  echo "Commerce library page HTTP $code (poll $i/24)"',
            '  if [ "$code" = "200" ]; then echo "Commerce example page ready"; exit 0; fi',
            '  sleep 15',
            'done',
            'echo "Commerce example page did not return 200 in time"; exit 1'
        ].join('\n');
        execFileSync('bash', ['-lc', waitCommercePage], { stdio: 'inherit' });
    }

    // Run integration tests (Java Sling HTTP ITs). Same LTS retry policy as UI tests — transient AEM/network flakes.
    if (TYPE === 'integration') {
        const mvnIt = `mvn clean verify -U -B \
                -Ptest-all \
                -Dexclude.category=${excludedCategory} \
                -Dsling.it.instance.url.1=http://localhost:4502 \
                -Dsling.it.instance.runmode.1=author \
                -Dsling.it.instances=1`;
        const maxItAttempts = AEM === 'lts' ? 3 : 1;
        ci.dir('it/http', () => {
            for (let attempt = 1; attempt <= maxItAttempts; attempt++) {
                try {
                    ci.sh(mvnIt);
                    return;
                } catch (err) {
                    if (attempt >= maxItAttempts) {
                        throw err;
                    }
                    ci.stage(`Integration tests failed (attempt ${attempt}/${maxItAttempts}) — retry after 90s`);
                    execFileSync('bash', ['-lc', 'sleep 90'], { stdio: 'inherit' });
                }
            }
        });
    }
    
    // Run UI tests
    if (TYPE === 'selenium') {
        // Get version of ChromeDriver
        let chromedriver = ci.sh('chromedriver --version', true); // Returns something like ChromeDriver 80.0.3987.16 (320f6526c1632ad4f205ebce69b99a062ed78647-refs/branch-heads/3987@{#185})
        chromedriver = chromedriver.split(' ');
        chromedriver = chromedriver.length >= 2 ? chromedriver[1] : '';

        ci.dir('ui.tests', () => {
            // LTS: WDIO sometimes dies immediately after "Execution of N workers started" (Chrome/Node OOM or selenium-standalone race).
            // Extra Node heap + a few retries with backoff usually clears it without lengthening successful runs.
            const nodeOpts = AEM === 'lts' ? 'NODE_OPTIONS=--max-old-space-size=4096 ' : '';
            const mvnUi = `${nodeOpts}CHROMEDRIVER=${chromedriver} mvn test -U -B -Pui-tests-local-execution -DHEADLESS_BROWSER=true -DSELENIUM-BROWSER=${BROWSER}`;
            const maxAttempts = AEM === 'lts' ? 3 : 1;
            for (let attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    ci.sh(mvnUi);
                    return;
                } catch (err) {
                    if (attempt >= maxAttempts) {
                        throw err;
                    }
                    ci.stage(`UI tests failed (attempt ${attempt}/${maxAttempts}) — retry after backoff`);
                    // LTS: immediate WDIO exit on retry is common; drop stale Chromedriver before cool-down.
                    if (AEM === 'lts') {
                        execFileSync('bash', ['-lc', 'pkill -f chromedriver 2>/dev/null || true'], { stdio: 'inherit' });
                        execFileSync('bash', ['-lc', 'sleep 30'], { stdio: 'inherit' });
                    }
                    execFileSync('bash', ['-lc', 'sleep 90'], { stdio: 'inherit' });
                }
            }
        });
    }
    
    ci.dir(qpPath, () => {
        // Stop CQ
        ci.sh('./qp.sh -v stop --id author');
    });
    
    // No coverage for UI tests
    if (TYPE === 'selenium') {
        return;
    }
    
    // Create coverage reports
    const createCoverageReport = () => {
        // Executing the integration tests runs also executes unit tests and generates a Jacoco report for them. To 
        // strictly separate unit test from integration test coverage, we explicitly delete the unit test report first.
        ci.sh('rm -rf target/site/jacoco');

        // Download Jacoco file which is exposed by a webserver running inside the AEM container.
        ci.sh('curl -O -f http://localhost:3000/crx-quickstart/jacoco-it.exec');

        // Generate new report
        ci.sh(`mvn -B org.jacoco:jacoco-maven-plugin:${process.env.JACOCO_VERSION}:report -Djacoco.dataFile=jacoco-it.exec`);

        // Upload report to codecov
        ci.sh('curl -s https://codecov.io/bash | bash -s -- -c -F integration -f target/site/jacoco/jacoco.xml');
    };

    ci.dir('bundles/core', createCoverageReport);
    ci.dir('examples/bundle', createCoverageReport);

} finally { // Always download logs from AEM container
    ci.sh('mkdir logs');
    ci.dir('logs', () => {
        // A webserver running inside the AEM container exposes the logs folder, so we can download log files as needed.
        ci.sh('curl -O -f http://localhost:3000/crx-quickstart/logs/error.log');
        ci.sh('curl -O -f http://localhost:3000/crx-quickstart/logs/stdout.log');
        ci.sh('curl -O -f http://localhost:3000/crx-quickstart/logs/stderr.log');
        ci.sh(`find . -name '*.log' -type f -size +32M -exec echo 'Truncating: ' {} \\; -execdir truncate --size 32M {} +`);
    });
}
