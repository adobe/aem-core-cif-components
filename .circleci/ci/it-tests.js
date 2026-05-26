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
const { TYPE, BROWSER, AEM, COMMERCE_ENDPOINT, COMMERCE_INTEGRATION_TOKEN } = process.env;

const CORE_BUNDLE = 'com.adobe.commerce.cif.core-cif-components-core';
const ADDON_BUNDLE = 'com.adobe.cq.cif.commerce-addon-bundle';
const AEM_READY_TIMEOUT_MS = 360000;

// Wait for AEM HTTP, commerce add-on (classic/LTS), then install and activate project bundles.
const prepareAemForCifTests = () => {
    const needAddon = AEM === 'classic' || AEM === 'lts';
    const deadline = Date.now() + AEM_READY_TIMEOUT_MS;
    let attempt = 0;
    let projectBundlesInstalled = false;

    const getBundle = (bundlesJson, symbolicName) => {
        const parsed = JSON.parse(bundlesJson);
        const bundles = Array.isArray(parsed) ? parsed : (parsed.data || []);
        return bundles.find((entry) => entry.symbolicName === symbolicName || entry.name === symbolicName);
    };

    const isActive = (bundle) => bundle && (bundle.state === 'Active' || bundle.stateRaw === 32);

    while (Date.now() < deadline) {
        attempt++;
        try {
            const loginStatus = ci.sh(
                'curl -sf -o /dev/null -w "%{http_code}" -u admin:admin http://localhost:4502/libs/granite/core/content/login.html',
                true,
                false
            );
            if (loginStatus !== '200') {
                throw new Error(`login page returned HTTP ${loginStatus}`);
            }

            const bundlesJson = ci.sh('curl -sf -u admin:admin http://localhost:4502/system/console/bundles.json', true, false);

            if (needAddon && !isActive(getBundle(bundlesJson, ADDON_BUNDLE))) {
                throw new Error('commerce add-on bundle not Active');
            }

            if (!projectBundlesInstalled) {
                const installJar = (moduleKey) => {
                    const jarPath = ci.resolveModuleArtifactPath(config.modules[moduleKey]);
                    ci.sh(
                        `curl -sf -u admin:admin -F action=install -F bundlestart=1 -F bundlefile=@${jarPath} http://localhost:4502/system/console/bundles`,
                        false,
                        true
                    );
                };
                installJar('core-cif-components-core');
                installJar('core-cif-components-examples-bundle');
                projectBundlesInstalled = true;
                continue;
            }

            if (!isActive(getBundle(bundlesJson, CORE_BUNDLE))) {
                throw new Error('core-cif-components-core not Active');
            }

            console.log(`AEM ready for CIF tests after ${attempt} attempt(s).`);
            return;
        } catch (error) {
            console.log(`Waiting for AEM (attempt ${attempt}): ${error.message}`);
            if (Date.now() >= deadline - 5000) {
                break;
            }
            ci.sh('sleep 5', false, false);
        }
    }

    throw new Error(`Timed out after ${AEM_READY_TIMEOUT_MS / 1000}s waiting for AEM to be ready.`);
};

