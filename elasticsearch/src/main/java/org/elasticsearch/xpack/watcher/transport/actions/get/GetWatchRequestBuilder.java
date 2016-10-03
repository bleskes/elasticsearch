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

package org.elasticsearch.xpack.watcher.transport.actions.get;

import org.elasticsearch.action.support.master.MasterNodeReadOperationRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.index.VersionType;

/**
 * A delete document action request builder.
 */
public class GetWatchRequestBuilder extends MasterNodeReadOperationRequestBuilder<GetWatchRequest, GetWatchResponse,
        GetWatchRequestBuilder> {

    public GetWatchRequestBuilder(ElasticsearchClient client, String id) {
        super(client, GetWatchAction.INSTANCE, new GetWatchRequest(id));
    }


    public GetWatchRequestBuilder(ElasticsearchClient client) {
        super(client, GetWatchAction.INSTANCE, new GetWatchRequest());
    }

    public GetWatchRequestBuilder setId(String id) {
        request.setId(id);
        return this;
    }

    /**
     * Sets the type of versioning to use. Defaults to {@link org.elasticsearch.index.VersionType#INTERNAL}.
     */
    public GetWatchRequestBuilder setVersionType(VersionType versionType) {
        request.setVersionType(versionType);
        return this;
    }
}
