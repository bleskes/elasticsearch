/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2017 Elasticsearch BV. All Rights Reserved.
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
package org.elasticsearch.xpack.ml.datafeed.extractor.scroll;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.test.SearchHitBuilder;

import java.util.Arrays;

import static org.hamcrest.Matchers.equalTo;

public class ExtractedFieldTests extends ESTestCase {

    public void testValueGivenDocValue() {
        SearchHit hit = new SearchHitBuilder(42).addField("single", "bar").addField("array", Arrays.asList("a", "b")).build();

        ExtractedField single = ExtractedField.newField("single", ExtractedField.ExtractionMethod.DOC_VALUE);
        assertThat(single.value(hit), equalTo(new String[] { "bar" }));

        ExtractedField array = ExtractedField.newField("array", ExtractedField.ExtractionMethod.DOC_VALUE);
        assertThat(array.value(hit), equalTo(new String[] { "a", "b" }));

        ExtractedField missing = ExtractedField.newField("missing", ExtractedField.ExtractionMethod.DOC_VALUE);
        assertThat(missing.value(hit), equalTo(new Object[0]));
    }

    public void testValueGivenScriptField() {
        SearchHit hit = new SearchHitBuilder(42).addField("single", "bar").addField("array", Arrays.asList("a", "b")).build();

        ExtractedField single = ExtractedField.newField("single", ExtractedField.ExtractionMethod.SCRIPT_FIELD);
        assertThat(single.value(hit), equalTo(new String[] { "bar" }));

        ExtractedField array = ExtractedField.newField("array", ExtractedField.ExtractionMethod.SCRIPT_FIELD);
        assertThat(array.value(hit), equalTo(new String[] { "a", "b" }));

        ExtractedField missing = ExtractedField.newField("missing", ExtractedField.ExtractionMethod.SCRIPT_FIELD);
        assertThat(missing.value(hit), equalTo(new Object[0]));
    }

    public void testValueGivenSource() {
        SearchHit hit = new SearchHitBuilder(42).setSource("{\"single\":\"bar\",\"array\":[\"a\",\"b\"]}").build();

        ExtractedField single = ExtractedField.newField("single", ExtractedField.ExtractionMethod.SOURCE);
        assertThat(single.value(hit), equalTo(new String[] { "bar" }));

        ExtractedField array = ExtractedField.newField("array", ExtractedField.ExtractionMethod.SOURCE);
        assertThat(array.value(hit), equalTo(new String[] { "a", "b" }));

        ExtractedField missing = ExtractedField.newField("missing", ExtractedField.ExtractionMethod.SOURCE);
        assertThat(missing.value(hit), equalTo(new Object[0]));
    }

    public void testValueGivenNestedSource() {
        SearchHit hit = new SearchHitBuilder(42).setSource("{\"level_1\":{\"level_2\":{\"foo\":\"bar\"}}}").build();

        ExtractedField nested = ExtractedField.newField("alias", "level_1.level_2.foo", ExtractedField.ExtractionMethod.SOURCE);
        assertThat(nested.value(hit), equalTo(new String[] { "bar" }));
    }

    public void testValueGivenSourceAndHitWithNoSource() {
        ExtractedField missing = ExtractedField.newField("missing", ExtractedField.ExtractionMethod.SOURCE);
        assertThat(missing.value(new SearchHitBuilder(3).build()), equalTo(new Object[0]));
    }

    public void testValueGivenMismatchingMethod() {
        SearchHit hit = new SearchHitBuilder(42).addField("a", 1).setSource("{\"b\":2}").build();

        ExtractedField invalidA = ExtractedField.newField("a", ExtractedField.ExtractionMethod.SOURCE);
        assertThat(invalidA.value(hit), equalTo(new Object[0]));
        ExtractedField validA = ExtractedField.newField("a", ExtractedField.ExtractionMethod.DOC_VALUE);
        assertThat(validA.value(hit), equalTo(new Integer[] { 1 }));

        ExtractedField invalidB = ExtractedField.newField("b", ExtractedField.ExtractionMethod.DOC_VALUE);
        assertThat(invalidB.value(hit), equalTo(new Object[0]));
        ExtractedField validB = ExtractedField.newField("b", ExtractedField.ExtractionMethod.SOURCE);
        assertThat(validB.value(hit), equalTo(new Integer[] { 2 }));
    }

    public void testValueGivenEmptyHit() {
        SearchHit hit = new SearchHitBuilder(42).build();

        ExtractedField docValue = ExtractedField.newField("a", ExtractedField.ExtractionMethod.SOURCE);
        assertThat(docValue.value(hit), equalTo(new Object[0]));

        ExtractedField sourceField = ExtractedField.newField("b", ExtractedField.ExtractionMethod.DOC_VALUE);
        assertThat(sourceField.value(hit), equalTo(new Object[0]));
    }

    public void testAliasVersusName() {
        SearchHit hit = new SearchHitBuilder(42).addField("a", 1).addField("b", 2).build();

        ExtractedField field = ExtractedField.newField("a", "a", ExtractedField.ExtractionMethod.DOC_VALUE);
        assertThat(field.getAlias(), equalTo("a"));
        assertThat(field.getName(), equalTo("a"));
        assertThat(field.value(hit), equalTo(new Integer[] { 1 }));

        hit = new SearchHitBuilder(42).addField("a", 1).addField("b", 2).build();

        field = ExtractedField.newField("a", "b", ExtractedField.ExtractionMethod.DOC_VALUE);
        assertThat(field.getAlias(), equalTo("a"));
        assertThat(field.getName(), equalTo("b"));
        assertThat(field.value(hit), equalTo(new Integer[] { 2 }));
    }
}
