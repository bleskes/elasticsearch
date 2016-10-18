
package org.elasticsearch.xpack.prelert.job;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.job.ModelDebugConfig.DebugDestination;
import org.elasticsearch.xpack.prelert.support.AbstractSerializingTestCase;

public class ModelDebugConfigTest extends AbstractSerializingTestCase<ModelDebugConfig> {

    public void testIsEnabled_GivenNullBoundsPercentile() {
        ModelDebugConfig modelDebugConfig = new ModelDebugConfig();

        assertFalse(modelDebugConfig.isEnabled());
    }


    public void testIsEnabled_GivenBoundsPercentile() {
        ModelDebugConfig modelDebugConfig = new ModelDebugConfig(0.95, null);

        assertTrue(modelDebugConfig.isEnabled());
    }


    public void testEquals() {
        assertFalse(new ModelDebugConfig().equals(null));
        assertFalse(new ModelDebugConfig().equals("a string"));
        assertFalse(new ModelDebugConfig(80.0, "").equals(new ModelDebugConfig(81.0, "")));
        assertFalse(new ModelDebugConfig(80.0, "foo").equals(new ModelDebugConfig(80.0, "bar")));
        assertFalse(new ModelDebugConfig(DebugDestination.FILE, 80.0, "foo").equals(new ModelDebugConfig(DebugDestination.DATA_STORE, 80.0, "foo")));

        ModelDebugConfig modelDebugConfig = new ModelDebugConfig();
        assertTrue(modelDebugConfig.equals(modelDebugConfig));
        assertTrue(new ModelDebugConfig().equals(new ModelDebugConfig()));
        assertTrue(new ModelDebugConfig(80.0, "foo").equals(new ModelDebugConfig(80.0, "foo")));
        assertTrue(new ModelDebugConfig(DebugDestination.FILE, 80.0, "foo").equals(new ModelDebugConfig(80.0, "foo")));
        assertTrue(new ModelDebugConfig(DebugDestination.DATA_STORE, 80.0, "foo").equals(new ModelDebugConfig(DebugDestination.DATA_STORE, 80.0, "foo")));
    }


    public void testHashCode() {
        assertEquals(new ModelDebugConfig(80.0, "foo").hashCode(),
                new ModelDebugConfig(80.0, "foo").hashCode());
        assertEquals(new ModelDebugConfig(DebugDestination.FILE, 80.0, "foo").hashCode(),
                new ModelDebugConfig(80.0, "foo").hashCode());
        assertEquals(new ModelDebugConfig(DebugDestination.DATA_STORE, 80.0, "foo").hashCode(),
                new ModelDebugConfig(DebugDestination.DATA_STORE, 80.0, "foo").hashCode());
    }

    @Override
    protected ModelDebugConfig createTestInstance() {
        return new ModelDebugConfig(randomFrom(DebugDestination.values()), randomDouble(), randomAsciiOfLengthBetween(1, 30));
    }

    @Override
    protected Reader<ModelDebugConfig> instanceReader() {
        return ModelDebugConfig::new;
    }

    @Override
    protected ModelDebugConfig parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return ModelDebugConfig.PARSER.apply(parser, () -> matcher);
    }
}
