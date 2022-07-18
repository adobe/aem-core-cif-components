/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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
package com.adobe.cq.commerce.core.components.internal.models.v1.experiencefragment;

import org.apache.sling.api.resource.Resource;

import com.adobe.cq.commerce.core.components.models.experiencefragment.CommerceExperienceFragmentContainer;

public class CommerceExperienceFragmentContainerImpl implements CommerceExperienceFragmentContainer {

    private Resource renderResource;
    private String cssClassName;

    public CommerceExperienceFragmentContainerImpl(Resource renderResource, String cssClassName) {
        this.renderResource = renderResource;
        this.cssClassName = cssClassName;
    }

    @Override
    public Resource getRenderResource() {
        return renderResource;
    }

    @Override
    public String getCssClassName() {
        return cssClassName;
    }

}
