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
// This is used to validate the queries against the Magento GraphQL schema
import { buildClientSchema, validate } from 'graphql';
import magentoSchema242ee from './magento-schema-2.4.2ee.json';

import fs from 'fs';
import path from 'path';

let files = fs.readdirSync(path.join(__dirname, '..')).filter(file => file.endsWith('.graphql.js')); // eslint-disable-line

describe.each([['2.4.2 EE', magentoSchema242ee]])(
    'Validate all GraphQL requests against Magento schema %s',
    (version, magentoSchema) => {
        beforeEach(() => {
            jest.resetModules();
        });

        expect(files.length).toBeGreaterThan(0); // Ensures we read the right folder
        let schema = buildClientSchema(magentoSchema.data);

        it.each(files)('validates the GraphQL request from %s', file => {
            let importedQuery = require(path.join(__dirname, '..', file)).default; // eslint-disable-line
            let errors = validate(schema, importedQuery);
            expect(errors).toHaveLength(0);
        });
    }
);
