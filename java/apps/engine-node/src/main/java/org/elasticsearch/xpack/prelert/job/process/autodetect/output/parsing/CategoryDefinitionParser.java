
package org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.xpack.prelert.job.results.CategoryDefinition;
import org.elasticsearch.xpack.prelert.utils.json.FieldNameParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class CategoryDefinitionParser extends FieldNameParser<CategoryDefinition> {
    private static final Logger LOGGER = Loggers.getLogger(CategoryDefinitionParser.class);

    public CategoryDefinitionParser(JsonParser jsonParser) {
        super("CategoryDefinition", jsonParser, LOGGER);
    }

    @Override
    protected CategoryDefinition supply() {
        return new CategoryDefinition();
    }

    @Override
    protected void handleFieldName(String fieldName, CategoryDefinition category) throws IOException {
        JsonToken token = parser.nextToken();
        switch (fieldName) {
            case CategoryDefinition.TYPE:
                category.setCategoryId(parseAsLongOrZero(fieldName));
                break;
            case CategoryDefinition.TERMS:
                category.setTerms(parseAsStringOrNull(fieldName));
                break;
            case CategoryDefinition.REGEX:
                category.setRegex(parseAsStringOrNull(fieldName));
                break;
            case CategoryDefinition.MAX_MATCHING_LENGTH:
                category.setMaxMatchingLength(parseAsLongOrZero(fieldName));
                break;
            case CategoryDefinition.EXAMPLES:
                category.setExamples(parseExamples(fieldName));
                break;
            default:
                LOGGER.warn(String.format("Parse error unknown field in CategoryDefinition %s:%s",
                        fieldName, token.asString()));
                break;
        }
    }

    private List<String> parseExamples(String fieldName) throws IOException {
        List<String> examples = new ArrayList<>();
        parseArray(fieldName, () -> parseAsStringOrNull(fieldName), examples);
        return examples;
    }
}
