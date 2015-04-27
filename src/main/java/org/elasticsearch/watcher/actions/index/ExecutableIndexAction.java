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

package org.elasticsearch.watcher.actions.index;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.watcher.actions.Action;
import org.elasticsearch.watcher.actions.ExecutableAction;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.support.init.proxy.ClientProxy;
import org.elasticsearch.watcher.watch.Payload;

import java.util.HashMap;
import java.util.Map;

public class ExecutableIndexAction extends ExecutableAction<IndexAction> {

    private final ClientProxy client;

    public ExecutableIndexAction(IndexAction action, ESLogger logger, ClientProxy client) {
        super(action, logger);
        this.client = client;
    }

    @Override
    public Action.Result execute(String actionId, WatchExecutionContext ctx, Payload payload) throws Exception {
        IndexRequest indexRequest = new IndexRequest();
        indexRequest.index(action.index);
        indexRequest.type(action.docType);

        XContentBuilder resultBuilder = XContentFactory.jsonBuilder().prettyPrint();
        resultBuilder.startObject();
        resultBuilder.field("data", payload.data());
        resultBuilder.field("timestamp", ctx.executionTime());
        resultBuilder.endObject();
        indexRequest.source(resultBuilder);

        Map<String, Object> data = new HashMap<>();
        if (ctx.simulateAction(actionId)) {
            return new IndexAction.Result.Simulated(action.index, action.docType, new Payload.Simple(indexRequest.sourceAsMap()));
        }

        IndexResponse response = client.index(indexRequest);
        data.put("created", response.isCreated());
        data.put("id", response.getId());
        data.put("version", response.getVersion());
        data.put("type", response.getType());
        data.put("index", response.getIndex());
        return new IndexAction.Result.Success(new Payload.Simple(data));
    }
}


