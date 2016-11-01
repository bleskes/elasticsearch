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
import org.elasticsearch.xpack.prelert.job.results.Influencer;
import org.elasticsearch.xpack.prelert.utils.json.FieldNameParser;

import java.io.IOException;
import java.util.Locale;

final class InfluencerParser extends FieldNameParser<Influencer> {
    private static final Logger LOGGER = Loggers.getLogger(InfluencerParser.class);

    public InfluencerParser(JsonParser jsonParser) {
        super("Influencer", jsonParser, LOGGER);
    }

    @Override
    protected Influencer supply() {
        return new Influencer();
    }

    @Override
    protected void handleFieldName(String fieldName, Influencer influencer) throws IOException {
        JsonToken token = parser.nextToken();
        switch (fieldName) {
        case "probability":
            influencer.setProbability(parseAsDoubleOrZero(fieldName));
            break;
        case "initialAnomalyScore":
            influencer.setInitialAnomalyScore(parseAsDoubleOrZero(fieldName));
            influencer.setAnomalyScore(influencer.getInitialAnomalyScore());
            break;
        case "influencerFieldName":
            influencer.setInfluencerFieldName(parseAsStringOrNull(fieldName));
            break;
        case "influencerFieldValue":
            influencer.setInfluencerFieldValue(parseAsStringOrNull(fieldName));
            break;
        default:
            LOGGER.warn(String.format(Locale.ROOT, "Parse error unknown field in Influencer %s:%s",
                    fieldName, token.asString()));
            break;
        }
    }
}