try {
    ci.stage("Integration Tests");
    let wcmVersion = ci.sh('mvn help:evaluate -Dexpression=core.wcm.components.version -q -DforceStdout', true);
    let magentoGraphqlVersion = ci.sh('mvn help:evaluate -Dexpression=magento.graphql.version -q -DforceStdout', true);
    let excludedCategory;
    if (AEM === 'classic') {
        excludedCategory = 'junit.category.IgnoreOn65';
    } else if (AEM === 'lts') {
        excludedCategory = 'junit.category.IgnoreOnLts';
    } else {
        excludedCategory = 'junit.category.IgnoreOnCloud';
    }

    // Build it/site with the appropriate profile
    ci.dir('it/site', () => {
        const profile = (AEM === 'classic' || AEM === 'lts') ? ' -Pclassic' : '';
        ci.sh(`mvn -B clean install${profile}`);
    });

    let itSitePackage = (AEM === 'classic' || AEM === 'lts')
        ? ci.addQpFileDependency(config.modules['cif-components-it-site.all-classic'])
        : ci.addQpFileDependency(config.modules['cif-components-it-site.all']);

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
            downloadArtifact('commerce-addon-aem-650-all', 'zip', 'addon.zip');
            extras += ` --install-file ${buildPath}/addon.zip`;
            extras += ` --bundle com.adobe.cq:core.wcm.components.all:${wcmVersion}:zip`;
        } else if (AEM === 'lts') {
            downloadArtifact('commerce-addon-aem-660-all', 'zip', 'addon.zip');
            extras += ` --install-file ${buildPath}/addon.zip`;
            extras += ` --bundle com.adobe.cq:core.wcm.components.all:${wcmVersion}:zip`;
        } else if (AEM === 'addon') {
            downloadArtifact('cif-cloud-ready-feature-pkg', 'far', 'addon.far', 'LATEST', 'cq-commerce-addon-authorfar');
            extras += ` --install-file ${buildPath}/addon.far`;
        }

        const maxMetaspace = '-XX:MaxMetaspaceSize=512m';
        // Start CQ (core and examples bundles are installed later via Felix when add-on is ready)
        ci.sh(`./qp.sh -v start --id author --runmode author --port 4502 --qs-jar /home/circleci/cq/author/cq-quickstart.jar \
            --bundle org.apache.sling:org.apache.sling.junit.core:1.0.23:jar \
            --bundle com.adobe.commerce.cif:magento-graphql:${magentoGraphqlVersion}:jar \
            ${extras} \
            --bundle com.adobe.cq:core.wcm.components.examples.ui.config:${wcmVersion}:zip \
            --bundle com.adobe.cq:core.wcm.components.examples.ui.apps:${wcmVersion}:zip \
            --bundle com.adobe.cq:core.wcm.components.examples.ui.content:${wcmVersion}:zip \
            ${ci.addQpFileDependency(config.modules['core-cif-components-config'])} \
            ${ci.addQpFileDependency(config.modules['core-cif-components-apps'])} \
            ${ci.addQpFileDependency(config.modules['core-cif-components-examples-config'])} \
            ${ci.addQpFileDependency(config.modules['core-cif-components-examples-apps'])} \
            ${ci.addQpFileDependency(config.modules['core-cif-components-examples-content'])} \
            ${ci.addQpFileDependency(config.modules['core-cif-components-it-tests-content'])} \
            ${itSitePackage} \
            --vm-options \\\"-Xmx1536m ${maxMetaspace} -Djava.awt.headless=true -javaagent:${process.env.JACOCO_AGENT}=destfile=crx-quickstart/jacoco-it.exec,output=tcpserver,port=6300\\\"`);
    });

    prepareAemForCifTests();

    // Configure GraphQL client for examples (allowInsecure on classic/LTS for http://localhost)
    const formData = {
        apply: true,
        factoryPid: 'com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl',
        action: 'ajaxConfigManager',
        url: 'http://localhost:4502/apps/cif-components-examples/graphql',
        httpMethod: 'GET',
        propertylist: 'url,httpMethod'
    };
    if (AEM === 'classic' || AEM === 'lts') {
        formData.allowInsecure = 'true';
        formData.propertylist = 'url,httpMethod,allowInsecure';
    }

    ci.sh(`curl 'http://localhost:4502/system/console/configMgr/com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl~examples' \
        -H 'Content-Type: application/x-www-form-urlencoded; charset=UTF-8' \
        -H 'Origin: http://localhost:4502' \
        -u 'admin:admin' \
        --data-raw '${Object.entries(formData)
        .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
        .join('&')}'`);

    // Run integration tests
    if (TYPE === 'integration') {
        const commerceEndpoint = COMMERCE_ENDPOINT ? `-DCOMMERCE_ENDPOINT="${COMMERCE_ENDPOINT}"` : '';
        const integrationToken = COMMERCE_INTEGRATION_TOKEN ? `-DCOMMERCE_INTEGRATION_TOKEN="${COMMERCE_INTEGRATION_TOKEN}"` : '';
        ci.dir('it/http', () => {
            ci.sh(`mvn clean verify -U -B \
                -Ptest-all \
                -Dexclude.category=${excludedCategory} \
                -Dsling.it.instance.url.1=http://localhost:4502 \
                -Dsling.it.instance.runmode.1=author \
                -Dsling.it.instances=1 \
                ${commerceEndpoint} \
                ${integrationToken}`);
        });
    }

    // Run UI tests
    if (TYPE === 'selenium') {
        ci.dir('ui.tests', () => {
            ci.sh(`mvn test -U -B -Pui-tests-local-execution -DHEADLESS_BROWSER=true -DSELENIUM-BROWSER=${BROWSER}`);
        });
    }

    // No coverage for UI tests
    if (TYPE !== 'selenium') {
        // Dump JaCoCo exec data via TCP while AEM is still running.
        // The agent uses output=tcpserver so data is only fully flushed through this dump —
        // relying on the file written during JVM shutdown is unreliable because AEM is often
        // killed (SIGKILL) after the stop timeout, which truncates the file mid-write.
        const dumpJacocoExec = () => {
            ci.sh(`mvn -B org.jacoco:jacoco-maven-plugin:${process.env.JACOCO_VERSION}:dump \
                -Djacoco.address=localhost -Djacoco.port=6300 \
                -Djacoco.destFile=jacoco-it.exec -Djacoco.append=false`);
        };
        ci.dir('bundles/core', dumpJacocoExec);
        ci.dir('examples/bundle', dumpJacocoExec);
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
        // Executing the integration tests also executes unit tests and generates a Jacoco report for them. To
        // strictly separate unit test from integration test coverage, we explicitly delete the unit test report first.
        ci.sh('rm -rf target/site/jacoco');

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
