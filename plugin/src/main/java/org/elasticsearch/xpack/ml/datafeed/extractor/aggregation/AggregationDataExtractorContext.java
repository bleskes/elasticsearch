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
package org.elasticsearch.xpack.ml.datafeed.extractor.aggregation;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregatorFactories;

import java.util.List;
import java.util.Objects;
import java.util.Set;

class AggregationDataExtractorContext {

    final String jobId;
    final String timeField;
    final Set<String> fields;
    final String[] indices;
    final String[] types;
    final QueryBuilder query;
    final AggregatorFactories.Builder aggs;
    final long start;
    final long end;
    final boolean includeDocCount;

    AggregationDataExtractorContext(String jobId, String timeField, Set<String> fields, List<String> indices, List<String> types,
                                    QueryBuilder query, AggregatorFactories.Builder aggs, long start, long end, boolean includeDocCount) {
        this.jobId = Objects.requireNonNull(jobId);
        this.timeField = Objects.requireNonNull(timeField);
        this.fields = Objects.requireNonNull(fields);
        this.indices = indices.toArray(new String[indices.size()]);
        this.types = types.toArray(new String[types.size()]);
        this.query = Objects.requireNonNull(query);
        this.aggs = Objects.requireNonNull(aggs);
        this.start = start;
        this.end = end;
        this.includeDocCount = includeDocCount;
    }
}
