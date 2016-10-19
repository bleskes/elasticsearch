
package org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.xpack.prelert.job.results.AnomalyCause;
import org.elasticsearch.xpack.prelert.utils.json.FieldNameParser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class AnomalyCauseParser extends FieldNameParser<AnomalyCause> {
    private static final Logger LOGGER = Loggers.getLogger(AnomalyCauseParser.class);

    public AnomalyCauseParser(JsonParser jsonParser) {
        super("AnomalyCause", jsonParser, LOGGER);
    }

    @Override
    protected AnomalyCause supply() {
        return new AnomalyCause();
    }

    @Override
    protected void handleFieldName(String fieldName, AnomalyCause cause) throws IOException {
        JsonToken token = parser.nextToken();
        switch (fieldName) {
            case "probability":
                cause.setProbability(parseAsDoubleOrZero(fieldName));
                break;
            case "byFieldName":
                cause.setByFieldName(parseAsStringOrNull(fieldName));
                break;
            case "byFieldValue":
                cause.setByFieldValue(parseAsStringOrNull(fieldName));
                break;
            case "correlatedByFieldValue":
                cause.setCorrelatedByFieldValue(parseAsStringOrNull(fieldName));
                break;
            case "partitionFieldName":
                cause.setPartitionFieldName(parseAsStringOrNull(fieldName));
                break;
            case "partitionFieldValue":
                cause.setPartitionFieldValue(parseAsStringOrNull(fieldName));
                break;
            case "function":
                cause.setFunction(parseAsStringOrNull(fieldName));
                break;
            case "functionDescription":
                cause.setFunctionDescription(parseAsStringOrNull(fieldName));
                break;
            case "typical":
                double[] typicalArray = parsePrimitiveDoubleArray(fieldName);
                List<Double> typicalList = new ArrayList<>(typicalArray.length);
                for (double value : typicalArray) {
                    typicalList.add(value);
                }
                cause.setTypical(typicalList);
                break;
            case "actual":
                double[] actualArray = parsePrimitiveDoubleArray(fieldName);
                List<Double> actualList = new ArrayList<>(actualArray.length);
                for (double value : actualArray) {
                    actualList.add(value);
                }
                cause.setActual(actualList);
                break;
            case "fieldName":
                cause.setFieldName(parseAsStringOrNull(fieldName));
                break;
            case "overFieldName":
                cause.setOverFieldName(parseAsStringOrNull(fieldName));
                break;
            case "overFieldValue":
                cause.setOverFieldValue(parseAsStringOrNull(fieldName));
                break;
            case "influencers":
                cause.setInfluencers(new InfluenceParser(parser).parseJson());
                break;
            default:
                LOGGER.warn(String.format("Parse error unknown field in Anomaly Cause %s:%s",
                        fieldName, token.asString()));
                break;
        }
    }
}
