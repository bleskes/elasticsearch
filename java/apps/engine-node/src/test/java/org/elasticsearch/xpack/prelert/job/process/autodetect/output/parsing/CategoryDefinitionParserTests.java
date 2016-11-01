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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.results.CategoryDefinition;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class CategoryDefinitionParserTests extends ESTestCase {
    public void testParseJson_GivenCategoryDefinitionWithExamplesThatIsNotAnArrayObject()
            throws IOException {
        String input = "{\"categoryDefinition\": 1, \"examples\": \"bar\"}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        ESTestCase.expectThrows(ElasticsearchParseException.class, () -> new CategoryDefinitionParser(parser).parseJson());
    }

    public void testParseJson_GivenCategoryDefinitionWithAllFieldsPopulatedAndValid()
            throws IOException {
        String input = "{\"categoryDefinition\": 1,"
                + " \"terms\":\"foo bar\","
                + " \"regex\":\".*?foo.*?bar.*\","
                + " \"maxMatchingLength\":350,"
                + " \"examples\": [\"foo\", \"bar\"]}";
        JsonParser parser = createJsonParser(input);
        parser.nextToken();

        CategoryDefinition category = new CategoryDefinitionParser(parser).parseJson();

        assertEquals(1, category.getCategoryId());
        assertEquals("foo bar", category.getTerms());
        assertEquals(".*?foo.*?bar.*", category.getRegex());
        assertEquals(350L, category.getMaxMatchingLength());
        assertEquals(Arrays.asList("bar", "foo"), category.getExamples());

        assertEquals(JsonToken.END_OBJECT, parser.getCurrentToken());
    }

    private static JsonParser createJsonParser(String input) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        return new JsonFactory().createParser(inputStream);
    }
}
