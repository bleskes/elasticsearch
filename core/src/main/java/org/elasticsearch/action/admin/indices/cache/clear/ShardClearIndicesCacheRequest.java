/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.action.admin.indices.cache.clear;

import org.elasticsearch.action.support.indices.BaseBroadcastByNodeRequest;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.List;

/**
 *
 */
class ShardClearIndicesCacheRequest extends BaseBroadcastByNodeRequest<ClearIndicesCacheRequest> {
    ShardClearIndicesCacheRequest() {
    }

    ShardClearIndicesCacheRequest(ClearIndicesCacheRequest request, List<ShardRouting> shards, String nodeId) {
        super(nodeId, request, shards);
    }

    public boolean queryCache() {
        return getIndicesLevelRequest().queryCache();
    }

    public boolean requestCache() {
        return getIndicesLevelRequest().requestCache();
    }

    public boolean fieldDataCache() {
        return getIndicesLevelRequest().fieldDataCache();
    }

    public boolean recycler() {
        return getIndicesLevelRequest().recycler();
    }

    public String[] fields() {
        return getIndicesLevelRequest().fields();
    }

    @Override
    protected ClearIndicesCacheRequest newRequest() {
        return new ClearIndicesCacheRequest();
    }
}
