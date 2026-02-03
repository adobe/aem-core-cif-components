package com.adobe.cq.commerce.core.components.internal.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
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
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

import com.day.cq.wcm.api.policies.ContentPolicy;
import com.day.cq.wcm.api.policies.ContentPolicyManager;

public abstract class AbstractProductListXfServlet extends SlingSafeMethodsServlet {
    public static final String PN_FRAGMENT_STYLES = "fragmentStyles";

    protected Resource getFragmentStylesResource(SlingHttpServletRequest request) {
        ResourceResolver resolver = request.getResourceResolver();

        Resource contentResource = request.getRequestPathInfo().getSuffixResource();

        if (contentResource != null) {
            ContentPolicyManager policyManager = resolver.adaptTo(ContentPolicyManager.class);
            if (policyManager != null) {
                ContentPolicy policy = policyManager.getPolicy(contentResource);
                if (policy != null) {
                    String policyPath = policy.getPath();
                    Resource policyResource = resolver.getResource(policyPath);

                    return policyResource.getChild(PN_FRAGMENT_STYLES);
                }
            }
        }

        return null;
    }

}
