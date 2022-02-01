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
package com.adobe.cq.commerce.core.components.internal.servlets;

import java.util.Arrays;
import java.util.List;

import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithSku;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithSkuAndUrlKey;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithSkuAndUrlPath;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithUrlKey;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithUrlPath;
import com.adobe.cq.commerce.core.components.services.urls.UrlFormat;
import com.adobe.granite.ui.components.ds.DataSource;

@Component(
    service = { Servlet.class },
    property = {
        "sling.servlet.resourceTypes=" + ProductPageUrlFormatsServlet.RESOURCE_TYPE,
        "sling.servlet.methods=GET",
        "sling.servlet.extensions=html"
    })
public class ProductPageUrlFormatsServlet extends AbstractPageUrlFormatsServlet {
    static final String RESOURCE_TYPE = "core/cif/components/datasource/producturlformats";
    static final String PAGE_URL_FORMAT_KEY = "productPageUrlFormat";
    static final List<String> URL_FORMATS = Arrays.asList(
        ProductPageWithSku.PATTERN.replace("#", "\\#"),
        ProductPageWithSkuAndUrlKey.PATTERN.replace("#", "\\#"),
        ProductPageWithSkuAndUrlPath.PATTERN.replace("#", "\\#"),
        ProductPageWithUrlKey.PATTERN.replace("#", "\\#"),
        ProductPageWithUrlPath.PATTERN.replace("#", "\\#"));

    @Reference(
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.STATIC,
        policyOption = ReferencePolicyOption.GREEDY,
        target = "("
            + UrlFormat.PROP_USE_AS + "=" + UrlFormat.PRODUCT_PAGE_URL_FORMAT + ")")
    private List<UrlFormat> productPageUrlFormats;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        request.setAttribute(DataSource.class.getName(), getDataSource(request, URL_FORMATS,
            productPageUrlFormats,
            PAGE_URL_FORMAT_KEY));
    }

}
