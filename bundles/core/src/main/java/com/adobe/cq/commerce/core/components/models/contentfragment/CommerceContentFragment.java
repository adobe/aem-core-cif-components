/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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
package com.adobe.cq.commerce.core.components.models.contentfragment;

import com.adobe.cq.wcm.core.components.models.contentfragment.ContentFragment;
import com.adobe.cq.wcm.core.components.models.contentfragment.ContentFragmentList;

/**
 * Sling model for the Commerce Content Fragment component.
 */
public interface CommerceContentFragment extends ContentFragment {

    /**
     * Property name to define the content fragment model of the displayed content fragment.
     */
    String PN_MODEL_PATH = ContentFragmentList.PN_MODEL_PATH;

    /**
     * Property name to define the name of the content fragment element which links the content fragment to a product or category.
     */
    String PN_LINK_ELEMENT = "linkElement";

    /**
     * Property name to define the parent path of the displayed content fragment.
     */
    String PN_PARENT_PATH = ContentFragmentList.PN_PARENT_PATH;

    /**
     * @return the title of the configured content fragment model
     */
    String getModelTitle();
}
