
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
        case "timestamp":
            modelDebugOutput.setTimestamp(new Date(parseAsLongOrZero(fieldName)));
            break;
        case "partitionFieldName":
            modelDebugOutput.setPartitionFieldName(parseAsStringOrNull(fieldName));
            break;
        case "partitionFieldValue":
            modelDebugOutput.setPartitionFieldValue(parseAsStringOrNull(fieldName));
            break;
        case "overFieldName":
            modelDebugOutput.setOverFieldName(parseAsStringOrNull(fieldName));
            break;
        case "overFieldValue":
            modelDebugOutput.setOverFieldValue(parseAsStringOrNull(fieldName));
            break;
        case "byFieldName":
            modelDebugOutput.setByFieldName(parseAsStringOrNull(fieldName));
            break;
        case "byFieldValue":
            modelDebugOutput.setByFieldValue(parseAsStringOrNull(fieldName));
            break;
        case "debugFeature":
            modelDebugOutput.setDebugFeature(parseAsStringOrNull(fieldName));
            break;
        case "debugLower":
            modelDebugOutput.setDebugLower(parseAsDoubleOrZero(fieldName));
            break;
        case "debugUpper":
            modelDebugOutput.setDebugUpper(parseAsDoubleOrZero(fieldName));
            break;
        case "debugMedian":
            modelDebugOutput.setDebugMedian(parseAsDoubleOrZero(fieldName));
            break;
        case "actual":
            modelDebugOutput.setActual(parseAsDoubleOrZero(fieldName));
            break;
        default:
            LOGGER.warn(String.format("Parse error unknown field in ModelDebugOutput %s:%s",
                    fieldName, token.asString()));
            break;
        }
    }
}
