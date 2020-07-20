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

'use strict';

const ci = new (require('./ci.js'))();
const path = require('path');

ci.context();

ci.stage('Project Configuration');
const configuration = ci.collectConfiguration();
console.log(configuration);

ci.stage('Build Project');
ci.sh('mvn -B clean install');

ci.stage('Collect test results');
const testFolder = path.resolve(process.cwd(), 'test-results/junit');
ci.sh(`mkdir -p ${testFolder}`);
ci.sh(`find . -type f -regex ".*/target/surefire-reports/.*xml" -exec cp {} ${testFolder}/ \\;`);