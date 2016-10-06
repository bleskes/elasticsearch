
package org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.results.BucketInfluencer;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class BucketInfluencerParserTest extends ESTestCase {
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

    private static final JsonParser createJsonParser(String input) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        return new JsonFactory().createParser(inputStream);
    }

}
