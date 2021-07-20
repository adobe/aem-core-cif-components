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

const distFolder = path.join(__dirname, 'dist');
if (!fs.existsSync(distFolder)) {
    console.error('Could not find dist folder');
    return;
}

const templatesFolder = path.join(distFolder, 'css-api');
if (!fs.existsSync(templatesFolder)) {
    fs.mkdirSync(templatesFolder);
}

const files = fs.readdirSync(distFolder);
files
    .filter(f => f.endsWith('.css') && !f.endsWith('-template.css'))
    .forEach(f => {
        let cssPath = path.join(distFolder, f);

        // Clear css classes
        let css = fs.readFileSync(cssPath, { encoding: 'utf-8' });
        let emptyCss = css.replace(/\{([^{}]*)\}/gm, '{}');

        // Store in new file
        let { base } = path.parse(cssPath);
        let cssPathNew = path.join(templatesFolder, base);
        fs.writeFileSync(cssPathNew, emptyCss);
    });
