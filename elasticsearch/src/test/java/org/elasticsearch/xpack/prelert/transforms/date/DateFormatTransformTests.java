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

import org.elasticsearch.xpack.prelert.transforms.Transform.TransformIndex;
import org.elasticsearch.xpack.prelert.transforms.TransformException;

public class DateFormatTransformTests extends ESTestCase {

    public void testTransform_GivenValidTimestamp() throws TransformException {
        List<TransformIndex> readIndexes = createIndexArray(new TransformIndex(0, 0));
        List<TransformIndex> writeIndexes = createIndexArray(new TransformIndex(2, 0));

        DateFormatTransform transformer = new DateFormatTransform("yyyy-MM-dd HH:mm:ss.SSSXXX",
                ZoneOffset.systemDefault(), readIndexes, writeIndexes, mock(Logger.class));

        String[] input = {"2014-01-01 13:42:56.500Z"};
        String[] scratch = {};
        String[] output = new String[1];
        String[][] readWriteArea = {input, scratch, output};

        transformer.transform(readWriteArea);

        assertEquals(1388583776500L, transformer.epochMs());
        assertEquals("1388583776", output[0]);
    }

    public void testTransform_GivenInvalidFormat() throws TransformException {

        IllegalArgumentException e = ESTestCase.expectThrows(IllegalArgumentException.class,
                () -> new DateFormatTransform("yyyy-MM HH:mm:ss", ZoneOffset.systemDefault(),
                        Collections.emptyList(), Collections.emptyList(), mock(Logger.class)));

        assertEquals("Timestamp cannot be derived from pattern: yyyy-MM HH:mm:ss", e.getMessage());

    }

    public void testTransform_GivenInvalidTimestamp() throws TransformException {

        List<TransformIndex> readIndexes = createIndexArray(new TransformIndex(0, 0));
        List<TransformIndex> writeIndexes = createIndexArray(new TransformIndex(2, 0));

        DateFormatTransform transformer = new DateFormatTransform("yyyy-MM-dd HH:mm:ss",
                ZoneOffset.systemDefault(), readIndexes, writeIndexes, mock(Logger.class));

        String[] input = {"invalid"};
        String[] scratch = {};
        String[] output = new String[1];
        String[][] readWriteArea = {input, scratch, output};

        ParseTimestampException e = ESTestCase.expectThrows(ParseTimestampException.class,
                () -> transformer.transform(readWriteArea));

        assertEquals("Cannot parse date 'invalid' with format string 'yyyy-MM-dd HH:mm:ss'", e.getMessage());
    }

    public void testTransform_GivenNull() throws TransformException {

        List<TransformIndex> readIndexes = createIndexArray(new TransformIndex(1, 0));
        List<TransformIndex> writeIndexes = createIndexArray(new TransformIndex(2, 0));

        DateFormatTransform transformer = new DateFormatTransform("yyyy-MM-dd HH:mm:ss",
                ZoneOffset.systemDefault(), readIndexes, writeIndexes, mock(Logger.class));

        String[] input = {};
        String[] scratch = {null};
        String[] output = new String[1];
        String[][] readWriteArea = {input, scratch, output};

        ESTestCase.expectThrows(ParseTimestampException.class, () -> transformer.transform(readWriteArea));
    }

    public void testTransform_GivenBadFormat() throws TransformException {

        List<TransformIndex> readIndexes = createIndexArray(new TransformIndex(0, 0));
        List<TransformIndex> writeIndexes = createIndexArray(new TransformIndex(2, 0));

        ESTestCase.expectThrows(IllegalArgumentException.class,
                () -> new DateFormatTransform("e", ZoneOffset.systemDefault(), readIndexes, writeIndexes, mock(Logger.class)));
    }

    public void testTransform_FromScratchArea() throws TransformException {
        List<TransformIndex> readIndexes = createIndexArray(new TransformIndex(1, 0));
        List<TransformIndex> writeIndexes = createIndexArray(new TransformIndex(2, 0));

        DateFormatTransform transformer = new DateFormatTransform("yyyy-MM-dd HH:mm:ssXXX",
                ZoneOffset.systemDefault(), readIndexes, writeIndexes, mock(Logger.class));

        String[] input = {};
        String[] scratch = {"2014-01-01 00:00:00Z"};
        String[] output = new String[1];
        String[][] readWriteArea = {input, scratch, output};

        transformer.transform(readWriteArea);

        assertEquals(1388534400000L, transformer.epochMs());
        assertEquals("1388534400", output[0]);
    }

    public void testTransform_WithBrackets() throws TransformException {
        List<TransformIndex> readIndexes = createIndexArray(new TransformIndex(0, 0));
        List<TransformIndex> writeIndexes = createIndexArray(new TransformIndex(2, 0));

        DateFormatTransform transformer = new DateFormatTransform("'['yyyy-MM-dd HH:mm:ssX']'",
                ZoneOffset.systemDefault(), readIndexes, writeIndexes, mock(Logger.class));

        String[] input = {"[2014-06-23 00:00:00Z]"};
        String[] scratch = {};
        String[] output = new String[1];
        String[][] readWriteArea = {input, scratch, output};

        transformer.transform(readWriteArea);
    }
}
