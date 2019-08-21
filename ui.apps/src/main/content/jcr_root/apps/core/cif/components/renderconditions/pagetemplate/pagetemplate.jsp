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