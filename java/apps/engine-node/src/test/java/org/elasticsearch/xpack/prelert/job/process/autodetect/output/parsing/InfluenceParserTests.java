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
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.results.Influence;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class InfluenceParserTests extends ESTestCase {
    private class InfluenceFieldComparator implements Comparator<Influence> {
        @Override
        public int compare(Influence o1, Influence o2) {
            return o1.getInfluencerFieldName().compareTo(o2.getInfluencerFieldName());
        }
    }

    public void testParseJson() throws IOException {
        String json = "{"
                + "\"host\": [\"web-server\", \"localhost\"]"
                + "}";

        JsonParser parser = createJsonParser(json);
        parser.nextToken();
        List<Influence> infs = new InfluenceParser(parser).parseJson();

        assertEquals(1, infs.size());
        Influence inf = infs.get(0);

        assertEquals("host", inf.getInfluencerFieldName());
        assertEquals(2, inf.getInfluencerFieldValues().size());
        assertEquals("web-server", inf.getInfluencerFieldValues().get(0));
        assertEquals("localhost", inf.getInfluencerFieldValues().get(1));
    }

    public void testParseJson_noScores() throws IOException {
        String json = "{"
                + "\"user\": []"
                + "}";

        JsonParser parser = createJsonParser(json);
        parser.nextToken();
        List<Influence> infs = new InfluenceParser(parser).parseJson();

        assertEquals(1, infs.size());
        Influence inf = infs.get(0);

        assertEquals("user", inf.getInfluencerFieldName());
        assertEquals(0, inf.getInfluencerFieldValues().size());
    }

    public void testParseScores_InvalidJson() throws IOException {
        // invalid json
        String json = "{"
                + "\"user\": {}"
                + "}";

        JsonParser parser = createJsonParser(json);
        parser.nextToken();
        ESTestCase.expectThrows(ElasticsearchParseException.class, () -> new InfluenceParser(parser).parseJson());
    }

    public void testParseJson_withTwoInfluencers() throws IOException {
        String json = "{"
                + "\"host\": [\"web-server\", \"localhost\"],"
                + "\"user\": [\"cat\", \"dave\", \"jo\"]"
                + "}";

        JsonParser parser = createJsonParser(json);
        parser.nextToken();
        List<Influence> infs = new InfluenceParser(parser).parseJson();

        assertEquals(2, infs.size());
        Collections.sort(infs, new InfluenceFieldComparator());

        Influence host = infs.get(0);
        assertEquals("host", host.getInfluencerFieldName());
        assertEquals(2, host.getInfluencerFieldValues().size());
        assertEquals("web-server", host.getInfluencerFieldValues().get(0));
        assertEquals("localhost", host.getInfluencerFieldValues().get(1));

        Influence user = infs.get(1);
        assertEquals("user", user.getInfluencerFieldName());
        assertEquals(3, user.getInfluencerFieldValues().size());
        assertEquals("cat", user.getInfluencerFieldValues().get(0));
        assertEquals("dave", user.getInfluencerFieldValues().get(1));
        assertEquals("jo", user.getInfluencerFieldValues().get(2));
    }


    private static JsonParser createJsonParser(String input) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        return new JsonFactory().createParser(inputStream);
    }
}
