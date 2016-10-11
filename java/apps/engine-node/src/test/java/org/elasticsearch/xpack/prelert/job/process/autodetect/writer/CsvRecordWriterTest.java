
package org.elasticsearch.xpack.prelert.job.process.autodetect.writer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.elasticsearch.test.ESTestCase;
import org.junit.Assert;

public class CsvRecordWriterTest extends ESTestCase {

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
