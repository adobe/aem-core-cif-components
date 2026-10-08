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
package com.adobe.cq.commerce.it.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ComponentJSONExporterIT extends CommerceTestBase {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final CustomComparator jsonComparator = new CustomComparator(JSONCompareMode.STRICT,
        new Customization("**.repo:modifyDate", (o1, o2) -> true));
    private static final String[] componentKeys = new String[] { "categorycarousel", "featuredcategorylist", "productteaser",
        "productcarousel", "relatedproducts", "teaser" };

    @Test
    public void testComponentsJsonOutput() throws ClientException, IOException, JSONException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/json/exporter.model.json", 200);

        JsonNode jsonOutput = mapper.readTree(response.getContent());
        JsonNode componentsJson = jsonOutput.at("/:items/root/:items/responsivegrid/:items");
        Assert.assertNotNull(componentsJson);

        for (String componentKey : componentKeys) {
            Assert.assertTrue(componentsJson.has(componentKey));
            String expectedComponentJsonOutput = readResourceFile("/exporter/" + componentKey + ".json");
            String actualComponentJsonOutput = componentsJson.get(componentKey).toString();
            JSONAssert.assertEquals(expectedComponentJsonOutput, actualComponentJsonOutput, jsonComparator);
        }
    }

    private String readResourceFile(String resourceFile)
        throws IOException {
        InputStream inputStream = ComponentJSONExporterIT.class.getResourceAsStream(
            resourceFile);
        StringBuilder resultStringBuilder = new StringBuilder();
        assert inputStream != null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }
}