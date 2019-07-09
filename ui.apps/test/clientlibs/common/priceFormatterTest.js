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

import PriceFormatter from '../../../src/main/content/jcr_root/apps/core/cif/clientlibs/common/js/PriceFormatter.js';

describe('PriceFormatter', () => {
    let locale = 'de-CH';

    it('initializes a PriceFormatter', () => {
        let formatter = new PriceFormatter(locale);

        assert.equal(formatter._locale, locale);
        assert.isNull(formatter._formatter);
    });

    it('formats a currency', () => {
        let formatter = new PriceFormatter(locale);

        let formattedPrice = formatter.formatPrice({ currency: 'CHF', value: 100.13 });
        assert.isNotNull(formatter._formatter);
        assert.equal(formattedPrice, 'CHF 100.13');

        // Formatter stores first given currency and will subsequently ignore
        // different currencies.
        formattedPrice = formatter.formatPrice({ currency: 'EUR', value: 25 });
        assert.equal(formattedPrice, 'CHF 25.00');
    });
});
