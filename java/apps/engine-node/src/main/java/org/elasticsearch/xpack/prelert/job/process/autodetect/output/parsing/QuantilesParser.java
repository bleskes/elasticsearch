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
import org.elasticsearch.xpack.prelert.job.quantiles.Quantiles;
import org.elasticsearch.xpack.prelert.utils.json.FieldNameParser;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;

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
        case "timestamp":
            long seconds = parseAsLongOrZero(fieldName);
            // convert seconds to ms
            quantiles.setTimestamp(new Date(seconds * 1000));
            break;
        case "quantileState":
            quantiles.setQuantileState(parseAsStringOrNull(fieldName));
            break;
        default:
            LOGGER.warn(String.format(Locale.ROOT, "Parse error unknown field in Quantiles %s:%s",
                    fieldName, token.asString()));
            break;
        }
    }
}
