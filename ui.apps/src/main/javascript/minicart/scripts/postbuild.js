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

const path = require('path');
const fs = require('fs');
const fsExtra = require('fs-extra');
const { exec } = require('child_process');

const CLIENTLIB_PATH = '../../content/jcr_root/apps/core/cif/components/commerce/minicart/v1/minicart/clientlib';
const SRC_PATH = path.resolve('.', 'dist');
const FILE_NAME = 'index';

const copySource = fs.copyFile(`${SRC_PATH}/${FILE_NAME}.js`, `${CLIENTLIB_PATH}/dist/${FILE_NAME}.js`, err => {
    if (err) {
        throw err;
    }
    console.error(`Output copied to ${CLIENTLIB_PATH}`);
});
const copyMap = fs.copyFile(`${SRC_PATH}/${FILE_NAME}.js.map`, `${CLIENTLIB_PATH}/dist/${FILE_NAME}.js.map`, err => {
    if (err) {
        throw err;
    }
    console.error(`Output copied to ${CLIENTLIB_PATH}`);
});
fsExtra.copy(`${SRC_PATH}/resources`, `${CLIENTLIB_PATH}/resources`, err => {
    if (err) {
        throw err;
    }
    console.error(`Output copied to ${CLIENTLIB_PATH}`);
});

exec(`repo put -f ${CLIENTLIB_PATH}`, (err, stdout, stderr) => {
    if (err) {
        console.log(err);
    } else {
        console.log(`stdout: ${stdout}`);
    }
});
