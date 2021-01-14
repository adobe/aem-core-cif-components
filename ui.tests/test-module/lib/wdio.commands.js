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
const fs = require('fs');
const path = require('path');
const request = require('request-promise');
const url = require('url');
const config = require('./config');
const commons = require('./commons');
const errors = require('request-promise/errors');

const AEM_SITES_PATH = '/sites.html';

browser.addCommand('AEMLogin', function (username, password) {
    // Check presence of local sign-in Accordion
    if ($('[class*="Accordion"] form').isExisting()) {
        try {
            $('#username').setValue(username);
        }
        // Form field not interactable, not visible
        // Need to open the Accordion
        catch (e) {
            $('[class*="Accordion"] button').click();
            browser.pause(500);
        }
    }

    $('#username').setValue(username);
    $('#password').setValue(password);

    $('form [type="submit"]').click();

    $('coral-shell-content').waitForExist(5000);
});

browser.addCommand('AEMForceLogout', function () {
    browser.url('/');

    if (browser.getTitle() != 'AEM Sign In') {
        browser.url('/system/sling/logout.html');
    }

    $('form[name="login"]').waitForExist();
});

browser.addCommand('configureGraphqlClient', async function (factoryPid, properties) {
    const auth = commons.getAuthenticatedRequestOptions(browser);

    // Update OSGi config of GraphQL client
    const configurations = await getOsgiConfigurations(auth, factoryPid);
    const pid = configurations.length > 0 ? configurations[0].pid : '';
    await editOsgiConfiguration(auth, pid, factoryPid, properties);

    // Update OSGi config of Sling Authenticator to make GraphQL servlet reachable
    await editOsgiConfiguration(auth, 'org.apache.sling.engine.impl.auth.SlingAuthenticator', null, {
        'auth.sudo.cookie': 'sling.sudo',
        'auth.sudo.parameter': 'sudo',
        'auth.annonymous': 'false',
        'sling.auth.requirements': [
            '+/',
            '-/libs/granite/core/content/login',
            '-/etc.clientlibs',
            '-/etc/clientlibs/granite',
            '-/libs/dam/remoteassets/content/loginerror',
            '-/apps/cif-components-examples/graphql'
        ],
        'sling.auth.anonymous.user': '',
        'sling.auth.anonymous.password': 'unmodified',
        'auth.http': 'preemptive',
        'auth.http.realm': 'Sling+(Development)',
        'auth.uri.suffix': '/j_security_check'
    });
});

// Returns file handle to use for file upload component,
// depending on test context (local, Docker or Cloud)
browser.addCommand('getFileHandleForUpload', function (filePath) {
    return browser.call(() => {
        return fileHandle(filePath);
    });
});

browser.addCommand('AEMPathExists', function (baseUrl, path) {
    let options = commons.getAuthenticatedRequestOptions(browser);
    Object.assign(options, {
        method: 'GET',
        uri: url.resolve(baseUrl, path)
    });

    return request(options)
        .then(function () {
            return true;
        })
        .catch(errors.StatusCodeError, function (reason) {
            if (reason.statusCode == 404) {
                return false;
            }
        });
});

browser.addCommand('AEMDeleteAsset', function (assetPath) {
    let options = commons.getAuthenticatedRequestOptions(browser);
    Object.assign(options, {
        formData: {
            cmd: 'deletePage',
            path: assetPath,
            force: 'true',
            '_charset_': 'utf-8'
        }
    });

    return request.post(url.resolve(config.aem.author.base_url, '/bin/wcmcommand'), options);
});

browser.addCommand('AEMSitesSetView', function (type) {
    if (!Object.values(commons.AEMSitesViewTypes).includes(type)) {
        throw new Error(`View type ${type} is not supported`);
    }

    browser.url(AEM_SITES_PATH);

    browser.setCookies({
        name: 'cq-sites-pages-pages',
        value: type
    });

    browser.refresh();
});

browser.addCommand('AEMSitesSetPageTitle', function (parentPath, name, title) {
    let originalTitle = '';

    // Navigate to page parent path
    browser.url(path.posix.join(AEM_SITES_PATH, parentPath));
    // Select sample page in the list
    $(`[data-foundation-collection-item-id="${path.posix.join(parentPath, name)}"] [type="checkbox"]`).waitForClickable();
    browser.pause(1000); // Avoid action bar not appearing after clicking checkbox
    $(`[data-foundation-collection-item-id="${path.posix.join(parentPath, name)}"] [type="checkbox"]`).click();
    // Access page properties form
    $('[data-foundation-collection-action*="properties"]').click();
    // Store original title
    originalTitle = $('[name="./jcr:title"]').getValue();
    // Modify title
    $('[name="./jcr:title"]').setValue(title);
    // Submit
    $('[type="submit"]').click();
    // Wait until we get redirected to the Sites console
    $(`[data-foundation-collection-item-id="${path.posix.join(parentPath, name)}"] [type="checkbox"]`).waitForExist();

    return originalTitle;
});

async function getOsgiConfigurations(auth, factoryPid) {
    const options = { ...auth, method: 'GET', uri: url.resolve(config.aem.author.base_url, '/system/console/configMgr/*.json'), json: true };

    const configurations = await request(options);
    const configuration = configurations.filter(c => c.factoryPid === factoryPid);

    return configuration;
}

async function editOsgiConfiguration(auth, pid, factoryPid, properties) {
    const form = {
        apply: 'true',
        action: 'ajaxConfigManager',
        propertylist: Object.keys(properties).join(','),
        _charset_: 'utf-8',
        ...properties
    };
    if (factoryPid) {
        form.factoryPid = factoryPid;
    }

    const options = { ...auth, method: 'POST', uri: url.resolve(config.aem.author.base_url, `/system/console/configMgr/${pid}`), form, simple: false, resolveWithFullResponse: true, useQuerystring: true };
    const { statusCode } = await request(options);
    expect(statusCode).toEqual(302);
}

async function fileHandle(filePath) {
    if (config.upload_url) {
        return fileHandleByUploadUrl(config.upload_url, filePath);
    }
    if (config.shared_folder) {
        return fileHandleBySharedFolder(config.shared_folder, filePath);
    }
    return filePath;
}

function fileHandleBySharedFolder(sharedFolderPath, filePath) {
    const sharedFilePath = path.join(sharedFolderPath, path.basename(filePath));
    fs.copyFileSync(filePath, sharedFilePath);
    return sharedFilePath;
}

function fileHandleByUploadUrl(uploadUrl, filePath) {
    return request.post(uploadUrl, {
        formData: {
            data: {
                value: fs.createReadStream(filePath),
                options: {
                    filename: path.basename(filePath)
                }
            },
        },
    });
}
