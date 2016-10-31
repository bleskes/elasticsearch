
package org.elasticsearch.xpack.prelert.job.transform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.elasticsearch.test.ESTestCase;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TransformSerialisationTests extends ESTestCase {

    public void testDeserialise_singleFieldAsArray() throws JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectReader reader = mapper.readerFor(TransformConfig.class)
                .with(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        String json = "{\"inputs\":\"dns\", \"transform\":\"domain_split\"}";
        TransformConfig tr = reader.readValue(json);

        assertEquals(1, tr.getInputs().size());
        assertEquals("dns", tr.getInputs().get(0));
        assertEquals("domain_split", tr.getTransform());
        assertEquals(2, tr.getOutputs().size());
        assertEquals("subDomain", tr.getOutputs().get(0));
        assertEquals("hrd", tr.getOutputs().get(1));


        json = "{\"inputs\":\"dns\", \"transform\":\"domain_split\", \"outputs\":\"catted\"}";
        tr = reader.readValue(json);

        assertEquals(1, tr.getInputs().size());
        assertEquals("dns", tr.getInputs().get(0));
        assertEquals("domain_split", tr.getTransform());
        assertEquals(1, tr.getOutputs().size());
        assertEquals("catted", tr.getOutputs().get(0));
    }


    public void testDeserialise_fieldsArray() throws JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectReader reader = mapper.readerFor(TransformConfig.class)
                .with(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        String json = "{\"inputs\":[\"dns\"], \"transform\":\"domain_split\"}";
        TransformConfig tr = reader.readValue(json);

        assertEquals(1, tr.getInputs().size());
        assertEquals("dns", tr.getInputs().get(0));
        assertEquals("domain_split", tr.getTransform());

        json = "{\"inputs\":[\"a\", \"b\", \"c\"], \"transform\":\"concat\", \"outputs\":[\"catted\"]}";
        tr = reader.readValue(json);

        assertEquals(3, tr.getInputs().size());
        assertEquals("a", tr.getInputs().get(0));
        assertEquals("b", tr.getInputs().get(1));
        assertEquals("c", tr.getInputs().get(2));
        assertEquals("concat", tr.getTransform());
        assertEquals(1, tr.getOutputs().size());
        assertEquals("catted", tr.getOutputs().get(0));
    }
}
