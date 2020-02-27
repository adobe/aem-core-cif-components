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

const fs = require('fs');
const path = require('path');

const languageFolder = './node_modules/@adobe/aem-core-cif-react-components/i18n/en';
const clientlibFolder = '../../content/jcr_root/apps/core/cif/clientlibs/react-components/resources/lang/en-US';

fs.mkdirSync(clientlibFolder, { recursive: true });
fs.readdirSync(languageFolder).forEach(file => {
    const src = path.join(languageFolder, file);
    const target = path.join(clientlibFolder, file);
    fs.copyFileSync(src, target);
    console.log('Copied', src, 'to', target);
});
