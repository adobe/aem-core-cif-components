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

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;

import com.adobe.granite.ui.components.rendercondition.RenderCondition;
import com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition;

@Component(
    service = { Servlet.class },
    property = {
        "sling.servlet.resourceTypes=" + ProductListXfStylesRenderConditionServlet.RESOURCE_TYPE_V1,
        "sling.servlet.methods=GET",
        "sling.servlet.extensions=html"
    })
public class ProductListXfStylesRenderConditionServlet extends AbstractProductListXfServlet {
    public static final String RESOURCE_TYPE_V1 = "core/cif/components/commerce/renderconditions/productlistxfstyles/v1";

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
        throws ServletException, IOException {
        request.setAttribute(RenderCondition.class.getName(),
            new SimpleRenderCondition(checkXfStyles(request)));
    }

    private boolean checkXfStyles(SlingHttpServletRequest request) {
        Resource fragmentStylesResource = getFragmentStylesResource(request);
        if (fragmentStylesResource != null) {
            return fragmentStylesResource.hasChildren();
        }

        return false;
    }
}
