/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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
package com.adobe.cq.commerce.core.components.internal.servlets;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.servlet.Servlet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpHeaders;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderConfiguration;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.day.cq.wcm.api.Page;

@Component(
    service = Servlet.class,
    immediate = true,
    property = {
        "sling.servlet.methods=GET",
        "sling.servlet.paths=/bin/cif/product",
        "sling.servlet.extensions=" + UrlProvider.SKU_PARAM + "," + UrlProvider.URL_KEY_PARAM
    })
@Designate(ocd = UrlProviderConfiguration.class)
public class ProductPageRedirectServlet extends SlingSafeMethodsServlet {

    private Pair<UrlProvider.IdentifierLocation, UrlProvider.ProductIdentifierType> productIdentifierConfig;
    private Page productPage;

    @Reference
    private UrlProvider urlProvider;

    private AbstractProductRetriever productRetriever;

    @Activate
    public void activate(UrlProviderConfiguration conf) {
        productIdentifierConfig = Pair.of(conf.productIdentifierLocation(), conf.productIdentifierType());
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        Page currentPage = getRefererPage(request);
        productPage = SiteNavigation.getProductPage(currentPage);

        String identifierTypeSelector = request.getRequestPathInfo().getExtension();
        String identifierValue = request.getRequestPathInfo().getSelectors()[0];
        // prepare all possible parameters
        Map<String, String> params = null;

        MagentoGraphqlClient magentoGraphqlClient = MagentoGraphqlClient.create(request.getResource(), currentPage, request);
        if (magentoGraphqlClient != null) {
            productRetriever = new ProductRetriever(magentoGraphqlClient);
        }

        if (productIdentifierConfig.getRight().toString().toLowerCase().equals(identifierTypeSelector)) {
            switch (identifierTypeSelector) {
                case UrlProvider.SKU_PARAM:
                    params = new UrlProvider.ParamsBuilder()
                        .sku(identifierValue)
                        .map();
                    break;
                case UrlProvider.URL_KEY_PARAM:
                    params = new UrlProvider.ParamsBuilder()
                        .urlKey(identifierValue)
                        .map();
                    break;
                default:
                    break;
            }
        } else {
            UrlProvider.ProductIdentifierType productIdentifierType = productIdentifierConfig.getRight().equals(
                UrlProvider.ProductIdentifierType.SKU) ? UrlProvider.ProductIdentifierType.URL_KEY : UrlProvider.ProductIdentifierType.SKU;
            productRetriever.setIdentifier(productIdentifierType, identifierValue);
            ProductInterface product = productRetriever.fetchProduct();
            params = new UrlProvider.ParamsBuilder()
                .urlKey(product.getUrlKey())
                .sku(product.getSku())
                .map();
        }

        response.sendRedirect(urlProvider.toProductUrl(request, productPage, params));
    }

    private Page getRefererPage(SlingHttpServletRequest request) {
        String referer = request.getHeader(HttpHeaders.REFERER);
        if (StringUtils.isBlank(referer)) {
            return null;
        }

        String pagePath;

        try {
            pagePath = new URL(referer).getPath();
        } catch (MalformedURLException e) {
            return null;
        }

        Resource pageResource = request.getResourceResolver().resolve(pagePath);
        return pageResource.adaptTo(Page.class);
    }
}
