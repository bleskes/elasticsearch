/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
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
package org.elasticsearch.xpack.ml.scheduler.extractor;

import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.internal.InternalSearchHit;
import org.elasticsearch.search.internal.InternalSearchHitField;
import org.elasticsearch.test.ESTestCase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;

public class SearchHitToJsonProcessorTests extends ESTestCase {

    public void testProcessGivenHitContainsNothing() throws IOException {
        InternalSearchHit searchHit = new InternalSearchHit(42);

        String json = searchHitToString(new String[] {"field_1", "field_2"}, searchHit);

        assertThat(json, equalTo("{}"));
    }

    public void testProcessGivenHitContainsEmptySource() throws IOException {
        InternalSearchHit searchHit = new InternalSearchHit(42);
        searchHit.sourceRef(new BytesArray("{}"));

        String json = searchHitToString(new String[] {"field_1", "field_2"}, searchHit);

        assertThat(json, equalTo("{}"));
    }

    public void testProcessGivenHitContainsSingleValueInFields() throws IOException {
        InternalSearchHit searchHit = new InternalSearchHit(42);
        Map<String, SearchHitField> fields = new HashMap<>();
        fields.put("field_1", new InternalSearchHitField("field_1", Arrays.asList(3)));
        searchHit.fields(fields);

        String json = searchHitToString(new String[] {"field_1"}, searchHit);

        assertThat(json, equalTo("{\"field_1\":3}"));
    }

    public void testProcessGivenHitContainsArrayValueInFields() throws IOException {
        InternalSearchHit searchHit = new InternalSearchHit(42);
        Map<String, SearchHitField> fields = new HashMap<>();
        fields.put("field_1", new InternalSearchHitField("field_1", Arrays.asList(3, 9)));
        searchHit.fields(fields);

        String json = searchHitToString(new String[] {"field_1"}, searchHit);

        assertThat(json, equalTo("{\"field_1\":[3,9]}"));
    }

    public void testProcessGivenHitContainsSingleValueInSource() throws IOException {
        InternalSearchHit searchHit = new InternalSearchHit(42);
        String hitSource = "{\"field_1\":\"foo\"}";
        searchHit.sourceRef(new BytesArray(hitSource));

        String json = searchHitToString(new String[] {"field_1"}, searchHit);

        assertThat(json, equalTo("{\"field_1\":\"foo\"}"));
    }

    public void testProcessGivenHitContainsArrayValueInSource() throws IOException {
        InternalSearchHit searchHit = new InternalSearchHit(42);
        String hitSource = "{\"field_1\":[\"foo\",\"bar\"]}";
        searchHit.sourceRef(new BytesArray(hitSource));

        String json = searchHitToString(new String[] {"field_1"}, searchHit);

        assertThat(json, equalTo("{\"field_1\":[\"foo\",\"bar\"]}"));
    }

    public void testProcessGivenHitContainsFieldsAndSource() throws IOException {
        InternalSearchHit searchHit = new InternalSearchHit(42);
        String hitSource = "{\"field_1\":\"foo\"}";
        searchHit.sourceRef(new BytesArray(hitSource));
        Map<String, SearchHitField> fields = new HashMap<>();
        fields.put("field_2", new InternalSearchHitField("field_2", Arrays.asList("bar")));
        searchHit.fields(fields);

        String json = searchHitToString(new String[] {"field_1", "field_2"}, searchHit);

        assertThat(json, equalTo("{\"field_1\":\"foo\",\"field_2\":\"bar\"}"));
    }

    public void testProcessGivenMultipleHits() throws IOException {
        InternalSearchHit searchHit1 = new InternalSearchHit(42);
        Map<String, SearchHitField> fields = new HashMap<>();
        fields.put("field_1", new InternalSearchHitField("field_1", Arrays.asList(3)));
        searchHit1.fields(fields);
        InternalSearchHit searchHit2 = new InternalSearchHit(42);
        fields = new HashMap<>();
        fields.put("field_1", new InternalSearchHitField("field_1", Arrays.asList(5)));
        searchHit2.fields(fields);

        String json = searchHitToString(new String[] {"field_1"}, searchHit1, searchHit2);

        assertThat(json, equalTo("{\"field_1\":3} {\"field_1\":5}"));
    }

    private String searchHitToString(String[] fields, SearchHit... searchHits) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (SearchHitToJsonProcessor hitProcessor = new SearchHitToJsonProcessor(fields, outputStream)) {
            for (int i = 0; i < searchHits.length; i++) {
                hitProcessor.process(searchHits[i]);
            }
        }
        return outputStream.toString(StandardCharsets.UTF_8.name());
    }
}
