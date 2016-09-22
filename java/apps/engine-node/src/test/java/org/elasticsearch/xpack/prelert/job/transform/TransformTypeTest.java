
package org.elasticsearch.xpack.prelert.job.transform;

import org.elasticsearch.xpack.prelert.integration.hack.ESTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;


public class TransformTypeTest extends ESTestCase {
    @Rule
    public ExpectedException thrown = ExpectedException.none();


    public void testFromString() {
        Set<TransformType> all = EnumSet.allOf(TransformType.class);

        for (TransformType type : all) {
            assertEquals(type.prettyName(), type.toString());

            TransformType created = TransformType.fromString(type.prettyName());
            assertEquals(type, created);
        }
    }

    public void testFromString_UnknownType() {
        thrown.expect(IllegalArgumentException.class);
        TransformType.fromString("random_type");
    }
}
