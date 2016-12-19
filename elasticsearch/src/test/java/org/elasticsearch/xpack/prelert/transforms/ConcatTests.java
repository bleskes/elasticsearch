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

public class ConcatTests extends ESTestCase {
    public void testMultipleInputs() throws TransformException {
        List<TransformIndex> readIndexes = createIndexArray(new TransformIndex(0, 1), new TransformIndex(0, 2), new TransformIndex(0, 4));
        List<TransformIndex> writeIndexes = createIndexArray(new TransformIndex(2, 1));

        Concat concat = new Concat(readIndexes, writeIndexes, mock(Logger.class));

        String[] input = {"a", "b", "c", "d", "e"};
        String[] scratch = {};
        String[] output = new String[2];
        String[][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.OK, concat.transform(readWriteArea));
        assertNull(output[0]);
        assertEquals("bce", output[1]);
    }

    public void testWithDelimiter() throws TransformException {
        List<TransformIndex> readIndexes = createIndexArray(new TransformIndex(0, 1), new TransformIndex(0, 2), new TransformIndex(0, 4));
        List<TransformIndex> writeIndexes = createIndexArray(new TransformIndex(2, 1));

        Concat concat = new Concat("--", readIndexes, writeIndexes, mock(Logger.class));

        String[] input = {"a", "b", "c", "d", "e"};
        String[] scratch = {};
        String[] output = new String[2];
        String[][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.OK, concat.transform(readWriteArea));
        assertNull(output[0]);
        assertEquals("b--c--e", output[1]);
    }

    public void testZeroInputs() throws TransformException {
        List<TransformIndex> readIndexes = createIndexArray();
        List<TransformIndex> writeIndexes = createIndexArray(new TransformIndex(2, 0));

        Concat concat = new Concat(readIndexes, writeIndexes, mock(Logger.class));

        String[] input = {"a", "b", "c", "d", "e"};
        String[] scratch = {};
        String[] output = new String[1];
        String[][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.OK, concat.transform(readWriteArea));
        assertEquals("", output[0]);
    }

    public void testNoOutput() throws TransformException {
        List<TransformIndex> readIndexes = createIndexArray(new TransformIndex(0, 1), new TransformIndex(0, 2), new TransformIndex(0, 3));
        List<TransformIndex> writeIndexes = createIndexArray();

        Concat concat = new Concat(readIndexes, writeIndexes, mock(Logger.class));

        String[] input = {"a", "b", "c", "d", "e"};
        String[] scratch = {};
        String[] output = new String[1];
        String[][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.FAIL, concat.transform(readWriteArea));
        assertNull(output[0]);
    }

    public void testScratchAreaInputs() throws TransformException {
        List<TransformIndex> readIndexes = createIndexArray(new TransformIndex(0, 1), new TransformIndex(0, 2),
                new TransformIndex(1, 0), new TransformIndex(1, 2));
        List<TransformIndex> writeIndexes = createIndexArray(new TransformIndex(1, 4));

        Concat concat = new Concat(readIndexes, writeIndexes, mock(Logger.class));

        String[] input = {"a", "b", "c", "d", "e"};
        String[] scratch = {"a", "b", "c", "d", null};
        String[] output = new String[1];
        String[][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.OK, concat.transform(readWriteArea));
        assertEquals("bcac", scratch[4]);
    }
}
