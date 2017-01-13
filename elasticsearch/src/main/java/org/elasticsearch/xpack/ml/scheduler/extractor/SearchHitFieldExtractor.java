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

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class SearchHitFieldExtractor {

    private SearchHitFieldExtractor() {}

    public static Object[] extractField(SearchHit hit, String field) {
        SearchHitField keyValue = hit.field(field);
        if (keyValue != null) {
            List<Object> values = keyValue.values();
            return values.toArray(new Object[values.size()]);
        } else {
            return extractFieldFromSource(hit.getSource(), field);
        }
    }

    private static Object[] extractFieldFromSource(Map<String, Object> source, String field) {
        if (source != null) {
            Object values = source.get(field);
            if (values != null) {
                if (values instanceof Object[]) {
                    return (Object[]) values;
                } else {
                    return new Object[]{values};
                }
            }
        }
        return new Object[0];
    }

    public static Long extractTimeField(SearchHit hit, String timeField) {
        Object[] fields = extractField(hit, timeField);
        if (fields.length != 1) {
            throw new RuntimeException("Time field [" + timeField + "] expected a single value; actual was: " + Arrays.toString(fields));
        }
        if (fields[0] instanceof Long) {
            return (Long) fields[0];
        }
        throw new RuntimeException("Time field [" + timeField + "] expected a long value; actual was: " + fields[0]);
    }
}
