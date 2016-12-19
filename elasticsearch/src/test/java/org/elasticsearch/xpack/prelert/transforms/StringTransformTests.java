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

public class StringTransformTests extends ESTestCase {
    public void testUpperCaseTransform_GivenZeroInputs() throws TransformException {
        List<TransformIndex> readIndexes = createIndexArray();
        List<TransformIndex> writeIndexes = createIndexArray(new TransformIndex(2, 1));

        ESTestCase.expectThrows(IllegalArgumentException.class,
                () -> StringTransform.createUpperCase(readIndexes, writeIndexes, mock(Logger.class)));
    }

    public void testUpperCaseTransform_GivenTwoInputs() throws TransformException {
        List<TransformIndex> readIndexes = createIndexArray(
                new TransformIndex(0, 0), new TransformIndex(0, 1));
        List<TransformIndex> writeIndexes = createIndexArray(new TransformIndex(2, 1));

        ESTestCase.expectThrows(IllegalArgumentException.class,
                () -> StringTransform.createUpperCase(readIndexes, writeIndexes, mock(Logger.class)));
    }

    public void testUpperCaseTransform_GivenZeroOutputs() throws TransformException {
        List<TransformIndex> readIndexes = createIndexArray(new TransformIndex(0, 1));
        List<TransformIndex> writeIndexes = createIndexArray();

        ESTestCase.expectThrows(IllegalArgumentException.class,
                () -> StringTransform.createUpperCase(readIndexes, writeIndexes, mock(Logger.class)));
    }

    public void testUpperCaseTransform_GivenTwoOutputs() throws TransformException {
        List<TransformIndex> readIndexes = createIndexArray(new TransformIndex(0, 1));
        List<TransformIndex> writeIndexes = createIndexArray(
                new TransformIndex(1, 1), new TransformIndex(1, 2));

        ESTestCase.expectThrows(IllegalArgumentException.class,
                () -> StringTransform.createUpperCase(readIndexes, writeIndexes, mock(Logger.class)));
    }

    public void testUpperCaseTransform_GivenSingleInputAndSingleOutput() throws TransformException {
        List<TransformIndex> readIndexes = createIndexArray(new TransformIndex(0, 1));
        List<TransformIndex> writeIndexes = createIndexArray(new TransformIndex(2, 0));

        StringTransform upperCase = StringTransform.createUpperCase(readIndexes, writeIndexes,
                mock(Logger.class));

        String[] input = {"aa", "aBcD", "cc", "dd", "ee"};
        String[] scratch = {};
        String[] output = new String[1];
        String[][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.OK, upperCase.transform(readWriteArea));
        assertEquals("aBcD".toUpperCase(Locale.ROOT), output[0]);
    }

    public void testLowerCaseTransform_GivenZeroInputs() throws TransformException {
        List<TransformIndex> readIndexes = createIndexArray();
        List<TransformIndex> writeIndexes = createIndexArray(new TransformIndex(2, 1));

        ESTestCase.expectThrows(IllegalArgumentException.class,
                () -> StringTransform.createLowerCase(readIndexes, writeIndexes, mock(Logger.class)));
    }

    public void testLowerCaseTransform_GivenTwoInputs() throws TransformException {
        List<TransformIndex> readIndexes = createIndexArray(
                new TransformIndex(0, 0), new TransformIndex(0, 1));
        List<TransformIndex> writeIndexes = createIndexArray(new TransformIndex(2, 1));

        ESTestCase.expectThrows(IllegalArgumentException.class,
                () -> StringTransform.createLowerCase(readIndexes, writeIndexes, mock(Logger.class)));
    }

    public void testLowerCaseTransform_GivenZeroOutputs() throws TransformException {
        List<TransformIndex> readIndexes = createIndexArray(new TransformIndex(0, 1));
        List<TransformIndex> writeIndexes = createIndexArray();

        ESTestCase.expectThrows(IllegalArgumentException.class,
                () -> StringTransform.createLowerCase(readIndexes, writeIndexes, mock(Logger.class)));
    }

    public void testLowerCaseTransform_GivenTwoOutputs() throws TransformException {
        List<TransformIndex> readIndexes = createIndexArray(new TransformIndex(0, 1));
        List<TransformIndex> writeIndexes = createIndexArray(
                new TransformIndex(1, 1), new TransformIndex(1, 2));

        ESTestCase.expectThrows(IllegalArgumentException.class,
                () -> StringTransform.createLowerCase(readIndexes, writeIndexes, mock(Logger.class)));
    }

    public void testLowerCaseTransform_GivenSingleInputAndSingleOutput() throws TransformException {
        List<TransformIndex> readIndexes = createIndexArray(new TransformIndex(0, 1));
        List<TransformIndex> writeIndexes = createIndexArray(new TransformIndex(2, 0));

        StringTransform upperCase = StringTransform.createLowerCase(readIndexes, writeIndexes,
                mock(Logger.class));

        String[] input = {"aa", "AbCde", "cc", "dd", "ee"};
        String[] scratch = {};
        String[] output = new String[1];
        String[][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.OK, upperCase.transform(readWriteArea));
        assertEquals("AbCde".toLowerCase(Locale.ROOT), output[0]);
    }

    public void testTrimTransform_GivenZeroInputs() throws TransformException {
        List<TransformIndex> readIndexes = createIndexArray();
        List<TransformIndex> writeIndexes = createIndexArray(new TransformIndex(2, 1));

        ESTestCase.expectThrows(IllegalArgumentException.class,
                () -> StringTransform.createTrim(readIndexes, writeIndexes, mock(Logger.class)));
    }

    public void testTrimTransform_GivenTwoInputs() throws TransformException {
        List<TransformIndex> readIndexes = createIndexArray(
                new TransformIndex(0, 0), new TransformIndex(0, 1));
        List<TransformIndex> writeIndexes = createIndexArray(new TransformIndex(2, 1));

        ESTestCase.expectThrows(IllegalArgumentException.class,
                () -> StringTransform.createTrim(readIndexes, writeIndexes, mock(Logger.class)));
    }

    public void testTrimTransform_GivenZeroOutputs() throws TransformException {
        List<TransformIndex> readIndexes = createIndexArray(new TransformIndex(0, 1));
        List<TransformIndex> writeIndexes = createIndexArray();

        ESTestCase.expectThrows(IllegalArgumentException.class,
                () -> StringTransform.createTrim(readIndexes, writeIndexes, mock(Logger.class)));
    }

    public void testTrimTransform_GivenTwoOutputs() throws TransformException {
        List<TransformIndex> readIndexes = createIndexArray(new TransformIndex(0, 1));
        List<TransformIndex> writeIndexes = createIndexArray(
                new TransformIndex(1, 1), new TransformIndex(1, 2));

        ESTestCase.expectThrows(IllegalArgumentException.class,
                () -> StringTransform.createTrim(readIndexes, writeIndexes, mock(Logger.class)));
    }

    public void testTrimTransform_GivenSingleInputAndSingleOutput() throws TransformException {
        List<TransformIndex> readIndexes = createIndexArray(new TransformIndex(0, 1));
        List<TransformIndex> writeIndexes = createIndexArray(new TransformIndex(2, 0));

        StringTransform upperCase = StringTransform.createTrim(readIndexes, writeIndexes,
                mock(Logger.class));

        String[] input = {"  a ", "\t b ", " c", "d", "e"};
        String[] scratch = {};
        String[] output = new String[1];
        String[][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.OK, upperCase.transform(readWriteArea));
        assertEquals("\t b".trim(), output[0]);
    }
}
