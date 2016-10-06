
package org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.xpack.prelert.job.results.BucketInfluencer;
import org.elasticsearch.xpack.prelert.utils.json.FieldNameParser;

import java.io.IOException;

final class BucketInfluencerParser extends FieldNameParser<BucketInfluencer> {
    private static final Logger LOGGER = Loggers.getLogger(BucketInfluencerParser.class);

    public BucketInfluencerParser(JsonParser jsonParser) {
        super("BucketInfluencer", jsonParser, LOGGER);
    }

    @Override
    protected BucketInfluencer supply() {
        return new BucketInfluencer();
    }

    @Override
    protected void handleFieldName(String fieldName, BucketInfluencer influencer) throws IOException {
        JsonToken token = parser.nextToken();
        switch (fieldName) {
            case BucketInfluencer.PROBABILITY:
                influencer.setProbability(parseAsDoubleOrZero(fieldName));
                break;
            case BucketInfluencer.INITIAL_ANOMALY_SCORE:
                influencer.setInitialAnomalyScore(parseAsDoubleOrZero(fieldName));
                influencer.setAnomalyScore(influencer.getInitialAnomalyScore());
                break;
            case BucketInfluencer.RAW_ANOMALY_SCORE:
                influencer.setRawAnomalyScore(parseAsDoubleOrZero(fieldName));
                break;
            case BucketInfluencer.INFLUENCER_FIELD_NAME:
                influencer.setInfluencerFieldName(parseAsStringOrNull(fieldName));
                break;
            default:
                LOGGER.warn(String.format("Parse error unknown field in BucketInfluencer %s:%s",
                        fieldName, token.asString()));
                break;
        }
    }
}

