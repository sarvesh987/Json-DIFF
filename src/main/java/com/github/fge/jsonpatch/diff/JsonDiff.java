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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jackson.JsonNumEquals;
import com.github.fge.jackson.NodeType;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchMessages;
import com.github.fge.msgsimple.bundle.MessageBundle;
import com.github.fge.msgsimple.load.MessageBundles;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.json.JsonObject;

import java.io.IOException;
import java.util.*;

/**
 * JSON "diff" implementation
 *
 * <p>This class generates a JSON Patch (as in, an RFC 6902 JSON Patch) given
 * two JSON values as inputs. The patch can be obtained directly as a {@link
 * JsonPatch} or as a {@link JsonNode}.</p>
 *
 * <p>Note: there is <b>no guarantee</b> about the usability of the generated
 * patch for any other source/target combination than the one used to generate
 * the patch.</p>
 *
 * <p>This class always performs operations in the following order: removals,
 * additions and replacements. It then factors removal/addition pairs into
 * move operations, or copy operations if a common element exists, at the same
 * {@link JsonPointer pointer}, in both the source and destination.</p>
 *
 * <p>You can obtain a diff either as a {@link JsonPatch} directly or, for
 * backwards compatibility, as a {@link JsonNode}.</p>
 *
 * @since 1.2
 */
@ParametersAreNonnullByDefault
public final class JsonDiff
{
    private static final MessageBundle BUNDLE
        = MessageBundles.getBundle(JsonPatchMessages.class);
    private static final ObjectMapper MAPPER = JacksonUtils.newMapper();

    private static final JsonNumEquals EQUIVALENCE
        = JsonNumEquals.getInstance();

    private JsonDiff()
    {
    }

    /**
     * Generate a JSON patch for transforming the source node into the target
     * node
     *
     * @param source the node to be patched
     * @param target the expected result after applying the patch
     * @param map checking attributes
     * @return the patch as a {@link JsonPatch}
     *
     * @since 1.9
     */
    public static JsonPatch asJsonPatch(final JsonNode source,
                                        final JsonNode target,
                                        final Map<JsonPointer, Set<String>> map)
            throws JsonProcessingException {

        BUNDLE.checkNotNull(source, "common.nullArgument");
        BUNDLE.checkNotNull(target, "common.nullArgument");

        DiffProcessor processor = null;

        for (Map.Entry<JsonPointer, Set<String>> me : map.entrySet()) {

            Set<String> s = me.getValue();

            ArrayNode array = (ArrayNode) source.get("Entitlements");

            boolean flag = false;
            for (int i = 0; i < array.size(); i++)
            {
                for (String value : s)
                {
                    if (array.get(i).get(value) == null)
                    {
                        flag = true;
                    }
                }
            }

            if (!flag) {
                //then do ...
                final Map<JsonPointer, JsonNode> unchanged = getUnchangedValues(source, target);
                processor = new DiffProcessor(unchanged);

                generateDiffs(processor, JsonPointer.empty(), source, target, source);

            } else {
                //then do ...
                System.out.println("3 attributes are not present in the entitlement node");
            }
        }
        return processor.getPatch();
    }

    /**
     * Generate a JSON patch for transforming the source node into the target
     * node
     *
     * @param source the node to be patched
     * @param target the expected result after applying the patch
     * @return the patch as a {@link JsonPatch}
     *
     * @since 1.9
     */
   /* public static JsonPatch asJsonPatch(final JsonNode source,
        final JsonNode target) throws JsonProcessingException
    {
        BUNDLE.checkNotNull(source, "common.nullArgument");
        BUNDLE.checkNotNull(target, "common.nullArgument");
        final Map<JsonPointer, JsonNode> unchanged = getUnchangedValues(source, target);
        final DiffProcessor processor = new DiffProcessor(unchanged);

        generateDiffs(processor, JsonPointer.empty(), source, target, source);
        return processor.getPatch();
    }*/

    /**
     * Generate a JSON patch for transforming the source node into the target
     * node
     *
     * @param source the node to be patched
     * @param target the expected result after applying the patch
     * @return the patch as a {@link JsonNode}
     */
    public static JsonNode asJson(final JsonNode source, final JsonNode target, final Map<JsonPointer, Set<String>> map )
    {
        final String s;

        try {
            s = MAPPER.writeValueAsString(asJsonPatch(source, target,map));
            return MAPPER.readTree(s);
        } catch (IOException e) {
            throw new RuntimeException("cannot generate JSON diff", e);
        }
    }

    private static void generateDiffs(final DiffProcessor processor,
                                      final JsonPointer pointer,
                                      final JsonNode source,
                                      final JsonNode target,
                                      final JsonNode source2)
            throws JsonProcessingException
    {

        if (EQUIVALENCE.equivalent(source, target))
        {return;}

        final NodeType firstType = NodeType.getNodeType(source);
        final NodeType secondType = NodeType.getNodeType(target);

        /*
         * Node types differ: generate a replacement operation.
         */
        if (firstType != secondType) {
            processor.valueReplaced(pointer, source, target, source2);
            return;
        }

        /*
         * If we reach this point, it means that both nodes are the same type,
         * but are not equivalent.
         *
         * If this is not a container, generate a replace operation.
         */
        if (!source.isContainerNode()) {
            processor.valueReplaced(pointer, source, target,source2);
            return;
        }

        /*
         * If we reach this point, both nodes are either objects or arrays;
         * delegate.
         */
        if (firstType == NodeType.OBJECT)
            generateObjectDiffs(processor, pointer, (ObjectNode) source,
                (ObjectNode) target);
        else // array
            generateArrayDiffs(processor, pointer, (ArrayNode) source,
                (ArrayNode) target);
    }

