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
import org.elasticsearch.xpack.prelert.job.process.autodetect.output.FlushAcknowledgement;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.xpack.prelert.utils.json.FieldNameParser;

import java.io.IOException;
import java.util.Locale;

public final class FlushAcknowledgementParser extends FieldNameParser<FlushAcknowledgement> {
    private static final Logger LOGGER = Loggers.getLogger(FlushAcknowledgementParser.class);

    public FlushAcknowledgementParser(JsonParser jsonParser) {
        super("FlushAcknowledgement", jsonParser, LOGGER);
    }

    @Override
    protected FlushAcknowledgement supply() {
        return new FlushAcknowledgement();
    }

    @Override
    protected void handleFieldName(String fieldName, FlushAcknowledgement ack) throws IOException {
        JsonToken token = parser.nextToken();
        if (FlushAcknowledgement.TYPE.getPreferredName().equals(fieldName)) {
            ack.setId(parseAsStringOrNull(fieldName));
        } else {
            LOGGER.warn(String.format(Locale.ROOT, "Parse error unknown field in FlushAcknowledgement %s:%s",
                    fieldName, token.asString()));
        }
    }
}

