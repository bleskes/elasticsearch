
package org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.xpack.prelert.job.results.AnomalyRecord;
import org.elasticsearch.xpack.prelert.job.results.Bucket;
import org.elasticsearch.xpack.prelert.job.results.PartitionScore;
import org.elasticsearch.xpack.prelert.utils.json.FieldNameParser;

import java.io.IOException;

public class PartitionScoreParser extends FieldNameParser<PartitionScore>
{
    private static final Logger LOGGER = Loggers.getLogger(PartitionScoreParser.class);

    public PartitionScoreParser(JsonParser jsonParser)
    {
        super(Bucket.PARTITION_SCORES, jsonParser, LOGGER);
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
        case AnomalyRecord.PROBABILITY:
            score.setProbability(parseAsDoubleOrZero( fieldName));
            break;
        case AnomalyRecord.NORMALIZED_PROBABILITY:
            score.setAnomalyScore(parseAsDoubleOrZero(fieldName));
            break;
        case AnomalyRecord.PARTITION_FIELD_NAME:
            score.setPartitionFieldName(parseAsStringOrNull(fieldName));
            break;
        case AnomalyRecord.PARTITION_FIELD_VALUE:
            score.setPartitionFieldValue(parseAsStringOrNull(fieldName));
            break;
        default:
            LOGGER.warn(String.format("Parse error unknown field in PartitionScore %s:%s",
                    fieldName, token.asString()));
            break;
        }
    }
}
