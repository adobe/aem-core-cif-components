<%--
/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
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
--%>
<%@include file="/libs/foundation/global.jsp"%>
<%@page session="false"
        import="com.adobe.granite.ui.components.Config,
          com.adobe.granite.ui.components.rendercondition.RenderCondition,
          com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition,
          com.adobe.cq.commerce.core.components.utils.TemplateRenderCondition,
          com.adobe.granite.ui.components.ComponentHelper"%><%--###
              
Template
======
.. granite:servercomponent:: /apps/core/cif/components/renderconditions/pagetemplate
   :rendercondition:

   A condition that renders page properties based on page template path

   It has the following content structure:
   .. gnd:gnd::
      [granite:RenderConditionsTemplate]
      /**
       * The template path to match
       */
      - templatePath (String)

###--%>
<sling:defineObjects/>
<%

ComponentHelper cmp = new ComponentHelper(pageContext);
String path = cmp.getConfig().get("templatePath", "");
boolean vote = TemplateRenderCondition.isTemplate(slingRequest, path);
request.setAttribute(RenderCondition.class.getName(), new SimpleRenderCondition(vote));

%>