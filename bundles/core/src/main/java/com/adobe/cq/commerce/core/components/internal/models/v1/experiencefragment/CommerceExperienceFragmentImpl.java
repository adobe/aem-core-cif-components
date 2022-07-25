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
package com.adobe.cq.commerce.core.components.internal.models.v1.experiencefragment;

import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.services.experiencefragments.CommerceExperienceFragmentsRetriever;
import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.core.components.models.experiencefragment.CommerceExperienceFragment;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = CommerceExperienceFragment.class,
    resourceType = CommerceExperienceFragmentImpl.RESOURCE_TYPE)
public class CommerceExperienceFragmentImpl implements CommerceExperienceFragment {

    public static final String RESOURCE_TYPE = "core/cif/components/commerce/experiencefragment/v1/experiencefragment";

    @Self
    private SlingHttpServletRequest request;

    @Self(injectionStrategy = InjectionStrategy.OPTIONAL)
    private MagentoGraphqlClient magentoGraphqlClient;

    @ValueMapValue(name = PN_FRAGMENT_LOCATION, injectionStrategy = InjectionStrategy.OPTIONAL)
    private String fragmentLocation;

    @ScriptVariable
    private Page currentPage;

    @SlingObject
    protected Resource resource;

    @SlingObject
    private ResourceResolver resolver;

    @OSGiService
    private UrlProvider urlProvider;

    @OSGiService
    private CommerceExperienceFragmentsRetriever fragmentsRetriever;

    @Self
    private SiteStructure siteNavigation;

    private Resource xfResource;
    private String name;

    @PostConstruct
    private void initModel() {
        List<Resource> xfs = null;
        if (siteNavigation.isProductPage(currentPage)) {
            String sku = urlProvider.getProductIdentifier(request);
            xfs = fragmentsRetriever.getExperienceFragmentsForProduct(sku, fragmentLocation, 1, currentPage);
        } else if (siteNavigation.isCategoryPage(currentPage)) {
            String categoryUid = urlProvider.getCategoryIdentifier(request);
            xfs = fragmentsRetriever.getExperienceFragmentsForCategory(categoryUid, fragmentLocation, 1, currentPage);
        }

        if (xfs != null && !xfs.isEmpty()) {
            xfResource = xfs.get(0);
            resolveName();
        }
    }

    private void resolveName() {
        PageManager pageManager = resolver.adaptTo(PageManager.class);
        Page xfVariationPage = pageManager.getPage(xfResource.getParent().getPath());
        if (xfVariationPage != null) {
            Page xfPage = xfVariationPage.getParent();
            if (xfPage != null) {
                name = xfPage.getName();
            }
        }
    }

    @Override
    public Resource getExperienceFragmentResource() {
        return xfResource;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getExportedType() {
        return resource.getResourceType();
    }
}
