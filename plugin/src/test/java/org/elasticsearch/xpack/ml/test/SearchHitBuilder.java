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
package org.elasticsearch.xpack.ml.test;

import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to build {@link SearchHit} in tests
 */
public class SearchHitBuilder {

    private final SearchHit hit;
    private final Map<String, SearchHitField> fields;

    public SearchHitBuilder(int docId) {
        hit = new SearchHit(docId);
        fields = new HashMap<>();
    }

    public SearchHitBuilder addField(String name, Object value) {
        return addField(name, Arrays.asList(value));
    }

    public SearchHitBuilder addField(String name, List<Object> values) {
        fields.put(name, new SearchHitField(name, values));
        return this;
    }

    public SearchHitBuilder setSource(String sourceJson) {
        hit.sourceRef(new BytesArray(sourceJson));
        return this;
    }

    public SearchHit build() {
        if (!fields.isEmpty()) {
            hit.fields(fields);
        }
        return hit;
    }
}
