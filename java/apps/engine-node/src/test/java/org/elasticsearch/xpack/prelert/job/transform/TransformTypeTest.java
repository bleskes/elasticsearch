
package org.elasticsearch.xpack.prelert.job.transform;

import org.elasticsearch.test.ESTestCase;

import java.util.EnumSet;
import java.util.Set;

public class TransformTypeTest extends ESTestCase {

    public void testFromString() {
        Set<TransformType> all = EnumSet.allOf(TransformType.class);

        for (TransformType type : all) {
            assertEquals(type.prettyName(), type.toString());

            TransformType created = TransformType.fromString(type.prettyName());
            assertEquals(type, created);
        }
    }

    public void testFromString_UnknownType() {
        ESTestCase.expectThrows(IllegalArgumentException.class, () -> TransformType.fromString("random_type"));
    }
}
