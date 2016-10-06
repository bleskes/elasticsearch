
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
        Influence influence = new Influence();
        influence.setInfluencerFieldName(fieldName);
        parser.nextToken();
        influence.setInfluencerFieldValues(parseValues(fieldName));

        influences.add(influence);
    }

    private List<String> parseValues(String fieldName) throws IOException {
        List<String> influenceValues = new ArrayList<>();
        parseArray(fieldName, () -> parseAsStringOrNull(fieldName), influenceValues);
        return influenceValues;
    }
}

