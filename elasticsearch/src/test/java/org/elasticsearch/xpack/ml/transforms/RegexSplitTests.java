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
import org.elasticsearch.xpack.ml.transforms.Transform.TransformIndex;
import org.elasticsearch.xpack.ml.transforms.Transform.TransformResult;

public class RegexSplitTests extends ESTestCase {

    public void testTransform() throws TransformException {
        List<TransformIndex> readIndexes = createIndexArray(new TransformIndex(0, 0));
        List<TransformIndex> writeIndexes = createIndexArray(new TransformIndex(2, 0),
                new TransformIndex(2, 1), new TransformIndex(2, 2));

        String regex = ":";

        RegexSplit transform = new RegexSplit(regex, readIndexes, writeIndexes, mock(Logger.class));

        String[] input = {"A:B:C"};
        String[] scratch = {};
        String[] output = new String[3];
        String[][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.OK, transform.transform(readWriteArea));
        assertArrayEquals(readWriteArea[2], new String[]{"A", "B", "C"});

        readWriteArea[0] = new String[]{"A:B:C:D"};
        readWriteArea[2] = new String[]{"", "", ""};
        assertEquals(TransformResult.OK, transform.transform(readWriteArea));
        assertArrayEquals(readWriteArea[2], new String[]{"A", "B", "C"});


        readWriteArea[0] = new String[]{"A"};
        readWriteArea[2] = new String[]{""};
        assertEquals(TransformResult.OK, transform.transform(readWriteArea));
        assertArrayEquals(readWriteArea[2], new String[]{"A"});

        readWriteArea[0] = new String[]{""};
        readWriteArea[2] = new String[]{""};
        assertEquals(TransformResult.OK, transform.transform(readWriteArea));
        assertArrayEquals(readWriteArea[2], new String[]{""});
    }
}
