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
import org.elasticsearch.xpack.prelert.job.results.Bucket;
import org.elasticsearch.xpack.prelert.job.results.PartitionScore;
import org.elasticsearch.xpack.prelert.utils.json.FieldNameParser;

import java.io.IOException;
import java.util.Locale;

public class PartitionScoreParser extends FieldNameParser<PartitionScore>
{
    private static final Logger LOGGER = Loggers.getLogger(PartitionScoreParser.class);

    public PartitionScoreParser(JsonParser jsonParser)
    {
        super(Bucket.PARTITION_SCORES.getPreferredName(), jsonParser, LOGGER);
    }

    @Override
    protected PartitionScore supply()
    {
        return new PartitionScore();
    }

    @Override
    protected void handleFieldName(String fieldName, PartitionScore score) throws IOException
    {
        JsonToken token = parser.nextToken();
        switch (fieldName)
        {
        case "probability":
            score.setProbability(parseAsDoubleOrZero( fieldName));
            break;
        case "normalizedProbability":
            score.setAnomalyScore(parseAsDoubleOrZero(fieldName));
            break;
        case "partitionFieldName":
            score.setPartitionFieldName(parseAsStringOrNull(fieldName));
            break;
        case "partitionFieldValue":
            score.setPartitionFieldValue(parseAsStringOrNull(fieldName));
            break;
        default:
            LOGGER.warn(String.format(Locale.ROOT, "Parse error unknown field in PartitionScore %s:%s",
                    fieldName, token.asString()));
            break;
        }
    }
}
