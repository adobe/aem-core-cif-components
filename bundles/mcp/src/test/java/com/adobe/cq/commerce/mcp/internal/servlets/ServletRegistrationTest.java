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

import java.lang.reflect.Method;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ServletRegistrationTest {
    private String selectorOf(AbstractMcpServlet s) throws Exception {
        Method m = AbstractMcpServlet.class.getDeclaredMethod("selector");
        m.setAccessible(true);
        return (String) m.invoke(s);
    }

    @Test
    public void shopperSelector() throws Exception {
        assertEquals("mcp", selectorOf(new ShopperMcpServlet()));
    }

    @Test
    public void authoringSelector() throws Exception {
        assertEquals("mcp-authoring", selectorOf(new AuthoringMcpServlet()));
    }
}
