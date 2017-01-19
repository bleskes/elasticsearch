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

import org.elasticsearch.common.lease.Releasable;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.search.SearchHit;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

class SearchHitToJsonProcessor implements Releasable {

    private final ExtractedFields fields;
    private final XContentBuilder jsonBuilder;

    public SearchHitToJsonProcessor(ExtractedFields fields, OutputStream outputStream) throws IOException {
        this.fields = Objects.requireNonNull(fields);
        this.jsonBuilder = new XContentBuilder(JsonXContent.jsonXContent, outputStream);
    }

    public void process(SearchHit hit) throws IOException {
        jsonBuilder.startObject();
        for (ExtractedField field : fields.getAllFields()) {
            writeKeyValue(field.getName(), field.value(hit));
        }
        jsonBuilder.endObject();
    }

    private void writeKeyValue(String key, Object... values) throws IOException {
        if (values.length == 0) {
            return;
        }
        if (values.length == 1) {
            jsonBuilder.field(key, values[0]);
        } else {
            jsonBuilder.array(key, values);
        }
    }

    @Override
    public void close() {
        jsonBuilder.close();
    }
}
