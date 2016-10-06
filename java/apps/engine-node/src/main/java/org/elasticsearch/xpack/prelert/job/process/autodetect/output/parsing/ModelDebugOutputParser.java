
package org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.elasticsearch.xpack.prelert.job.results.ModelDebugOutput;
import org.elasticsearch.xpack.prelert.utils.json.FieldNameParser;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;

import java.io.IOException;
import java.util.Date;

final class ModelDebugOutputParser extends FieldNameParser<ModelDebugOutput> {
    private static final Logger LOGGER = Loggers.getLogger(ModelDebugOutputParser.class);

    public ModelDebugOutputParser(JsonParser jsonParser) {
        super("ModelDebugOutput", jsonParser, LOGGER);
    }

    @Override
    protected ModelDebugOutput supply() {
        return new ModelDebugOutput();
    }

    @Override
    protected void handleFieldName(String fieldName, ModelDebugOutput modelDebugOutput)
            throws IOException {
        JsonToken token = parser.nextToken();
        switch (fieldName) {
            case ModelDebugOutput.TIMESTAMP:
                modelDebugOutput.setTimestamp(new Date(parseAsLongOrZero(fieldName)));
                break;
            case ModelDebugOutput.PARTITION_FIELD_NAME:
                modelDebugOutput.setPartitionFieldName(parseAsStringOrNull(fieldName));
                break;
            case ModelDebugOutput.PARTITION_FIELD_VALUE:
                modelDebugOutput.setPartitionFieldValue(parseAsStringOrNull(fieldName));
                break;
            case ModelDebugOutput.OVER_FIELD_NAME:
                modelDebugOutput.setOverFieldName(parseAsStringOrNull(fieldName));
                break;
            case ModelDebugOutput.OVER_FIELD_VALUE:
                modelDebugOutput.setOverFieldValue(parseAsStringOrNull(fieldName));
                break;
            case ModelDebugOutput.BY_FIELD_NAME:
                modelDebugOutput.setByFieldName(parseAsStringOrNull(fieldName));
                break;
            case ModelDebugOutput.BY_FIELD_VALUE:
                modelDebugOutput.setByFieldValue(parseAsStringOrNull(fieldName));
                break;
            case ModelDebugOutput.DEBUG_FEATURE:
                modelDebugOutput.setDebugFeature(parseAsStringOrNull(fieldName));
                break;
            case ModelDebugOutput.DEBUG_LOWER:
                modelDebugOutput.setDebugLower(parseAsDoubleOrZero(fieldName));
                break;
            case ModelDebugOutput.DEBUG_UPPER:
                modelDebugOutput.setDebugUpper(parseAsDoubleOrZero(fieldName));
                break;
            case ModelDebugOutput.DEBUG_MEDIAN:
                modelDebugOutput.setDebugMedian(parseAsDoubleOrZero(fieldName));
                break;
            case ModelDebugOutput.ACTUAL:
                modelDebugOutput.setActual(parseAsDoubleOrZero(fieldName));
                break;
            default:
                LOGGER.warn(String.format("Parse error unknown field in ModelDebugOutput %s:%s",
                        fieldName, token.asString()));
                break;
        }
    }
}
