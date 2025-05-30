/*******************************************************************************
 *
 *    Copyright 2025 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.cacheinvalidation.internal;

import java.util.Arrays;
import java.util.List;

import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.core.cacheinvalidation.spi.CacheInvalidationStrategy;

@Component(
    service = CacheInvalidationStrategy.class)
public class RegexPatternsInvalidateCache implements CacheInvalidationStrategy {

    @Override
    public List<String> getPatterns(String[] invalidationParameters) {
        return Arrays.asList(invalidationParameters);
    }

    @Override
    public String getInvalidationType() {
        return "regexPatterns";
    }
}
