
package org.elasticsearch.xpack.prelert.transforms;

import static org.elasticsearch.xpack.prelert.transforms.TransformTestUtils.createIndexArray;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.apache.logging.log4j.Logger;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.condition.Condition;
import org.elasticsearch.xpack.prelert.job.condition.Operator;
import org.elasticsearch.xpack.prelert.transforms.Transform.TransformIndex;
import org.elasticsearch.xpack.prelert.transforms.Transform.TransformResult;

public class ExcludeFilterNumericTest extends ESTestCase {
    
    public void testEq()
            throws TransformException {
        ExcludeFilterNumeric transform = createTransform(Operator.EQ, "5.0");

        String[] input = {"5"};
        String[] scratch = {};
        String[] output = {};
        String[][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.EXCLUDE, transform.transform(readWriteArea));

        input[0] = "5.10000";
        assertEquals(TransformResult.OK, transform.transform(readWriteArea));
    }

    
    public void testGT()
            throws TransformException {
        ExcludeFilterNumeric transform = createTransform(Operator.GT, "10.000");

        String[] input = {"100"};
        String[] scratch = {};
        String[] output = {};
        String[][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.EXCLUDE, transform.transform(readWriteArea));

        input[0] = "1.0";
        assertEquals(TransformResult.OK, transform.transform(readWriteArea));
    }

    
    public void testGTE()
            throws TransformException {
        ExcludeFilterNumeric transform = createTransform(Operator.GTE, "10.000");

        String[] input = {"100"};
        String[] scratch = {};
        String[] output = {};
        String[][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.EXCLUDE, transform.transform(readWriteArea));

        input[0] = "10";
        assertEquals(TransformResult.EXCLUDE, transform.transform(readWriteArea));

        input[0] = "9.5";
        assertEquals(TransformResult.OK, transform.transform(readWriteArea));
    }

    
    public void testLT()
            throws TransformException {
        ExcludeFilterNumeric transform = createTransform(Operator.LT, "2000");

        String[] input = {"100.2"};
        String[] scratch = {};
        String[] output = {};
        String[][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.EXCLUDE, transform.transform(readWriteArea));

        input[0] = "2005.0000";
        assertEquals(TransformResult.OK, transform.transform(readWriteArea));
    }

    
    public void testLTE()
            throws TransformException {
        ExcludeFilterNumeric transform = createTransform(Operator.LTE, "2000");

        String[] input = {"100.2"};
        String[] scratch = {};
        String[] output = {};
        String[][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.EXCLUDE, transform.transform(readWriteArea));

        input[0] = "2000.0000";
        assertEquals(TransformResult.EXCLUDE, transform.transform(readWriteArea));

        input[0] = "9000.5";
        assertEquals(TransformResult.OK, transform.transform(readWriteArea));
    }

    private ExcludeFilterNumeric createTransform(Operator op, String filterValue) {
        Condition condition = new Condition(op, filterValue);
        List<TransformIndex> readIndexes = createIndexArray(new TransformIndex(0, 0));
        List<TransformIndex> writeIndexes = createIndexArray();

        return new ExcludeFilterNumeric(condition, readIndexes, writeIndexes, mock(Logger.class));
    }
}
