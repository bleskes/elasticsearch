
package org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.xpack.prelert.job.quantiles.Quantiles;
import org.elasticsearch.xpack.prelert.utils.json.FieldNameParser;

import java.io.IOException;
import java.util.Date;

final class QuantilesParser extends FieldNameParser<Quantiles> {
    private static final Logger LOGGER = Loggers.getLogger(QuantilesParser.class);

    public QuantilesParser(JsonParser jsonParser) {
        super("Quantiles", jsonParser, LOGGER);
    }

    @Override
    protected Quantiles supply() {
        return new Quantiles();
    }

    @Override
    protected void handleFieldName(String fieldName, Quantiles quantiles) throws IOException {
        JsonToken token = parser.nextToken();
        switch (fieldName) {
            case Quantiles.TIMESTAMP:
                long seconds = parseAsLongOrZero(fieldName);
                // convert seconds to ms
                quantiles.setTimestamp(new Date(seconds * 1000));
                break;
            case Quantiles.QUANTILE_STATE:
                quantiles.setQuantileState(parseAsStringOrNull(fieldName));
                break;
            default:
                LOGGER.warn(String.format("Parse error unknown field in Quantiles %s:%s",
                        fieldName, token.asString()));
                break;
        }
    }
}
