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
const { spawnSync } = require('child_process');
const path = require('path');
const mode = process.argv[2] || 'production';
const webpackPath = require.resolve('webpack/bin/webpack.js');
// Node 17+ uses OpenSSL 3.0; Webpack 4 needs --openssl-legacy-provider
// Node 18+ blocks this flag in NODE_OPTIONS, so we pass it directly to node
// Node 16 uses OpenSSL 1.1 and doesn't need this flag
const majorVersion = parseInt(process.version.slice(1).split('.')[0], 10);
const needsLegacyProvider = majorVersion >= 17;
const args = needsLegacyProvider
    ? ['--openssl-legacy-provider', webpackPath, '--mode', mode]
    : [webpackPath, '--mode', mode];
const result = spawnSync(process.execPath, args, {
    stdio: 'inherit',
    cwd: path.resolve(__dirname, '..')
});
process.exit(result.status || 0);
