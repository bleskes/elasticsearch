
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
            case "detectorIndex":
                record.setDetectorIndex(parseAsIntOrZero(fieldName));
                break;
            case "probability":
                record.setProbability(parseAsDoubleOrZero(fieldName));
                break;
            case "anomalyScore":
                record.setAnomalyScore(parseAsDoubleOrZero(fieldName));
                break;
            case "normalizedProbability":
                record.setNormalizedProbability(parseAsDoubleOrZero(fieldName));
                record.setInitialNormalizedProbability(record.getNormalizedProbability());
                break;
            case "byFieldName":
                record.setByFieldName(parseAsStringOrNull(fieldName));
                break;
            case "byFieldValue":
                record.setByFieldValue(parseAsStringOrNull(fieldName));
                break;
            case "correlatedByFieldValue":
                record.setCorrelatedByFieldValue(parseAsStringOrNull(fieldName));
                break;
            case "partitionFieldName":
                record.setPartitionFieldName(parseAsStringOrNull(fieldName));
                break;
            case "partitionFieldValue":
                record.setPartitionFieldValue(parseAsStringOrNull(fieldName));
                break;
            case "function":
                record.setFunction(parseAsStringOrNull(fieldName));
                break;
            case "functionDescription":
                record.setFunctionDescription(parseAsStringOrNull(fieldName));
                break;
            case "typical":
                double[] typicalArray = parsePrimitiveDoubleArray(fieldName);
                List<Double> typicalList = new ArrayList<>(typicalArray.length);
                for (double value : typicalArray) {
                    typicalList.add(value);
                }
                record.setTypical(typicalList);
                break;
            case "actual":
                double[] actualArray = parsePrimitiveDoubleArray(fieldName);
                List<Double> actualList = new ArrayList<>(actualArray.length);
                for (double value : actualArray) {
                    actualList.add(value);
                }
                record.setActual(actualList);
                break;
            case "fieldName":
                record.setFieldName(parseAsStringOrNull(fieldName));
                break;
            case "overFieldName":
                record.setOverFieldName(parseAsStringOrNull(fieldName));
                break;
            case "overFieldValue":
                record.setOverFieldValue(parseAsStringOrNull(fieldName));
                break;
            case "isInterim":
                record.setInterim(parseAsBooleanOrNull(fieldName));
                break;
            case "influencers":
                record.setInfluencers(new InfluenceParser(parser).parseJson());
                break;
            case "causes":
                record.setCauses(parseCauses(fieldName));
                break;
            case "bucketSpan":
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
