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

const ci = new (require('./ci.js'))();

ci.context();

ci.stage('Project Configuration');
const config = ci.restoreConfiguration();
console.log(config);
const qpPath = '/home/circleci/cq';
const buildPath = '/home/circleci/build';
const { TYPE, BROWSER, AEM } = process.env;

try {
    ci.stage("Integration Tests");
    let wcmVersion = ci.sh('mvn help:evaluate -Dexpression=core.wcm.components.version -q -DforceStdout', true);
    let magentoGraphqlVersion = ci.sh('mvn help:evaluate -Dexpression=magento.graphql.version -q -DforceStdout', true);
    let excludedCategory = AEM === 'classic' ? 'junit.category.IgnoreOn65' : 'junit.category.IgnoreOnCloud';

    // TODO: Remove when https://jira.corp.adobe.com/browse/ARTFY-6646 is resolved
    let aemCifSdkApiVersion = "2025.09.02.1-SNAPSHOT";


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

        // LTS (Java 21 / 6.6) needs more metaspace + heap than classic 6.5 (PermGen) for quickstart + CIF + WCM.
        const jvmHeap = AEM === 'lts' ? '-Xmx2048m' : '-Xmx1536m';
        const maxMetaspace = AEM === 'lts' ? '-XX:MaxMetaspaceSize=1024m' : '-XX:MaxPermSize=256m';
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

    // AEM 6.6 LTS (660 addon, Java 21) is slower to reach full readiness than 6.5 / Cloud: OSGi bundles, HTL,
    // and the GraphQL servlet may not exist until minutes after qp reports "started". Posting GraphQL OSGi
    // config too early yields 404 on /apps/cif-components-examples/graphql and broken pages (StoreConfigExporter / empty DOM).
    if (AEM === 'lts') {
        ci.stage('Wait for AEM HTTP (LTS 6.6)');
        // Do not use `set -e` here: with command substitutions and `||`, bash can exit non-zero even after a
        // successful `exit 0` path (observed in CI: "ready after attempt 1" then process exit 1).
        const aemWaitScript = [
            'i=0',
            'while [ "$i" -lt 60 ]; do',
            '  i=$((i + 1))',
            '  code=$(curl -s -o /dev/null -w "%{http_code}" -u admin:admin http://localhost:4502/libs/granite/core/content/login.html) || code=000',
            '  if [ "$code" = "200" ] || [ "$code" = "302" ]; then',
            '    echo "AEM login page ready after attempt $i (HTTP $code)"',
            '    exit 0',
            '  fi',
            '  echo "Waiting for AEM (attempt $i/60, HTTP $code)..."',
            '  sleep 10',
            'done',
            'echo "AEM did not become ready in time"',
            'exit 1'
        ].join('\n');
        ci.sh('bash -lc ' + JSON.stringify(aemWaitScript));
        ci.sh(
            'bash -lc ' +
                JSON.stringify('echo "LTS settle time for bundles / HTL / GraphQL (45s)..."; sleep 45')
        );
    }

    // Temporary fix for integration & selenium test
    const formData = {
        apply: true,
        factoryPid: 'com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl',
        action: 'ajaxConfigManager',
        url: "http://localhost:4502/apps/cif-components-examples/graphql",
        httpMethod: 'GET',
        propertylist: 'url,httpMethod'
    };

    const graphqlConfigPayload = Object.entries(formData)
        .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
        .join('&');

    // -f: fail on HTTP error — used for LTS retries so we re-post if config endpoint was not ready.
    const graphqlCurlStrict =
        'curl -sS -f -o /dev/null ' +
        '\'http://localhost:4502/system/console/configMgr/com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl~examples\' ' +
        '-H \'Content-Type: application/x-www-form-urlencoded; charset=UTF-8\' ' +
        '-H \'Origin: http://localhost:4502\' ' +
        '-u \'admin:admin\' ' +
        '--data-raw \'' + graphqlConfigPayload + '\'';

    if (AEM === 'lts') {
        ci.stage('Apply GraphQL OSGi config (LTS retries)');
        let graphqlOk = false;
        for (let attempt = 1; attempt <= 5; attempt++) {
            try {
                console.log(`GraphQL OSGi config attempt ${attempt}/5`);
                ci.sh(graphqlCurlStrict);
                graphqlOk = true;
                console.log('GraphQL OSGi config POST succeeded');
                break;
            } catch (e) {
                console.log(`GraphQL OSGi config attempt ${attempt} failed: ${e.message}`);
                if (attempt < 5) {
                    ci.sh('sleep 20');
                }
            }
        }
        if (!graphqlOk) {
            throw new Error('GraphQL OSGi config failed after 5 attempts (LTS)');
        }
    } else {
        ci.sh(`curl 'http://localhost:4502/system/console/configMgr/com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl~examples' \
        -H 'Content-Type: application/x-www-form-urlencoded; charset=UTF-8' \
        -H 'Origin: http://localhost:4502' \
        -u 'admin:admin' \
        --data-raw '${graphqlConfigPayload}'`);
    }



    // Run integration tests
    if (TYPE === 'integration') {
        ci.dir('it/http', () => {
            ci.sh(`mvn clean verify -U -B \
                -Ptest-all \
                -Dexclude.category=${excludedCategory} \
                -Dsling.it.instance.url.1=http://localhost:4502 \
                -Dsling.it.instance.runmode.1=author \
                -Dsling.it.instances=1`);
        });
    }
    
    // Run UI tests
    if (TYPE === 'selenium') {
        // selenium-standalone must use a ChromeDriver build matching the *Chrome binary* under test.
        // CircleCI browser-tools often installs `google-chrome-stable` only; LTS qp (openjdk21) may have no
        // `google-chrome` symlink, so the old probe returned empty and we fell back to PATH chromedriver
        // (wrong major vs installed Chrome) — WDIO then exits immediately after starting workers.
        let driverVersion = '';
        // Use --product-version only (one line, e.g. 130.0.6723.100). Do not use --version here: its text
        // can confuse parsing and is unnecessary once product-version exists on Chrome for Testing builds.
        const versionProbe = ci.sh(
            'sh -c \'for c in google-chrome google-chrome-stable chromium chromium-browser; do ' +
                'if command -v "$c" >/dev/null 2>&1; then ' +
                'o=$("$c" --product-version 2>/dev/null); [ -n "$o" ] && echo "$o" && exit 0; ' +
                'fi; done; exit 0\'',
            true,
            false
        );
        let verMatch = versionProbe.match(/(\d+\.\d+\.\d+\.\d+)/);
        if (!verMatch) {
            verMatch = versionProbe.match(/(\d+\.\d+\.\d+)/);
        }
        if (verMatch) {
            driverVersion = verMatch[1];
        } else {
            let chromedriver = ci.sh('chromedriver --version', true, false);
            chromedriver = chromedriver.split(' ');
            driverVersion = chromedriver.length >= 2 ? chromedriver[1] : '';
        }
        console.log(
            'UI tests: Chrome version probe =>',
            JSON.stringify(versionProbe),
            'using CHROMEDRIVER=',
            driverVersion || '(none; selenium-standalone default)'
        );

        ci.dir('ui.tests', () => {
            const prefix = driverVersion ? `CHROMEDRIVER=${driverVersion} ` : '';
            // Give OSGi more time after GraphQL config before browser tests (LTS 6.6 only).
            const ltsPause =
                AEM === 'lts' ? '-DAEM_LTS_UI_EXTRA_PAUSE_MS=20000 ' : '';
            ci.sh(
                `${prefix}mvn test -U -B -Pui-tests-local-execution ${ltsPause}-DHEADLESS_BROWSER=true -DSELENIUM-BROWSER=${BROWSER}`
            );
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
