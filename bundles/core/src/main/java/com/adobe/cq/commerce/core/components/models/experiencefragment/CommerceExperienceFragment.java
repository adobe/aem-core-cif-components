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
package com.adobe.cq.commerce.core.components.models.experiencefragment;

import org.apache.sling.api.resource.Resource;

import com.adobe.cq.wcm.core.components.models.Component;

public interface CommerceExperienceFragment extends Component {

    /**
     * Name of the configuration property that specifies the products selected in an experience fragment.
     */
    String PN_CQ_PRODUCTS = "cq:products";

    /**
     * Name of the configuration property that specifies the categories selected in an experience fragment.
     */
    String PN_CQ_CATEGORIES = "cq:categories";

    /**
     * Name of the configuration property that specifies the experience fragment location name.
     */
    String PN_FRAGMENT_LOCATION = "fragmentLocation";

    /**
     * This method returns the first experience fragment that matches the location and product or category identifier
     * of the current page. If multiple experience fragments match, the first one is returned, without any guarantee
     * about ordering.
     * 
     * @return The first experience fragment that matches this container or null.
     */
    Resource getExperienceFragmentResource();

    /**
     * Returns the technical name of the experience fragment.
     *
     * @return the technical name of the experience fragment
     */
    String getName();
}
