/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
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
package com.adobe.cq.commerce.mcp.internal.servlets;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * The OSGi DS {@code @Component} annotation is retained with {@link java.lang.annotation.RetentionPolicy#CLASS},
 * so {@code configurationPolicy} is not visible via runtime reflection on the annotation itself. The actual,
 * build-verifiable representation of the declarative services metadata is the SCR component descriptor generated
 * by the bnd plugin under {@code OSGI-INF}. This test asserts against that generated descriptor.
 */
public class AuthoringPolicyTest {

    private Element componentElement(Class<?> componentClass) throws ParserConfigurationException, SAXException, IOException {
        String resource = "/OSGI-INF/" + componentClass.getName() + ".xml";
        try (InputStream is = componentClass.getResourceAsStream(resource)) {
            assertNotNull("Expected generated SCR descriptor at " + resource, is);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(is);
            return document.getDocumentElement();
        }
    }

    @Test
    public void authoringServletRequiresConfig() throws ParserConfigurationException, SAXException, IOException {
        Element component = componentElement(AuthoringMcpServlet.class);
        assertEquals("require", component.getAttribute("configuration-policy"));
    }

    @Test
    public void shopperServletDoesNotRequireConfig() throws ParserConfigurationException, SAXException, IOException {
        Element component = componentElement(ShopperMcpServlet.class);
        assertEquals("", component.getAttribute("configuration-policy"));
    }
}
