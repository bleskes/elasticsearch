
package org.elasticsearch.xpack.prelert.job.transform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.elasticsearch.test.ESTestCase;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TransformSerialisationTest extends ESTestCase {

    public void testDeserialise_singleFieldAsArray() throws JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectReader reader = mapper.readerFor(TransformConfig.class)
                .with(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        String json = "{\"inputs\":\"dns\", \"transform\":\"highest_registered_domain\"}";
        TransformConfig tr = reader.readValue(json);

        assertEquals(1, tr.getInputs().size());
        assertEquals("dns", tr.getInputs().get(0));
        assertEquals("highest_registered_domain", tr.getTransform());
        assertEquals(0, tr.getOutputs().size());


        json = "{\"inputs\":\"dns\", \"transform\":\"highest_registered_domain\", \"outputs\":\"catted\"}";
        tr = reader.readValue(json);

        assertEquals(1, tr.getInputs().size());
        assertEquals("dns", tr.getInputs().get(0));
        assertEquals("highest_registered_domain", tr.getTransform());
        assertEquals(1, tr.getOutputs().size());
        assertEquals("catted", tr.getOutputs().get(0));
    }


    public void testDeserialise_fieldsArray() throws JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectReader reader = mapper.readerFor(TransformConfig.class)
                .with(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        String json = "{\"inputs\":[\"dns\"], \"transform\":\"highest_registered_domain\"}";
        TransformConfig tr = reader.readValue(json);

        assertEquals(1, tr.getInputs().size());
        assertEquals("dns", tr.getInputs().get(0));
        assertEquals("highest_registered_domain", tr.getTransform());

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
