#!/usr/bin/env node
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
const fs = require('fs');
const path = require('path');
const { extractAndWrite, compileAndWrite } = require('@formatjs/cli');
const rimraf = require('rimraf');

function findFiles(dir, ext) {
    const results = [];
    if (!fs.existsSync(dir)) return results;
    for (const entry of fs.readdirSync(dir)) {
        const fullPath = path.join(dir, entry);
        const stat = fs.statSync(fullPath);
        if (stat.isDirectory()) {
            if (!entry.startsWith('.') && entry !== 'node_modules') {
                results.push(...findFiles(fullPath, ext));
            }
        } else if (ext.test(entry)) {
            results.push(fullPath);
        }
    }
    return results;
}

const rootDir = path.join(__dirname, '..');
const srcDir = path.join(rootDir, 'src');
const i18nDir = path.join(rootDir, 'i18n');
const tempFile = path.join(i18nDir, '__temp.json');
const outFile = path.join(i18nDir, 'en.json');

const files = findFiles(srcDir, /\.js$/);

if (files.length === 0) {
    console.log('No JS files found in src');
    process.exit(0);
}

async function run() {
    try {
        if (!fs.existsSync(i18nDir)) {
            fs.mkdirSync(i18nDir, { recursive: true });
        }
        await extractAndWrite(files, {
            outFile: tempFile,
            idInterpolationPattern: '[sha512:contenthash:base64:6]'
        });
        await compileAndWrite([tempFile], {
            outFile,
            ast: true
        });
        rimraf.sync(tempFile);
    } catch (err) {
        console.error(err);
        process.exit(1);
    }
}

run();
