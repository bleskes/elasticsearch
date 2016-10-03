
package org.elasticsearch.xpack.prelert.job.quantiles;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.TestJsonStorageSerialiser;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;

import static org.junit.Assert.*;

public class QuantilesTest extends ESTestCase {

    public void testEquals_GivenSameObject() {
        Quantiles quantiles = new Quantiles();
        assertTrue(quantiles.equals(quantiles));
    }


    public void testEquals_GivenDifferentClassObject() {
        Quantiles quantiles = new Quantiles();
        assertFalse(quantiles.equals("not a quantiles object"));
    }


    public void testEquals_GivenEqualQuantilesObject() {
        Quantiles quantiles1 = new Quantiles();
        quantiles1.setQuantileState("foo");

        Quantiles quantiles2 = new Quantiles();
        quantiles2.setQuantileState("foo");

        assertTrue(quantiles1.equals(quantiles2));
        assertTrue(quantiles2.equals(quantiles1));
    }


    public void testEquals_GivenDifferentState() {
        Quantiles quantiles1 = new Quantiles();
        quantiles1.setQuantileState("bar1");

        Quantiles quantiles2 = new Quantiles();
        quantiles2.setQuantileState("bar2");

        assertFalse(quantiles1.equals(quantiles2));
        assertFalse(quantiles2.equals(quantiles1));
    }


    public void testHashCode_GivenEqualObject() {
        Quantiles quantiles1 = new Quantiles();
        quantiles1.setQuantileState("foo");

        Quantiles quantiles2 = new Quantiles();
        quantiles2.setQuantileState("foo");

        assertEquals(quantiles1.hashCode(), quantiles2.hashCode());
    }


    public void testSerialise() throws IOException {
        Quantiles quantiles = new Quantiles();
        quantiles.setTimestamp(new Date(1234L));
        quantiles.setQuantileState("foo");

        TestJsonStorageSerialiser serialiser = new TestJsonStorageSerialiser();
        serialiser.startObject();
        quantiles.serialise(serialiser);
        serialiser.endObject();

        String expected = "{"
                + "\"@timestamp\":1234,"
                + "\"quantileState\":\"foo\""
                + "}";
        assertEquals(expected, serialiser.toJson());
    }
}