    private static void generateObjectDiffs(final DiffProcessor processor,
        final JsonPointer pointer, final ObjectNode source,
        final ObjectNode target) throws JsonProcessingException
    {
        final Set<String> firstFields = collect(source.fieldNames(), new TreeSet<String>());
        final Set<String> secondFields = collect(target.fieldNames(), new TreeSet<String>());

        final Set<String> copy1 = new HashSet<String>(firstFields);
        copy1.removeAll(secondFields);

        for (final String field: Collections.unmodifiableSet(copy1)) {

            //for removed operation here i take 3 parameter from the node.
            ArrayNode og_value_node = new ObjectMapper().createArrayNode();
            og_value_node.add(target.get("Application Key"));
            og_value_node.add(target.get("Entitlement Type"));
            og_value_node.add(target.get("Entitlement Name"));
            processor.valueRemoved(pointer.parent().append("?"), target.get(field) , og_value_node);

        }
        final Set<String> copy2 = new HashSet<String>(secondFields);
        copy2.removeAll(firstFields);


        for (final String field: Collections.unmodifiableSet(copy2))
        {

            processor.valueAdded(pointer.parent().append("-"), target.get(field), source);
        //trying
            //adding new attribute or element
         /*   String json = "{\n" +
                    "  \"name\": \"extra data\",\n" +
                    "  \"salary\": 56000\n" +
                    "}";
            JsonNode jsonNode1 = new ObjectMapper().readTree(json);
            processor.valueAdded(pointer.parent().append("->"), jsonNode1);*/

        }
        final Set<String> intersection = new HashSet<String>(firstFields);
        intersection.retainAll(secondFields);

        for (final String field: intersection)
            generateDiffs(processor, pointer.append(field), source.get(field),
                target.get(field) , source);
    }

    private static <T> Set<T> collect(Iterator<T> from, Set<T> to) {
        if (from == null) {
            throw new NullPointerException();
        }
        if (to == null) {
            throw new NullPointerException();
        }
        while (from.hasNext()) {
            to.add(from.next());
        }
        return Collections.unmodifiableSet(to);
    }



    private static void generateArrayDiffs(final DiffProcessor processor,
        final JsonPointer pointer, final ArrayNode source,
        final ArrayNode target) throws JsonProcessingException {
        final int firstSize = source.size();
        final int secondSize = target.size();
        final int size = Math.min(firstSize, secondSize);

        /*
         * Source array is larger; in this case, elements are removed from the
         * target; the index of removal is always the original arrays's length.
         */
        for (int index = size; index < firstSize; index++) {
            ArrayNode og_value_node = new ObjectMapper().createArrayNode();
            og_value_node.add(source.get("Application Key"));
            og_value_node.add(source.get("Entitlement Type"));
            og_value_node.add(source.get("Entitlement Name"));

            // System.out.println(""+og_value_node.toPrettyString());

            processor.valueRemoved(pointer.parent().append("?"), source.get(index) , og_value_node);
            //check code here because i added this code :- source.findValue(String.valueOf(index))
        }
        for (int index = 0; index < size; index++) {
            //here we simply check op type and change index with dash but unfortunately our op is not in scope.
            generateDiffs(processor, pointer.append(index),
                    source.get(index), target.get(index), source);
        }
        // Deal with the destination array being larger...
        for (int index = size; index < secondSize; index++) {
            processor.valueAdded(pointer, target.get(index) , null);
            //adding new attribute or element
           /* String json = "{\n" +
                    "  \"opb\": \"sample\",\n" +
                    "  \"path\": \"/volRoleInVEM/?\",\n" +
                    "  \"original_value\": {\n" +
                    "    \"roleName\": \"role1\"\n" +
                    "  }\n" +
                    "}\n" +
                    "\n";
            JsonNode jsonNode1 = new ObjectMapper().readTree(json);
            processor.valueAdded(pointer , jsonNode1);*/
        }
    }


    static Map<JsonPointer, JsonNode> getUnchangedValues(final JsonNode source,
        final JsonNode target)
    {
        final Map<JsonPointer, JsonNode> ret = new HashMap<JsonPointer, JsonNode>();
        computeUnchanged(ret, JsonPointer.empty(), source, target);
        return ret;
    }

    private static void computeUnchanged(final Map<JsonPointer, JsonNode> ret,
        final JsonPointer pointer, final JsonNode first, final JsonNode second)
    {
        if (EQUIVALENCE.equivalent(first, second)) {
            ret.put(pointer, second);
            return;
        }

        final NodeType firstType = NodeType.getNodeType(first);
        final NodeType secondType = NodeType.getNodeType(second);

        if (firstType != secondType)
            return; // nothing in common

        // We know they are both the same type, so...

        switch (firstType) {
            case OBJECT:
                computeObject(ret, pointer, first, second);
                break;
            case ARRAY:
                computeArray(ret, pointer, first, second);
                break;
            default:
                /* nothing */
        }
    }

    private static void computeObject(final Map<JsonPointer, JsonNode> ret,
        final JsonPointer pointer, final JsonNode source,
        final JsonNode target)
    {
        final Iterator<String> firstFields = source.fieldNames();

        String name;

        while (firstFields.hasNext()) {
            name = firstFields.next();
            if (!target.has(name))
                continue;computeUnchanged(ret, pointer.append(name), source.get(name),
                target.get(name));
        }
    }

    private static void computeArray(final Map<JsonPointer, JsonNode> ret,
        final JsonPointer pointer, final JsonNode source, final JsonNode target)
    {
        final int size = Math.min(source.size(), target.size());

        for (int i = 0; i < size; i++) {
            computeUnchanged(ret, pointer.append(i), source.get(i), target.get(i));
        }
    }
}
