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
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.xpack.prelert.job.results.Influence;
import org.elasticsearch.xpack.prelert.utils.json.FieldNameParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


final class InfluenceParser extends FieldNameParser<List<Influence>> {
    private static final Logger LOGGER = Loggers.getLogger(InfluenceParser.class);

    public InfluenceParser(JsonParser jsonParser) {
        super("Influences", jsonParser, LOGGER);
    }

    @Override
    protected List<Influence> supply() {
        return new ArrayList<>();
    }

    @Override
    protected void handleFieldName(String fieldName, List<Influence> influences) throws IOException {
        parser.nextToken();
        Influence influence = new Influence(fieldName, parseValues(fieldName));
        influences.add(influence);
    }

    private List<String> parseValues(String fieldName) throws IOException {
        List<String> influenceValues = new ArrayList<>();
        parseArray(fieldName, () -> parseAsStringOrNull(fieldName), influenceValues);
        return influenceValues;
    }
}

