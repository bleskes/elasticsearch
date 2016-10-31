
package org.elasticsearch.xpack.prelert.job.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.test.ESTestCase;
import org.junit.Before;

import org.elasticsearch.xpack.prelert.job.persistence.serialisation.DotNotationReverser;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialisable;

public class ElasticsearchStorageSerialiserTests extends ESTestCase {
    private XContentBuilder builder;
    private ElasticsearchStorageSerialiser serialiser;

    @Before
    public void setUpMocks() throws IOException {
        builder = XContentFactory.jsonBuilder();
        serialiser = new ElasticsearchStorageSerialiser(builder);
    }

    public void testAdd() throws IOException {
        serialiser.startObject();
        serialiser.add("aBool", true);
        serialiser.add("aDouble", 3.14);
        serialiser.add("anInt", 18);
        serialiser.add("aLong", 1234567891234L);
        serialiser.add("aDate", new Date(1455753600000L));
        serialiser.add("doubles", 1.1, 2.2);
        serialiser.add("strings", "a", "b");
        serialiser.addTimestamp(new Date(1455757200000L));
        serialiser.add("nestedList", Arrays.asList(
                createSingleKeyValueSerialisable("a", "a_value"),
                createSingleKeyValueSerialisable("b", "b_value")));
        Map<String, Object> map = new HashMap<>();
        map.put("map_a", "map_a_value");
        serialiser.add("aMap", map);
        serialiser.startList("anEmptyList");
        serialiser.endList();
        serialiser.startObject("nestedObj");
        serialiser.add("nested_key", "nested_value");
        serialiser.endObject();
        serialiser.endObject();

        String expected = "{"
                + "\"aBool\":true,"
                + "\"aDouble\":3.14,"
                + "\"anInt\":18,"
                + "\"aLong\":1234567891234,"
                + "\"aDate\":\"2016-02-18T00:00:00.000Z\","
                + "\"doubles\":[1.1,2.2],"
                + "\"strings\":[\"a\",\"b\"],"
                + "\"@timestamp\":\"2016-02-18T01:00:00.000Z\","
                + "\"nestedList\":["
                + "{\"a\":\"a_value\"},"
                + "{\"b\":\"b_value\"}"
                + "],"
                + "\"aMap\":{\"map_a\":\"map_a_value\"},"
                + "\"anEmptyList\":[],"
                + "\"nestedObj\":{\"nested_key\":\"nested_value\"}"
                + "}";
        assertEquals(expected, builder.string());
    }

    public void testSerialise() throws IOException {
        serialiser.startObject();
        serialiser.serialise(createSingleKeyValueSerialisable("a", "a_value"));
        serialiser.endObject();

        String expected = "{\"a\":\"a_value\"}";
        assertEquals(expected, builder.string());
    }

    public void testNewDotNotationReverser() {
        assertTrue(serialiser.newDotNotationReverser() instanceof ElasticsearchDotNotationReverser);
    }

    public void testAddReverserResults() throws IOException {
        Map<String, Object> results = new HashMap<>();
        results.put("a", "a_value");
        results.put("b", "b_value");

        DotNotationReverser reverser = mock(DotNotationReverser.class);
        when(reverser.getResultsMap()).thenReturn(results);

        serialiser.startObject();
        serialiser.addReverserResults(reverser);
        serialiser.endObject();

        String expected = "{\"a\":\"a_value\",\"b\":\"b_value\"}";
        assertEquals(expected, builder.string());
    }

    private static StorageSerialisable createSingleKeyValueSerialisable(String key, String value) {
        return s -> s.add(key, value);
    }
}
