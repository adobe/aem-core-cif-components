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
package com.adobe.cq.commerce.core.components.internal.models.v2.button;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.models.annotations.via.ResourceSuperType;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.wcm.core.components.models.Button;
import com.day.cq.wcm.api.Page;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = Button.class,
    resourceType = ButtonImpl.RESOURCE_TYPE,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ButtonImpl implements Button {

    protected static final String RESOURCE_TYPE = "core/cif/components/content/button/v2/button";

    private static final String DEFAULT_LABEL = "Label";
    private static final String DEFAULT_LINK = "#";
    private static final String PRODUCT = "product";
    private static final String CATEGORY = "category";
    private static final String EXTERNAL_LINK = "externalLink";
    private static final String LINK_TO = "linkTo";

    @ValueMapValue
    @Default(values = DEFAULT_LINK)
    private String linkTo;

    @ValueMapValue(name = "productSku")
    @Default(values = DEFAULT_LINK)
    private String productIdentifier;

    @ValueMapValue(name = "categoryId")
    @Default(values = DEFAULT_LINK)
    private String categoryIdentifier;

    @ValueMapValue
    @Default(values = DEFAULT_LINK)
    private String externalLink;

    @Self
    @Via(type = ResourceSuperType.class)
    private Button button;

    @Self(injectionStrategy = InjectionStrategy.OPTIONAL)
    private MagentoGraphqlClient magentoGraphqlClient;

    @ValueMapValue
    private String linkType;

    @Self
    private SlingHttpServletRequest request;

    @OSGiService
    private UrlProvider urlProvider;

    @ScriptVariable
    private Page currentPage;

    @ScriptVariable
    protected Resource resource;

    private Page productPage;
    private Page categoryPage;
    private String url;

    @PostConstruct
    private void initModel() {
        if (linkType != null) {
            assignUrl(linkType);
        }
    }

    private String assignUrl(final String linkType) {
        switch (linkType) {
            case PRODUCT: {
                if (!productIdentifier.equals(DEFAULT_LINK)) {
                    productPage = SiteNavigation.getProductPage(currentPage);
                    if (productPage == null) {
                        productPage = currentPage;
                    }
                    url = urlProvider.toProductUrl(request, productPage, productIdentifier);
                }
                break;
            }

            case CATEGORY: {
                if (!categoryIdentifier.equals(DEFAULT_LINK)) {
                    categoryPage = SiteNavigation.getCategoryPage(currentPage);
                    if (categoryPage == null) {
                        categoryPage = currentPage;
                    }
                    url = urlProvider.toCategoryUrl(request, categoryPage, categoryIdentifier);
                }
                break;
            }

            case EXTERNAL_LINK: {
                if (!externalLink.equals(DEFAULT_LINK)) {
                    url = this.externalLink;
                }
                break;
            }

            case LINK_TO: {
                if (!linkTo.equals(DEFAULT_LINK)) {
                    url = this.linkTo + ".html";
                }
                break;
            }
        }
        return url;
    }

    @Override
    public String getText() {
        return StringUtils.isNotBlank(button.getText()) ? button.getText() : DEFAULT_LABEL;
    }

    @Override
    public String getLink() {
        return StringUtils.isNotBlank(url) ? url : DEFAULT_LINK;
    }

    @Override
    public String getIcon() {
        return button.getIcon();
    }

    @Override
    public String getExportedType() {
        return button.getExportedType();
    }
}
