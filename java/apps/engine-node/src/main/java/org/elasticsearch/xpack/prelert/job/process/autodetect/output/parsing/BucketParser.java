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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.xpack.prelert.job.results.AnomalyRecord;
import org.elasticsearch.xpack.prelert.job.results.Bucket;
import org.elasticsearch.xpack.prelert.job.results.BucketInfluencer;
import org.elasticsearch.xpack.prelert.job.results.Influencer;
import org.elasticsearch.xpack.prelert.job.results.PartitionScore;
import org.elasticsearch.xpack.prelert.utils.json.FieldNameParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class BucketParser extends FieldNameParser<Bucket> {
    private static final Logger LOGGER = Loggers.getLogger(BucketParser.class);

    public BucketParser(JsonParser jsonParser) {
        super("Bucket", jsonParser, LOGGER);
    }

    @Override
    protected Bucket supply() {
        return new Bucket();
    }

    @Override
    protected void handleFieldName(String fieldName, Bucket bucket) throws IOException {
        JsonToken token = parser.nextToken();
        switch (fieldName) {
        case "timestamp":
            parseTimestamp(bucket, token);
            break;
        case "anomalyScore":
            bucket.setAnomalyScore(parseAsDoubleOrZero(fieldName));
            bucket.setInitialAnomalyScore(bucket.getAnomalyScore());
            break;
        case "maxNormalizedProbability":
            bucket.setMaxNormalizedProbability(parseAsDoubleOrZero(fieldName));
            break;
        case "recordCount":
            bucket.setRecordCount(parseAsIntOrZero(fieldName));
            break;
        case "eventCount":
            bucket.setEventCount(parseAsLongOrZero(fieldName));
            break;
        case "isInterim":
            bucket.setInterim(parseAsBooleanOrNull(fieldName));
            break;
        case "records":
            bucket.setRecords(parseRecords(fieldName));
            break;
        case "bucketInfluencers":
            bucket.setBucketInfluencers(parseBucketInfluencers(fieldName));
            break;
        case "influencers":
            bucket.setInfluencers(parseInfluencers(fieldName));
            break;
        case "bucketSpan":
            bucket.setBucketSpan(parseAsLongOrZero(fieldName));
            break;
        case "processingTimeMs":
            bucket.setProcessingTimeMs(parseAsLongOrZero(fieldName));
            break;
        case "partitionScores":
            bucket.setPartitionScores(parsePartitionScores(fieldName));
            break;
        default:
            LOGGER.warn(String.format(Locale.ROOT, "Parse error: unknown field in Bucket %s:%s",
                    fieldName, token.asString()));
            break;
        }
    }

    private void parseTimestamp(Bucket bucket, JsonToken token) throws IOException {
        if (token == JsonToken.VALUE_NUMBER_INT) {
            // convert seconds to ms
            long val = parser.getLongValue() * 1000;
            bucket.setTimestamp(new Date(val));
        } else {
            LOGGER.warn("Cannot parse " + Bucket.TIMESTAMP + " : " + parser.getText()
            + " as a long");
        }
    }

    private List<AnomalyRecord> parseRecords(String fieldName) throws IOException {
        List<AnomalyRecord> anomalyRecords = new ArrayList<>();
        parseArray(fieldName, () -> new AnomalyRecordParser(parser).parseJson(), anomalyRecords);
        return anomalyRecords;
    }

    private List<BucketInfluencer> parseBucketInfluencers(String fieldName) throws IOException {
        List<BucketInfluencer> influencers = new ArrayList<>();
        parseArray(fieldName, () -> new BucketInfluencerParser(parser).parseJson(), influencers);
        return influencers;
    }

    private List<Influencer> parseInfluencers(String fieldName) throws IOException {
        List<Influencer> influencers = new ArrayList<>();
        parseArray(fieldName, () -> new InfluencerParser(parser).parseJson(), influencers);
        return influencers;
    }

    private List<PartitionScore> parsePartitionScores(String fieldName)
            throws IOException {
        List<PartitionScore> scores = new ArrayList<>();
        parseArray(fieldName, () -> new PartitionScoreParser(parser).parseJson(), scores);
        return scores;
    }


}
