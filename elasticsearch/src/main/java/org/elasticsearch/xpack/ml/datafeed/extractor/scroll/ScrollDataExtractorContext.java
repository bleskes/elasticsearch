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
package org.elasticsearch.xpack.ml.datafeed.extractor.scroll;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.util.List;
import java.util.Objects;

class ScrollDataExtractorContext {

    final String jobId;
    final ExtractedFields extractedFields;
    final String[] indexes;
    final String[] types;
    final QueryBuilder query;
    final List<SearchSourceBuilder.ScriptField> scriptFields;
    final int scrollSize;
    final long start;
    final long end;

    public ScrollDataExtractorContext(String jobId, ExtractedFields extractedFields, List<String> indexes, List<String> types,
                                      QueryBuilder query, List<SearchSourceBuilder.ScriptField> scriptFields, int scrollSize,
                                      long start, long end) {
        this.jobId = Objects.requireNonNull(jobId);
        this.extractedFields = Objects.requireNonNull(extractedFields);
        this.indexes = indexes.toArray(new String[indexes.size()]);
        this.types = types.toArray(new String[types.size()]);
        this.query = Objects.requireNonNull(query);
        this.scriptFields = Objects.requireNonNull(scriptFields);
        this.scrollSize = scrollSize;
        this.start = start;
        this.end = end;
    }
}
