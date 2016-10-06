
package org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.xpack.prelert.job.results.AnomalyCause;
import org.elasticsearch.xpack.prelert.job.results.AnomalyRecord;
import org.elasticsearch.xpack.prelert.utils.json.FieldNameParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class AnomalyRecordParser extends FieldNameParser<AnomalyRecord> {
    private static final Logger LOGGER = Loggers.getLogger(AnomalyRecordParser.class);

    public AnomalyRecordParser(JsonParser jsonParser) {
        super("Anomaly Record", jsonParser, LOGGER);
    }

    @Override
    protected AnomalyRecord supply() {
        return new AnomalyRecord();
    }

    @Override
    protected void handleFieldName(String fieldName, AnomalyRecord record) throws IOException {
        JsonToken token = parser.nextToken();
        switch (fieldName) {
            case AnomalyRecord.DETECTOR_INDEX:
                record.setDetectorIndex(parseAsIntOrZero(fieldName));
                break;
            case AnomalyRecord.PROBABILITY:
                record.setProbability(parseAsDoubleOrZero(fieldName));
                break;
            case AnomalyRecord.ANOMALY_SCORE:
                record.setAnomalyScore(parseAsDoubleOrZero(fieldName));
                break;
            case AnomalyRecord.NORMALIZED_PROBABILITY:
                record.setNormalizedProbability(parseAsDoubleOrZero(fieldName));
                record.setInitialNormalizedProbability(record.getNormalizedProbability());
                break;
            case AnomalyRecord.BY_FIELD_NAME:
                record.setByFieldName(parseAsStringOrNull(fieldName));
                break;
            case AnomalyRecord.BY_FIELD_VALUE:
                record.setByFieldValue(parseAsStringOrNull(fieldName));
                break;
            case AnomalyRecord.CORRELATED_BY_FIELD_VALUE:
                record.setCorrelatedByFieldValue(parseAsStringOrNull(fieldName));
                break;
            case AnomalyRecord.PARTITION_FIELD_NAME:
                record.setPartitionFieldName(parseAsStringOrNull(fieldName));
                break;
            case AnomalyRecord.PARTITION_FIELD_VALUE:
                record.setPartitionFieldValue(parseAsStringOrNull(fieldName));
                break;
            case AnomalyRecord.FUNCTION:
                record.setFunction(parseAsStringOrNull(fieldName));
                break;
            case AnomalyRecord.FUNCTION_DESCRIPTION:
                record.setFunctionDescription(parseAsStringOrNull(fieldName));
                break;
            case AnomalyRecord.TYPICAL:
                record.setTypical(parsePrimitiveDoubleArray(fieldName));
                break;
            case AnomalyRecord.ACTUAL:
                record.setActual(parsePrimitiveDoubleArray(fieldName));
                break;
            case AnomalyRecord.FIELD_NAME:
                record.setFieldName(parseAsStringOrNull(fieldName));
                break;
            case AnomalyRecord.OVER_FIELD_NAME:
                record.setOverFieldName(parseAsStringOrNull(fieldName));
                break;
            case AnomalyRecord.OVER_FIELD_VALUE:
                record.setOverFieldValue(parseAsStringOrNull(fieldName));
                break;
            case AnomalyRecord.IS_INTERIM:
                record.setInterim(parseAsBooleanOrNull(fieldName));
                break;
            case AnomalyRecord.INFLUENCERS:
                record.setInfluencers(new InfluenceParser(parser).parseJson());
                break;
            case AnomalyRecord.CAUSES:
                record.setCauses(parseCauses(fieldName));
                break;
            case AnomalyRecord.BUCKET_SPAN:
                record.setBucketSpan(parseAsLongOrZero(fieldName));
                break;
            default:
                LOGGER.warn(String.format("Parse error unknown field in Anomaly Record %s:%s",
                        fieldName, token.asString()));
                break;
        }
    }

    private List<AnomalyCause> parseCauses(String fieldName) throws IOException {
        List<AnomalyCause> causes = new ArrayList<>();
        parseArray(fieldName, () -> new AnomalyCauseParser(parser).parseJson(), causes);
        return causes;
    }
}
