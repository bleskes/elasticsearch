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
package org.elasticsearch.xpack.ml;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.ml.job.persistence.ElasticsearchMappings;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public final class MlMetaIndex {
    /**
     * Where to store the ml info in Elasticsearch - must match what's
     * expected by kibana/engineAPI/app/directives/mlLogUsage.js
     */
    public static final String INDEX_NAME = ".ml-meta";

    public static final String TYPE = "doc";

    private MlMetaIndex() {}

    public static XContentBuilder docMapping() throws IOException {
        XContentBuilder builder = jsonBuilder();
        builder.startObject();
        builder.startObject(TYPE);
        ElasticsearchMappings.addDefaultMapping(builder);
        builder.endObject();
        builder.endObject();
        return builder;
    }
}
