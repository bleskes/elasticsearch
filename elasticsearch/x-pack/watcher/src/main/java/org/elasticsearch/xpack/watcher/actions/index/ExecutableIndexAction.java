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

package org.elasticsearch.xpack.watcher.actions.index;

import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.xpack.watcher.actions.Action;
import org.elasticsearch.xpack.watcher.actions.ExecutableAction;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.support.ArrayObjectIterator;
import org.elasticsearch.xpack.watcher.support.WatcherDateTimeUtils;
import org.elasticsearch.xpack.watcher.support.init.proxy.WatcherClientProxy;
import org.elasticsearch.xpack.watcher.support.xcontent.XContentSource;
import org.elasticsearch.xpack.watcher.watch.Payload;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.xpack.watcher.support.Exceptions.illegalState;

public class ExecutableIndexAction extends ExecutableAction<IndexAction> {

    private final WatcherClientProxy client;
    private final TimeValue timeout;

    public ExecutableIndexAction(IndexAction action, ESLogger logger, WatcherClientProxy client, @Nullable TimeValue defaultTimeout) {
        super(action, logger);
        this.client = client;
        this.timeout = action.timeout != null ? action.timeout : defaultTimeout;
    }

    @Override
    public Action.Result execute(String actionId, WatchExecutionContext ctx, Payload payload) throws Exception {
        Map<String, Object> data = payload.data();
        if (data.containsKey("_doc")) {
            Object doc = data.get("_doc");
            if (doc instanceof Iterable) {
                return indexBulk((Iterable) doc, actionId, ctx);
            }
            if (doc.getClass().isArray()) {
                return indexBulk(new ArrayObjectIterator.Iterable(doc), actionId, ctx);
            }
            if (doc instanceof Map) {
                data = (Map<String, Object>) doc;
            } else {
                throw illegalState("could not execute action [{}] of watch [{}]. failed to index payload data." +
                        "[_data] field must either hold a Map or an List/Array of Maps", actionId, ctx.watch().id());
            }
        }

        IndexRequest indexRequest = new IndexRequest();
        indexRequest.index(action.index);
        indexRequest.type(action.docType);
        data = addTimestampToDocument(data, ctx.executionTime());
        indexRequest.source(jsonBuilder().prettyPrint().map(data));

        if (ctx.simulateAction(actionId)) {
            return new IndexAction.Result.Simulated(indexRequest.index(), action.docType, new XContentSource(indexRequest.source(),
                    XContentType.JSON));
        }

        IndexResponse response = client.index(indexRequest, timeout);
        XContentBuilder jsonBuilder = jsonBuilder();
        indexResponseToXContent(jsonBuilder, response);
        return new IndexAction.Result.Success(new XContentSource(jsonBuilder));
    }

    Action.Result indexBulk(Iterable list, String actionId, WatchExecutionContext ctx) throws Exception {
        BulkRequest bulkRequest = new BulkRequest();
        for (Object item : list) {
            if (!(item instanceof Map)) {
                throw illegalState("could not execute action [{}] of watch [{}]. failed to index payload data. " +
                        "[_data] field must either hold a Map or an List/Array of Maps", actionId, ctx.watch().id());
            }
            Map<String, Object> doc = (Map<String, Object>) item;
            IndexRequest indexRequest = new IndexRequest();
            indexRequest.index(action.index);
            indexRequest.type(action.docType);
            doc = addTimestampToDocument(doc, ctx.executionTime());
            indexRequest.source(jsonBuilder().prettyPrint().map(doc));
            bulkRequest.add(indexRequest);
        }
        BulkResponse bulkResponse = client.bulk(bulkRequest, action.timeout);
        XContentBuilder jsonBuilder = jsonBuilder().startArray();
        for (BulkItemResponse item : bulkResponse) {
            IndexResponse response = item.getResponse();
            indexResponseToXContent(jsonBuilder, response);
        }
        jsonBuilder.endArray();
        return new IndexAction.Result.Success(new XContentSource(jsonBuilder.bytes(), XContentType.JSON));
    }

    private Map<String, Object> addTimestampToDocument(Map<String, Object> data, DateTime executionTime) {
        if (action.executionTimeField != null) {
            if (!(data instanceof HashMap)) {
                data = new HashMap<>(data); // ensuring mutability
            }
            data.put(action.executionTimeField, WatcherDateTimeUtils.formatDate(executionTime));
        }
        return data;
    }

    static void indexResponseToXContent(XContentBuilder builder, IndexResponse response) throws IOException {
        builder.startObject()
                .field("created", response.isCreated())
                .field("id", response.getId())
                .field("version", response.getVersion())
                .field("type", response.getType())
                .field("index", response.getIndex())
                .endObject();
    }
}


