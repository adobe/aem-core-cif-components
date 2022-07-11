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
package com.adobe.cq.commerce.core.components.internal.models.v1.page;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.factory.ModelFactory;

import com.adobe.cq.wcm.core.components.models.Page;

/**
 * A {@link Page} model implementation that can be adapted from non-page resources and delegate to the {@link Page} model of the requests
 * currentPage. This is used for compatibility reasons where components (e.g. product and productlist) use the {@link Page} model in their
 * htl files but are actually not of the {@link Page}'s supported resource types. In the past this worked because of the
 * LatestVersionImplementationPicker that picked the highest versioned {@link Page} implementation form the wcm core components. With the
 * implementation of {@link PageImpl} however this is not possible anymore, as we require a proper resourceSuperType on the resource
 * pointing to a page, otherwise the adaption ends in a loop.
 *
 * @deprecated because this is an anti-pattern. With the next update of product and product collection we should remove the usage of
 *             {@link Page} by those components
 */
@Deprecated
@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = com.adobe.cq.wcm.core.components.models.Page.class,
    resourceType = {
        com.adobe.cq.commerce.core.components.internal.models.v1.product.ProductImpl.RESOURCE_TYPE,
        com.adobe.cq.commerce.core.components.internal.models.v2.product.ProductImpl.RESOURCE_TYPE,
        com.adobe.cq.commerce.core.components.internal.models.v1.productcollection.ProductCollectionImpl.RESOURCE_TYPE,
        com.adobe.cq.commerce.core.components.internal.models.v1.productcollection.ProductCollectionImpl.RESOURCE_TYPE_V2
    })
public class CurrentPageImpl extends AbstractPageDelegator {

    @Self
    private SlingHttpServletRequest request;
    @ScriptVariable
    private com.day.cq.wcm.api.Page currentPage;
    @OSGiService
    protected ModelFactory modelFactory;

    private com.adobe.cq.wcm.core.components.models.Page delegate;

    @PostConstruct
    protected void initModel() {
        delegate = modelFactory.getModelFromWrappedRequest(request, currentPage.getContentResource(), Page.class);
        request = null;
    }

    @Override
    protected Page getDelegate() {
        return delegate;
    }
}
