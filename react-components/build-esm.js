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
const path = require('path');
const { execSync } = require('child_process');
const fs = require('fs');

const srcDir = path.join(__dirname, 'src');
const outDir = path.join(__dirname, 'dist', 'esm');

// Ensure output directory exists
if (!fs.existsSync(path.join(__dirname, 'dist'))) {
    fs.mkdirSync(path.join(__dirname, 'dist'));
}
if (!fs.existsSync(outDir)) {
    fs.mkdirSync(outDir, { recursive: true });
}

// Run babel with ESM preset - preserve ES modules for tree-shaking
// Use --ignore to exclude test files (avoids .babelignore which would affect webpack/jest)
process.env.BABEL_ENV = 'esm';
execSync(
    `npx babel "${srcDir}" --out-dir "${outDir}" --copy-files --ignore "**/__test__/**" --ignore "**/__mocks__/**" --ignore "**/*.test.js" --ignore "**/*.spec.js"`,
    {
        stdio: 'inherit',
        cwd: __dirname
    }
);

console.log('ESM build complete: dist/esm/');
