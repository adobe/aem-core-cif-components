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

package com.adobe.cq.commerce.core.components.models.graphql;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;

public class GraphqlQueryBuilder {
    public GraphqlQueryBuilder() {}

    public String fromResource(Resource resource) {
        String query = "";
        for (Resource child : resource.getChildren()) {
            String type = child.getValueMap().get("type", "field");
            if ("fragmentdefinition".equals(type)) {
                String onType = child.getValueMap().get("on", String.class);
                query += "fragment " + child.getName() + " on " + onType + buildQuery(child);
            } else
                query += child.getName() + " " + buildQuery(child) + " ";
        }
        return query;
    }

    String buildQuery(Resource resource) {
        if (resource == null) {
            return "";
        }

        if (!resource.hasChildren()) {
            return "";
        }

        Collection<Resource> arguments = new ArrayList<Resource>();
        Resource args = resource.getChild("_arguments_");
        if (args != null) {
            for (Resource child : args.getChildren()) {
                String type = child.getValueMap().get("type", "field");
                if ("argument".equals(type)) {
                    arguments.add(child);
                }
            }
        }

        Collection<Resource> variableDefinitions = new ArrayList<Resource>();
        Resource vars = resource.getChild("_variables_");
        if (vars != null) {
            for (Resource child : vars.getChildren()) {
                String type = child.getValueMap().get("type", "field");
                if ("variabledefinition".equals(type)) {
                    variableDefinitions.add(child);
                }
            }
        }

        String val = "";
        if (!variableDefinitions.isEmpty()) {
            val += " ( ";
            for (Resource vardef : variableDefinitions) {
                val += " $" + vardef.getName() + ": " + vardef.getValueMap().get("variableType", String.class);
            }
            val += " ) ";
        }
        if (!arguments.isEmpty()) {
            val += " ( ";
            for (Resource arg : arguments) {
                String value = arg.getValueMap().get("value", String.class);
                if (StringUtils.isNotBlank(value)) {
                    val += " " + arg.getName() + ": " + value;
                } else {
                    val += " " + arg.getName() + ": " + buildQuery(arg);
                }
            }
            val += " ) ";
        }

        val += " {";

        for (Resource child : resource.getChildren()) {
            String type = child.getValueMap().get("type", "field");
            if ("fragment".equals(type)) {
                val += " ... " + child.getName() + buildQuery(child);
            } else if ("inlinefragment".equals(type)) {
                val += " ... on " + child.getName() + buildQuery(child);
            } else if ("argument".equals(type)) {
                continue;
            } else if ("_arguments_".equals(child.getName())) {
                continue;
            } else if ("_variables_".equals(child.getName())) {
                continue;
            } else if ("variabledefinition".equals(type)) {
                continue;
            } else if ("fragmentdefinition".equals(type)) {
                String onType = child.getValueMap().get("on", String.class);
                val += " fragment " + child.getName() + " on " + onType + buildQuery(child);
            } else if ("objectfield".equals(type)) {
                String value = child.getValueMap().get("value", String.class);
                if (value != null) {
                    val += " " + child.getName() + ": " + value;
                } else {
                    val += " " + child.getName() + ": " + buildQuery(child);
                }
            } else if ("variable".equals(type)) {
                val += " $" + child.getName();
            } else {
                val += " " + child.getName() + buildQuery(child);
            }
        }
        val += " }";
        return val;
    }
}