/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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
package com.adobe.cq.commerce.core.components.internal.storefrontcontext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.storefrontcontext.CommerceStorefrontContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AbstractCommerceStorefrontContext implements CommerceStorefrontContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCommerceStorefrontContext.class);

    @Override
    public String getJson() {
        try {
            return new ObjectMapper().writeValueAsString(this);

        } catch (JsonProcessingException e) {
            LOGGER.error("Unable to generate commerce schema JSON string", e);
        }
        return null;
    }
}
