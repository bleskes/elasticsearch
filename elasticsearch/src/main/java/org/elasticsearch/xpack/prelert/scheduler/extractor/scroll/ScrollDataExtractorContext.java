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
package org.elasticsearch.xpack.prelert.scheduler.extractor.scroll;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregatorFactories;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.util.List;
import java.util.Objects;

public class ScrollDataExtractorContext {

    final String jobId;
    final String[] jobFields;
    final String timeField;
    final String[] indexes;
    final String[] types;
    final QueryBuilder query;
    @Nullable
    final AggregatorFactories.Builder aggregations;
    final List<SearchSourceBuilder.ScriptField> scriptFields;
    final int scrollSize;
    final long start;
    final long end;

    public ScrollDataExtractorContext(String jobId, List<String> jobFields, String timeField, List<String> indexes, List<String> types,
                                      QueryBuilder query, @Nullable AggregatorFactories.Builder aggregations,
                                      List<SearchSourceBuilder.ScriptField> scriptFields, int scrollSize, long start, long end) {
        this.jobId = Objects.requireNonNull(jobId);
        this.jobFields = jobFields.toArray(new String[jobFields.size()]);
        this.timeField = Objects.requireNonNull(timeField);
        this.indexes = indexes.toArray(new String[indexes.size()]);
        this.types = types.toArray(new String[types.size()]);
        this.query = Objects.requireNonNull(query);
        this.aggregations = aggregations;
        this.scriptFields = Objects.requireNonNull(scriptFields);
        this.scrollSize = scrollSize;
        this.start = start;
        this.end = end;
    }
}
