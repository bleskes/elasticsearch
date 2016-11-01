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
import org.elasticsearch.xpack.prelert.job.results.BucketInfluencer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class BucketInfluencerParserTests extends ESTestCase {
    private static final double ERROR = 0.0001;


    public void testParseJson() throws IOException {
        String json = "{"
                + "\"probability\": 0.2,"
                + "\"initialAnomalyScore\": 10.0,"
                + "\"rawAnomalyScore\": 3.2,"
                + "\"influencerFieldName\": \"inf-name\""
                + "}";

        JsonParser parser = createJsonParser(json);
        parser.nextToken();
        BucketInfluencer influencer = new BucketInfluencerParser(parser).parseJson();

        assertEquals("inf-name", influencer.getInfluencerFieldName());
        assertEquals(0.2, influencer.getProbability(), ERROR);
        assertEquals(10.0, influencer.getInitialAnomalyScore(), ERROR);
        assertEquals(10.0, influencer.getAnomalyScore(), ERROR);
        assertEquals(3.2, influencer.getRawAnomalyScore(), ERROR);
    }


    public void testParseJson_GivenUnexpectedField() throws IOException {
        String json = "{"
                + "\"unexpected\": 0.2"
                + "}";

        JsonParser parser = createJsonParser(json);
        parser.nextToken();
        BucketInfluencer influencer = new BucketInfluencerParser(parser).parseJson();
        assertEquals(new BucketInfluencer(), influencer);
    }

    private static JsonParser createJsonParser(String input) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        return new JsonFactory().createParser(inputStream);
    }

}
