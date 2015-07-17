/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */

package org.elasticsearch.marvel.agent.collector.indices;

import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.marvel.agent.exporter.MarvelDoc;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class IndexMarvelDoc extends MarvelDoc<IndexMarvelDoc> {

    private final String index;
    private final Docs docs;
    private final Store store;
    private final Indexing indexing;

    public IndexMarvelDoc(String clusterName, String type, long timestamp,
                          String index, Docs docs, Store store, Indexing indexing) {
        super(clusterName, type, timestamp);
        this.index = index;
        this.docs = docs;
        this.store = store;
        this.indexing = indexing;
    }

    @Override
    public IndexMarvelDoc payload() {
        return this;
    }

    public String getIndex() {
        return index;
    }

    public Docs getDocs() {
        return docs;
    }

    public Store getStore() {
        return store;
    }

    public Indexing getIndexing() {
        return indexing;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        super.toXContent(builder, params);
        builder.field(Fields.INDEX, index);
        if (docs != null) {
            docs.toXContent(builder, params);
        }
        if (store != null) {
            store.toXContent(builder, params);
        }
        if (indexing != null) {
            indexing.toXContent(builder, params);
        }
        builder.endObject();
        return builder;
    }

    public static IndexMarvelDoc createMarvelDoc(String clusterName, String type, long timestamp,
                                                 String index, long docsCount, long storeSizeInBytes, long storeThrottleTimeInMillis, long indexingThrottleTimeInMillis) {
        return new IndexMarvelDoc(clusterName, type, timestamp, index,
                                    new Docs(docsCount),
                                    new Store(storeSizeInBytes, storeThrottleTimeInMillis),
                                    new Indexing(indexingThrottleTimeInMillis));
    }

    static final class Fields {
        static final XContentBuilderString INDEX = new XContentBuilderString("index");
    }

    static class Docs implements ToXContent {

        private final long count;

        Docs(long count) {
            this.count = count;
        }

        public long getCount() {
            return count;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject(Fields.DOCS);
            builder.field(Fields.COUNT, count);
            builder.endObject();
            return builder;
        }

        static final class Fields {
            static final XContentBuilderString DOCS = new XContentBuilderString("docs");
            static final XContentBuilderString COUNT = new XContentBuilderString("count");
        }
    }

    static class Store implements ToXContent {

        private final long sizeInBytes;
        private final long throttleTimeInMillis;

        public Store(long sizeInBytes, long throttleTimeInMillis) {
            this.sizeInBytes = sizeInBytes;
            this.throttleTimeInMillis = throttleTimeInMillis;
        }

        public long getSizeInBytes() {
            return sizeInBytes;
        }

        public long getThrottleTimeInMillis() {
            return throttleTimeInMillis;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject(Fields.STORE);
            builder.field(Fields.SIZE_IN_BYTES, sizeInBytes);
            builder.timeValueField(Fields.THROTTLE_TIME_IN_MILLIS, Fields.THROTTLE_TIME, new TimeValue(throttleTimeInMillis, TimeUnit.MILLISECONDS));
            builder.endObject();
            return builder;
        }

        static final class Fields {
            static final XContentBuilderString STORE = new XContentBuilderString("store");
            static final XContentBuilderString SIZE_IN_BYTES = new XContentBuilderString("size_in_bytes");
            static final XContentBuilderString THROTTLE_TIME = new XContentBuilderString("throttle_time");
            static final XContentBuilderString THROTTLE_TIME_IN_MILLIS = new XContentBuilderString("throttle_time_in_millis");
        }
    }

    static class Indexing implements ToXContent {

        private final long throttleTimeInMillis;

        public Indexing(long throttleTimeInMillis) {
            this.throttleTimeInMillis = throttleTimeInMillis;
        }

        public long getThrottleTimeInMillis() {
            return throttleTimeInMillis;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject(Fields.INDEXING);
            builder.timeValueField(Fields.THROTTLE_TIME_IN_MILLIS, Fields.THROTTLE_TIME, new TimeValue(throttleTimeInMillis, TimeUnit.MILLISECONDS));
            builder.endObject();
            return builder;
        }

        static final class Fields {
            static final XContentBuilderString INDEXING = new XContentBuilderString("indexing");
            static final XContentBuilderString THROTTLE_TIME = new XContentBuilderString("throttle_time");
            static final XContentBuilderString THROTTLE_TIME_IN_MILLIS = new XContentBuilderString("throttle_time_in_millis");
        }
    }
}

