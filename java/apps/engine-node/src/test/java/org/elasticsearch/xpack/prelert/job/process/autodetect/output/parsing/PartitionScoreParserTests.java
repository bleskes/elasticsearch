/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
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
package org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.results.PartitionScore;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PartitionScoreParserTests extends ESTestCase {

    public void testParse() throws IOException {
        String json = "{\"probability\":0.1,\"partitionFieldName\":\"part1\"," +
                "\"partitionFieldValue\":\"p0\",\"normalizedProbability\":0.2}";

        JsonParser parser = createJsonParser(json);
        parser.nextToken();
        PartitionScore score = new PartitionScoreParser(parser).parseJson();

        assertEquals("part1", score.getPartitionFieldName());
        assertEquals("p0", score.getPartitionFieldValue());
        assertEquals(0.2, score.getAnomalyScore(), 0.0001);
        assertEquals(0.1, score.getProbability(), 0.0001);
    }

    private static JsonParser createJsonParser(String input) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        return new JsonFactory().createParser(inputStream);
    }

}
