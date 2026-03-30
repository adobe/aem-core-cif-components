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
const prettier = require('prettier');

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
const testDir = path.join(rootDir, 'test');
const ext = /\.(js|css)$/;

const files = [
    ...findFiles(srcDir, ext),
    ...(fs.existsSync(testDir) ? findFiles(testDir, ext) : [])
];

if (files.length === 0) {
    console.log('No files to check');
    process.exit(0);
}

const configPath = path.join(rootDir, '.prettierrc');
const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));

const write = process.argv.includes('--write');
let hasUnformatted = false;

for (const file of files) {
    const content = fs.readFileSync(file, 'utf8');
    const formatted = prettier.format(content, { ...config, filepath: file });
    if (content !== formatted) {
        if (write) {
            fs.writeFileSync(file, formatted);
        } else {
            console.log(file.replace(rootDir + path.sep, ''));
        }
        hasUnformatted = true;
    }
}

if (hasUnformatted && !write) {
    console.error('\nCode style issues found. Run "npm run prettier:fix" to fix.');
    process.exit(1);
}
