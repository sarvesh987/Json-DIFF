/*
 * Copyright (c) 2014, Francis Galiegue (fgaliegue@gmail.com)
 *
 * This software is dual-licensed under:
 *
 * - the Lesser General Public License (LGPL) version 3.0 or, at your option, any
 *   later version;
 * - the Apache Software License (ASL) version 2.0.
 *
 * The text of this file and of both licenses is available at the root of this
 * project or, if you have the jar distribution, in directory META-INF/, under
 * the names LGPL-3.0.txt and ASL-2.0.txt respectively.
 *
 * Direct link to the sources:
 *
 * - LGPL 3.0: https://www.gnu.org/licenses/lgpl-3.0.txt
 * - ASL 2.0: http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package com.github.fge.jsonpatch.diff;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatch;
import com.google.common.collect.Lists;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;

public final class UnchangedTest {

    private static final ObjectMapper MAPPER = JacksonUtils.newMapper();
    private static final TypeReference<Map<JsonPointer, JsonNode>> TYPE_REF
            = new TypeReference<Map<JsonPointer, JsonNode>>() {
    };

    private final JsonNode testData;

    public UnchangedTest()
            throws IOException {
        final String resource = "/jsonpatch/diff/unchanged.json";
        testData = JsonLoader.fromResource(resource);
    }

    @DataProvider
    public Iterator<Object[]> getTestData()
            throws IOException {
        final List<Object[]> list = Lists.newArrayList();

        for (final JsonNode node : testData)
            list.add(new Object[]{node.get("first"), node.get("second"),
                    MAPPER.readValue(node.get("unchanged").traverse(), TYPE_REF)});

        return list.iterator();
    }

    @Test(dataProvider = "getTestData")
    public void computeUnchangedValuesWorks(final JsonNode first,
                                            final JsonNode second, final Map<JsonPointer, JsonNode> expected) {
        final Map<JsonPointer, JsonNode> actual
                = JsonDiff.getUnchangedValues(first, second);

        assertEquals(actual, expected);
    }

    @Test
    public void testCase1() throws JsonProcessingException {

        String json1 = "{\n" +
                "  \"Role Display Name\": \"Test Role Pset\",\n" +
                "  \"Role Category\": \"Default\",\n" +
                "  \"Role Name\": \"Test Role Pset\",\n" +
                "  \"Role Description\": \"Test Role Pset\",\n" +
                "  \"Organization\": \"Confluxsys\",\n" +
                "  \"Role Owner Login\": \"PBULE\",\n" +
                "  \"Role ID\": \"14350\",\n" +
                "  \"$identifier\": \"14350\",\n" +
                "  \"Entitlements\": [\n" +
                "    {\n" +
                "      \"Application Key\": \"121\",\n" +
                "      \"Entitlement Type\": \"UD_GROUPS_GROUPS\",\n" +
                "      \"Entitlement Name\": \"144~Network Security Role\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"Application Key\": \"562\",\n" +
                "      \"Entitlement Type\": \"UD_GROUPS_GROUPS\",\n" +
                "      \"Entitlement Name\": \"565~Deployment Owners\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"Application Key\": \"100\",\n" +
                "      \"Entitlement Type\": \"UD_GROUPS_GROUPS\",\n" +
                "      \"Entitlement Name\": \"565~Deployment Owners\",\n" +
                "      \"Entitlement Key\": \"askjdhfiegig02k\",\n" +
                "      \"Additional Info\": \"test\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";


       /* String json3 = "{\n" +
                "  \"Aidan Gillen\": \n" +
                "  {\n" +
                "    \"array\": [\n" +
                "      \"Game of Thron\\\"es\",\n" +
                "      \"The Wire\"\n" +
                "    ],\n" +
                "    \"string\": \"some string\",\n" +
                "    \"int\": 2,\n" +
                "    \"aboolean\": true,\n" +
                "    \"boolean\": true,\n" +
                "    \"ObjectArray\": {\n" +
                "      \"foo\": \"bar\",\n" +
                "      \"object1\": {\n" +
                "        \"new prop1\": \"new prop value\"\n" +
                "      },\n" +
                "      \"object2\": {\n" +
                "        \"new prop1\": \"new prop value\"\n" +
                "      },\n" +
                "      \"object3\": {\n" +
                "        \"new prop1\": \"new prop value\"\n" +
                "      },\n" +
                "      \"object4\": {\n" +
                "        \"new prop1\": \"new prop value\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        String json4 = "{\n" +
                "  \"Aidan Gillen\": \n" +
                "  {\n" +
                "    \"array\": [\n" +
                "      \"Game of Thron\\\"es\",\n" +
                "      \"The Wire\"\n" +
                "    ],\n" +
                "    \"string\": \"some string\",\n" +
                "    \"int\": 2,\n" +
                "    \"aboolean\": true,\n" +
                "    \"boolean\": true,\n" +
                "    \"ObjectArray\": {\n" +
                "      \"foo\": \"bar\",\n" +
                "      \"object1\": {\n" +
                "        \"new prop1\": \"new prepartion value\"\n" +
                "      },\n" +
                "      \"object2\": {\n" +
                "        \"new prop2\": \"new prop \"\n" +
                "      },\n" +
                "      \"object3\": {\n" +
                "        \"new prop1\": \"new prop value\"\n" +
                "      },\n" +
                "      \"object4\": {\n" +
                "        \"new prop1\": \"new prop value\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
*/

        String json2 = "{\n" +
                "  \"Role Display Name\": \"Test Role Pset\",\n" +
                "  \"Role Category\": \"Default\",\n" +
                "  \"Role Name\": \"Test Role Pset\",\n" +
                "  \"Role Description\": \"Test Role Pset\",\n" +
                "  \"Organization\": \"Confluxsys\",\n" +
                "  \"Role Owner Login\": \"JDOE\",\n" +
                "  \"Role ID\": \"14350\",\n" +
                "  \"$identifier\": \"14350\",\n" +
                "  \"Entitlements\": [\n" +
                "    {\n" +
                "      \"Application Key\": \"562\",\n" +
                "      \"Entitlement Type\": \"UD_GROUPS_GROUPS\",\n" +
                "      \"Entitlement Name\": \"565~Deployment Owners\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"Application Key\": \"100\",\n" +
                "      \"Entitlement Type\": \"UD_GROUPS_GROUPS\",\n" +
                "      \"Entitlement Name\": \"565~Deployment Owners\",\n" +
                "      \"Entitlement Key\": \"updated\",\n" +
                "      \"Additional Info\": \"test updated\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"Application Key\": \"122\",\n" +
                "      \"Entitlement Type\": \"UD_GROUPS_GROUPS\",\n" +
                "      \"Entitlement Name\": \"144~Network Security Role2\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        JsonNode jsonNode1 = new ObjectMapper().readTree(json1);
        Assert.assertNotNull(jsonNode1);
        JsonNode jsonNode2 = new ObjectMapper().readTree(json2);
        Assert.assertNotNull(jsonNode2);

        JsonPointer pointer = JsonPointer.of("Entitlements");
        Map<JsonPointer, Set<String>> map = new HashMap<>();
        Set<String> set = new HashSet<>();
        set.add("Application Key");
        set.add("Entitlement Type");
        set.add("Entitlement Name");
        map.put(pointer,set);

        JsonNode jsonNode = JsonDiff.asJson(jsonNode1, jsonNode2, map);
        Assert.assertNotNull(jsonNode);
        System.out.println("" + jsonNode.toPrettyString());
    }
}


