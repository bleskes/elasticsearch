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
import org.elasticsearch.search.SearchHitField;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a field to be extracted by the datafeed.
 * It encapsulates the extraction logic.
 */
abstract class ExtractedField {

    public enum ExtractionMethod {
        SOURCE, DOC_VALUE, SCRIPT_FIELD
    }

    /** The name of the field as configured in the job */
    protected final String alias;

    /** The name of the field we extract */
    protected final String name;

    private final ExtractionMethod extractionMethod;

    protected ExtractedField(String alias, String name, ExtractionMethod extractionMethod) {
        this.alias = Objects.requireNonNull(alias);
        this.name = Objects.requireNonNull(name);
        this.extractionMethod = Objects.requireNonNull(extractionMethod);
    }

    public String getAlias() {
        return alias;
    }

    public String getName() {
        return name;
    }

    public ExtractionMethod getExtractionMethod() {
        return extractionMethod;
    }

    public abstract Object[] value(SearchHit hit);

    public static ExtractedField newField(String name, ExtractionMethod extractionMethod) {
        return newField(name, name, extractionMethod);
    }

    public static ExtractedField newField(String alias, String name, ExtractionMethod extractionMethod) {
        switch (extractionMethod) {
            case DOC_VALUE:
            case SCRIPT_FIELD:
                return new FromFields(alias, name, extractionMethod);
            case SOURCE:
                return new FromSource(alias, name, extractionMethod);
            default:
                throw new IllegalArgumentException("Invalid extraction method [" + extractionMethod + "]");
        }
    }

    private static class FromFields extends ExtractedField {

        FromFields(String alias, String name, ExtractionMethod extractionMethod) {
            super(alias, name, extractionMethod);
        }

        @Override
        public Object[] value(SearchHit hit) {
            SearchHitField keyValue = hit.field(name);
            if (keyValue != null) {
                List<Object> values = keyValue.values();
                return values.toArray(new Object[values.size()]);
            }
            return new Object[0];
        }
    }

    private static class FromSource extends ExtractedField {

        private String[] namePath;

        FromSource(String alias, String name, ExtractionMethod extractionMethod) {
            super(alias, name, extractionMethod);
            namePath = name.split("\\.");
        }

        @Override
        public Object[] value(SearchHit hit) {
            Map<String, Object> source = hit.getSource();
            int level = 0;
            while (source != null && level < namePath.length - 1) {
                source = getNextLevel(source, namePath[level]);
                level++;
            }
            if (source != null) {
                Object values = source.get(namePath[level]);
                if (values != null) {
                    if (values instanceof List<?>) {
                        @SuppressWarnings("unchecked")
                        List<Object> asList = (List<Object>) values;
                        return asList.toArray(new Object[asList.size()]);
                    } else {
                        return new Object[]{values};
                    }
                }
            }
            return new Object[0];
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> getNextLevel(Map<String, Object> source, String key) {
            Object nextLevel = source.get(key);
            if (nextLevel instanceof Map<?, ?>) {
                return (Map<String, Object>) source.get(key);
            }
            return null;
        }
    }
}
