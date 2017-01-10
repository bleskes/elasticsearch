/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.ml.transforms;

import static org.elasticsearch.xpack.ml.transforms.TransformTestUtils.createIndexArray;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.apache.logging.log4j.Logger;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.job.condition.Condition;
import org.elasticsearch.xpack.ml.job.condition.Operator;
import org.elasticsearch.xpack.ml.transforms.Transform.TransformIndex;
import org.elasticsearch.xpack.ml.transforms.Transform.TransformResult;

public class ExcludeFilterNumericTests extends ESTestCase {

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
