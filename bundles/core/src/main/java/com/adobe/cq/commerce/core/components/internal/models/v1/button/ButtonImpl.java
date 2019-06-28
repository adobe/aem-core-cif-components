/*
 * Copyright 2019 Adobe.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adobe.cq.commerce.core.components.internal.models.v1.button;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.internal.models.v1.Utils;
import com.adobe.cq.commerce.core.components.models.button.Button;
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
    @Default(values = DEFAULT_LABEL)
    private String label;

    @ValueMapValue
    @Default(values = DEFAULT_LINK)
    private String linkTo;

    @ValueMapValue
    @Default(values = DEFAULT_LINK)
    private String productSlug;

    @ValueMapValue
    @Default(values = DEFAULT_LINK)
    private String categoryId;

    @ValueMapValue
    @Default(values = DEFAULT_LINK)
    private String externalLink;

    @ValueMapValue
    private String linkType;

    @Inject
    private Page currentPage;

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

                if (!productSlug.equals(DEFAULT_LINK)) {
                    productPage = Utils.getProductPage(currentPage);
                    if (productPage == null) {
                        productPage = currentPage;
                    }
                    url = this.constructUrl(productPage.getPath(), productSlug);
                } else {
                    LOGGER.error("Can not get Product Slug!");
                }
                break;
            }
            case CATEGORY: {

                if (!categoryId.equals(DEFAULT_LINK)) {
                    categoryPage = Utils.getCategoryPage(currentPage);
                    if (categoryPage == null) {
                        categoryPage = currentPage;
                    }
                    url = this.constructUrl(categoryPage.getPath(), categoryId);
                } else {
                    LOGGER.error("Can not get Category Id!");
                }
                break;
            }
            case EXTERNAL_LINK: {

                if (!externalLink.equals(DEFAULT_LINK)) {
                    url = this.externalLink;
                } else {
                    LOGGER.error("Can not get External Link!");
                }
                break;
            }
            case LINK_TO: {

                if (!linkTo.equals(DEFAULT_LINK)) {
                    url = this.linkTo + ".html";
                } else {
                    LOGGER.error("Can not get LinkToPage!");
                }
                break;
            }
        }
        return url;
    }

    @Override
    public String getUrl() {
        return StringUtils.isNotBlank(url) ? url : DEFAULT_LINK;
    }

    @Override
    public String getLabel() {
        return StringUtils.isNotBlank(label) ? label : DEFAULT_LABEL;
    }

    private String constructUrl(final String pagePath, final String urlKey) {
        return String.format("%s.%s.html", pagePath, urlKey);
    }
}