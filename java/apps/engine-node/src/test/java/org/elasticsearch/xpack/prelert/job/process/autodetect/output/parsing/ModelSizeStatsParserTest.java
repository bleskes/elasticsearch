package org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.job.ModelSizeStats.MemoryStatus;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;

public class ModelSizeStatsParserTest extends ESTestCase {
    public void testParse() throws IOException {
        String input = "{\"modelBytes\": 1,"
                + "\"totalByFieldCount\" : 2,"
                + "\"totalOverFieldCount\" : 3,"
                + "\"totalPartitionFieldCount\" : 4,"
                + "\"bucketAllocationFailuresCount\" : 5,"
                + "\"memoryStatus\" : \"OK\","
                + "\"bucketTime\" : 1444333321"
                + "}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        Date d1 = new Date();
        ModelSizeStats stats = new ModelSizeStatsParser(parser).parseJson();
        Date d2 = new Date();

        assertEquals(1L, stats.getModelBytes());
        assertEquals(2L, stats.getTotalByFieldCount());
        assertEquals(3L, stats.getTotalOverFieldCount());
        assertEquals(4L, stats.getTotalPartitionFieldCount());
        assertEquals(5L, stats.getBucketAllocationFailuresCount());
        assertEquals(1444333321000L, stats.getTimestamp().getTime());
        assertTrue(stats.getLogTime().getTime() >= d1.getTime());
        assertTrue(stats.getLogTime().getTime() <= d2.getTime());
        assertEquals(MemoryStatus.OK, stats.getMemoryStatus());
    }

    private static final JsonParser createJsonParser(String input) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        return new JsonFactory().createParser(inputStream);
    }
}
