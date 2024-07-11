/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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
package com.adobe.cq.commerce.core.components.internal.models.v1.button;

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
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.models.annotations.via.ResourceSuperType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.wcm.core.components.models.Button;
import com.day.cq.wcm.api.Page;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = Button.class,
    resourceType = ButtonImpl.RESOURCE_TYPE,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ButtonImpl implements Button {

    protected static final String RESOURCE_TYPE = "core/cif/components/content/button/v1/button";

    private static final Logger LOGGER = LoggerFactory.getLogger(ButtonImpl.class);
    private static final String DEFAULT_LABEL = "Label";
    private static final String DEFAULT_LINK = "#";
    private static final String PRODUCT = "product";
    private static final String CATEGORY = "category";
    private static final String EXTERNAL_LINK = "externalLink";
    private static final String LINK_TO = "linkTo";

    @ValueMapValue
    @Default(values = DEFAULT_LINK)
    private String linkTo;

    @ValueMapValue
    @Default(values = DEFAULT_LINK)
    private String productSlug;

    @ValueMapValue
    @Default(values = "slug")
    private String productSlugType;

    @ValueMapValue
    @Default(values = DEFAULT_LINK)
    private String categoryId;

    @ValueMapValue
    @Default(values = "uid")
    private String categoryIdType;

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

    @SlingObject
    protected Resource resource;

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
                if (!productSlug.equals(DEFAULT_LINK)) {
                    ProductUrlFormat.Params params = new ProductUrlFormat.Params();
                    params.setUrlKey(productSlug);
                    url = urlProvider.toProductUrl(request, currentPage, params);
                } else {
                    LOGGER.debug("Can not get Product Slug!");
                }
                break;
            }

            case CATEGORY: {
                if (!categoryId.equals(DEFAULT_LINK)) {
                    CategoryUrlFormat.Params params = null;
                    if (magentoGraphqlClient != null) {
                        CategoryRetriever categoryRetriever = new CategoryRetriever(magentoGraphqlClient);
                        categoryRetriever.setIdentifier(categoryId);
                        CategoryInterface category = categoryRetriever.fetchCategory();
                        if (category != null) {
                            params = new CategoryUrlFormat.Params(category);
                        }
                    }
                    url = urlProvider.formatCategoryUrl(request, currentPage, params != null ? params : new CategoryUrlFormat.Params());
                } else {
                    LOGGER.debug("Can not get Category identifier!");
                }
                break;
            }

            case EXTERNAL_LINK: {
                if (!externalLink.equals(DEFAULT_LINK)) {
                    url = this.externalLink;
                } else {
                    LOGGER.debug("Can not get External Link!");
                }
                break;
            }

            case LINK_TO: {
                if (!linkTo.equals(DEFAULT_LINK)) {
                    url = this.linkTo + ".html";
                } else {
                    LOGGER.debug("Can not get LinkToPage!");
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
