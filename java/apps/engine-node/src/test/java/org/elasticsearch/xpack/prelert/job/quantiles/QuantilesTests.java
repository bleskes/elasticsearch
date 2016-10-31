
package org.elasticsearch.xpack.prelert.job.quantiles;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.TestJsonStorageSerialisers;
import org.elasticsearch.xpack.prelert.support.AbstractSerializingTestCase;

import java.io.IOException;
import java.util.Date;

public class QuantilesTests extends AbstractSerializingTestCase<Quantiles> {

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

        TestJsonStorageSerialisers serialiser = new TestJsonStorageSerialisers();
        serialiser.startObject();
        quantiles.serialise(serialiser);
        serialiser.endObject();

        String expected = "{"
                + "\"@timestamp\":1234,"
                + "\"quantileState\":\"foo\""
                + "}";
        assertEquals(expected, serialiser.toJson());
    }

    @Override
    protected Quantiles createTestInstance() {
        Quantiles quantiles = new Quantiles();
        if (randomBoolean()) {
            quantiles.setTimestamp(new Date(randomLong()));
        }
        if (randomBoolean()) {
            quantiles.setQuantileState(randomAsciiOfLengthBetween(0, 1000));
        }
        return quantiles;
    }

    @Override
    protected Reader<Quantiles> instanceReader() {
        return Quantiles::new;
    }

    @Override
    protected Quantiles parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return Quantiles.PARSER.apply(parser, () -> matcher);
    }
}
