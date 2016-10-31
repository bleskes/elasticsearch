
package org.elasticsearch.xpack.prelert.job.persistence;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.test.ESTestCase;


public class ElasticsearchDotNotationReverserTests extends ESTestCase {
    public void testResultsMap() throws JsonProcessingException {
        ElasticsearchDotNotationReverser reverser = createReverser();

        String expected = "{\"complex\":{\"nested\":{\"structure\":{\"first\":\"x\"," +
                "\"second\":\"y\"},\"value\":\"z\"}},\"cpu\":{\"system\":\"5\"," +
                "\"user\":\"10\",\"wait\":\"1\"},\"simple\":\"simon\"}";

        String actual = new ObjectMapper().writeValueAsString(reverser.getResultsMap());
        assertEquals(expected, actual);
    }

    public void testMappingsMap() throws JsonProcessingException {
        ElasticsearchDotNotationReverser reverser = createReverser();

        String expected = "{\"complex\":{\"properties\":{\"nested\":{\"properties\":" +
                "{\"structure\":{\"properties\":{\"first\":{\"type\":\"keyword\"}," +
                "\"second\":{\"type\":\"keyword\"}},\"type\":\"object\"}," +
                "\"value\":{\"type\":\"keyword\"}},\"type\":\"object\"}}," +
                "\"type\":\"object\"},\"cpu\":{\"properties\":{\"system\":" +
                "{\"type\":\"keyword\"},\"user\":{\"type\":\"keyword\"}," +
                "\"wait\":{\"type\":\"keyword\"}},\"type\":\"object\"}," +
                "\"simple\":{\"type\":\"keyword\"}}";

        String actual = new ObjectMapper().writeValueAsString(reverser.getMappingsMap());
        assertEquals(expected, actual);
    }

    private ElasticsearchDotNotationReverser createReverser() {
        ElasticsearchDotNotationReverser reverser = new ElasticsearchDotNotationReverser();
        // This should get ignored as it's a reserved field name
        reverser.add("bucketSpan", "3600");
        reverser.add("simple", "simon");
        reverser.add("cpu.user", "10");
        reverser.add("cpu.system", "5");
        reverser.add("cpu.wait", "1");
        // This should get ignored as one of its segments is a reserved field name
        reverser.add("foo.bucketSpan", "3600");
        reverser.add("complex.nested.structure.first", "x");
        reverser.add("complex.nested.structure.second", "y");
        reverser.add("complex.nested.value", "z");
        return reverser;
    }
}
