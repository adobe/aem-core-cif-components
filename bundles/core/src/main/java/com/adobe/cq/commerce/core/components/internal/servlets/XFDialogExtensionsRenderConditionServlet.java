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
package com.adobe.cq.commerce.core.components.internal.servlets;

import java.util.Arrays;

import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import com.adobe.granite.ui.components.rendercondition.RenderCondition;
import com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition;

@Component(
    service = Servlet.class,
    name = "XFDialogExtensionsRenderConditionServlet",
    immediate = true,
    property = {
        "sling.servlet.resourceTypes=core/cif/components/renderconditions/xf-extensions",
        "sling.servlet.methods=GET",
        "sling.servlet.extensions=html"
    })
public class XFDialogExtensionsRenderConditionServlet extends SlingSafeMethodsServlet {

    private static final String ADDON_BUNDLE = "com.adobe.cq.cif.commerce-addon-bundle";

    private BundleContext bundleContext;

    @Activate
    protected void activate(ComponentContext ctx) {
        bundleContext = ctx.getBundleContext();
    }

    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        Bundle[] bundles = bundleContext.getBundles();
        boolean enable = Arrays.asList(bundles).stream().anyMatch(b -> ADDON_BUNDLE.equals(b.getSymbolicName()));
        request.setAttribute(RenderCondition.class.getName(), new SimpleRenderCondition(enable));
    }
}
