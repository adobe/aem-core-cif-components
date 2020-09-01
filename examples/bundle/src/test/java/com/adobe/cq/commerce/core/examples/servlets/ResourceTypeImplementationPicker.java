/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.examples.servlets;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.models.spi.ImplementationPicker;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = ImplementationPicker.class,
    immediate = true,
    property = {
        "service.ranking=1000"
    })
public class ResourceTypeImplementationPicker implements ImplementationPicker {

    // The default model picker takes the first available implementation when adapting to sling models
    // This fails for the RelatedProductsImpl model because it conflicts with ProductCarouselImpl when adapting to ProductCarousel
    // --> we hence select the type that best matches the resource type

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceTypeImplementationPicker.class);

    @Override
    public Class<?> pick(Class<?> adapterType, Class<?>[] implementationsTypes, Object adaptable) {
        if (implementationsTypes.length == 1) {
            return implementationsTypes[0];
        }

        // This is a special case only for the breadcrumb component to support @Via with ForcedResourceType
        if (!(adaptable instanceof MockSlingHttpServletRequest)) {
            for (int i = 0, l = implementationsTypes.length; i < l; i++) {
                Class<?> implementationsType = implementationsTypes[i];
                LOGGER.debug("... with " + implementationsType.getCanonicalName());
                if (implementationsType.getCanonicalName().contains("wcm")) {
                    LOGGER.debug("--> Returning " + implementationsType.getCanonicalName());
                    return implementationsType;
                }
            }
        }

        MockSlingHttpServletRequest request = (MockSlingHttpServletRequest) adaptable;
        String componentName = StringUtils.substringAfterLast(request.getResource().getResourceType(), "/");

        LOGGER.debug("Trying to adapt to " + request.getResource().getResourceType());

        for (int i = 0, l = implementationsTypes.length; i < l; i++) {
            Class<?> implementationsType = implementationsTypes[i];
            LOGGER.debug("... with " + implementationsType.getCanonicalName());

            if (implementationsType.getCanonicalName().contains(componentName)) {
                LOGGER.debug("--> Returning " + implementationsType.getCanonicalName());
                return implementationsType;
            }
        }

        return implementationsTypes[0];
    }

}
