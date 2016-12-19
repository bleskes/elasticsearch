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
    public void testWriteArray() throws IOException {
        String[] header = {"one", "two", "three", "four", "five"};
        String[] record1 = {"r1", "r2", "", "rrr4", "r5"};
        String[] record2 = {"y1", "y2", "yy3", "yyy4", "y5"};

        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);

        CsvRecordWriter writer = new CsvRecordWriter(bos);
        writer.writeRecord(header);

        // write the same record this number of times
        final int NUM_RECORDS = 1;
        for (int i = 0; i < NUM_RECORDS; i++) {
            writer.writeRecord(record1);
            writer.writeRecord(record2);
        }
        writer.flush();

        String output = new String(bos.toByteArray(), StandardCharsets.UTF_8);
        String[] lines = output.split("\\r?\\n");
        Assert.assertEquals(1 + NUM_RECORDS * 2, lines.length);

        String[] fields = lines[0].split(",");
        Assert.assertArrayEquals(fields, header);
        for (int i = 1; i < NUM_RECORDS; ) {
            fields = lines[i++].split(",");
            Assert.assertArrayEquals(fields, record1);
            fields = lines[i++].split(",");
            Assert.assertArrayEquals(fields, record2);
        }
    }

    public void testWriteList() throws IOException {
        String[] header = {"one", "two", "three", "four", "five"};
        String[] record1 = {"r1", "r2", "", "rrr4", "r5"};
        String[] record2 = {"y1", "y2", "yy3", "yyy4", "y5"};

        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);

        CsvRecordWriter writer = new CsvRecordWriter(bos);
        writer.writeRecord(Arrays.asList(header));

        // write the same record this number of times
        final int NUM_RECORDS = 1;
        for (int i = 0; i < NUM_RECORDS; i++) {
            writer.writeRecord(Arrays.asList(record1));
            writer.writeRecord(Arrays.asList(record2));
        }
        writer.flush();

        String output = new String(bos.toByteArray(), StandardCharsets.UTF_8);
        String[] lines = output.split("\\r?\\n");
        Assert.assertEquals(1 + NUM_RECORDS * 2, lines.length);

        String[] fields = lines[0].split(",");
        Assert.assertArrayEquals(fields, header);
        for (int i = 1; i < NUM_RECORDS; ) {
            fields = lines[i++].split(",");
            Assert.assertArrayEquals(fields, record1);
            fields = lines[i++].split(",");
            Assert.assertArrayEquals(fields, record2);
        }
    }

}
