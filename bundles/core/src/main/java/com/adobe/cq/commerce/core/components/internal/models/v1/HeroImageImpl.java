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

package com.adobe.cq.commerce.core.components.internal.models.v1;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.adobe.cq.commerce.core.components.models.HeroImage;

@Model(adaptables = SlingHttpServletRequest.class, adapters = HeroImage.class, resourceType = HeroImageImpl.RESOURCE_TYPE)
public class HeroImageImpl implements HeroImage {

    protected static final String RESOURCE_TYPE = "core/cif/components/content/heroimage/v1/heroimage";
    private static final String PN_FULL_WIDTH = "useFullWidth";

    @Self
    private SlingHttpServletRequest request;

    @ScriptVariable
    private ValueMap properties;

    private String classList;
    private CifHeroImage image;

    @PostConstruct
    private void initModel() {
        classList = getClassList();
        com.adobe.cq.wcm.core.components.models.Image coreImage = request.adaptTo(com.adobe.cq.wcm.core.components.models.Image.class);
        if (image != null) {
            image = new CifHeroImage(image.getSrc());
        }
    }

    @Override
    public String getClassList() {
        if (classList != null) {
            return classList;
        }
        classList = "venia-HeroImage";
        if ("true".equals(properties.get(PN_FULL_WIDTH, ""))) {
            classList += " width-full";
        }
        return classList;
    }

    @Override
    public String getImageSrc() {
        if (image != null) {
            return image.getSrc();
        }
        com.adobe.cq.wcm.core.components.models.Image image = request.adaptTo(com.adobe.cq.wcm.core.components.models.Image.class);
        if (image != null) {
            this.image = new CifHeroImage(image.getSrc());
        }
        return this.image.getSrc();
    }

    public class CifHeroImage {
        private String src;

        public CifHeroImage(String src) {
            this.src = src;
        }

        public String getSrc() {
            return src;
        }
    }

}
