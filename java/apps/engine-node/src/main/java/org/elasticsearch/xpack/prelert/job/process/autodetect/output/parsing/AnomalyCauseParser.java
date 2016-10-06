
package org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.xpack.prelert.job.results.AnomalyCause;
import org.elasticsearch.xpack.prelert.utils.json.FieldNameParser;

import java.io.IOException;

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
            case AnomalyCause.PROBABILITY:
                cause.setProbability(parseAsDoubleOrZero(fieldName));
                break;
            case AnomalyCause.BY_FIELD_NAME:
                cause.setByFieldName(parseAsStringOrNull(fieldName));
                break;
            case AnomalyCause.BY_FIELD_VALUE:
                cause.setByFieldValue(parseAsStringOrNull(fieldName));
                break;
            case AnomalyCause.CORRELATED_BY_FIELD_VALUE:
                cause.setCorrelatedByFieldValue(parseAsStringOrNull(fieldName));
                break;
            case AnomalyCause.PARTITION_FIELD_NAME:
                cause.setPartitionFieldName(parseAsStringOrNull(fieldName));
                break;
            case AnomalyCause.PARTITION_FIELD_VALUE:
                cause.setPartitionFieldValue(parseAsStringOrNull(fieldName));
                break;
            case AnomalyCause.FUNCTION:
                cause.setFunction(parseAsStringOrNull(fieldName));
                break;
            case AnomalyCause.FUNCTION_DESCRIPTION:
                cause.setFunctionDescription(parseAsStringOrNull(fieldName));
                break;
            case AnomalyCause.TYPICAL:
                cause.setTypical(parsePrimitiveDoubleArray(fieldName));
                break;
            case AnomalyCause.ACTUAL:
                cause.setActual(parsePrimitiveDoubleArray(fieldName));
                break;
            case AnomalyCause.FIELD_NAME:
                cause.setFieldName(parseAsStringOrNull(fieldName));
                break;
            case AnomalyCause.OVER_FIELD_NAME:
                cause.setOverFieldName(parseAsStringOrNull(fieldName));
                break;
            case AnomalyCause.OVER_FIELD_VALUE:
                cause.setOverFieldValue(parseAsStringOrNull(fieldName));
                break;
            case AnomalyCause.INFLUENCERS:
                cause.setInfluencers(new InfluenceParser(parser).parseJson());
                break;
            default:
                LOGGER.warn(String.format("Parse error unknown field in Anomaly Cause %s:%s",
                        fieldName, token.asString()));
                break;
        }
    }
}
