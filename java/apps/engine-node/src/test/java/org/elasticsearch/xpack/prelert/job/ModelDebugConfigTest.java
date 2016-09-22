
package org.elasticsearch.xpack.prelert.job;

import org.elasticsearch.xpack.prelert.integration.hack.ESTestCase;
import org.elasticsearch.xpack.prelert.job.ModelDebugConfig.DebugDestination;
import org.junit.Test;

import static org.junit.Assert.*;

public class ModelDebugConfigTest extends ESTestCase {

    public void testIsEnabled_GivenNullBoundsPercentile() {
        ModelDebugConfig modelDebugConfig = new ModelDebugConfig();
        modelDebugConfig.setBoundsPercentile(null);

        assertFalse(modelDebugConfig.isEnabled());
    }


    public void testIsEnabled_GivenBoundsPercentile() {
        ModelDebugConfig modelDebugConfig = new ModelDebugConfig();
        modelDebugConfig.setBoundsPercentile(0.95);

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
}
