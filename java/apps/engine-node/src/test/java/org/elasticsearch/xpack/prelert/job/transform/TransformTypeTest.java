
package org.elasticsearch.xpack.prelert.job.transform;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;


public class TransformTypeTest {
    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void testFromString() {
        Set<TransformType> all = EnumSet.allOf(TransformType.class);

        for (TransformType type : all) {
            assertEquals(type.prettyName(), type.toString());

            TransformType created = TransformType.fromString(type.prettyName());
            assertEquals(type, created);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromString_UnknownType() {
        @SuppressWarnings("unused")
        TransformType created = TransformType.fromString("random_type");
    }
}
