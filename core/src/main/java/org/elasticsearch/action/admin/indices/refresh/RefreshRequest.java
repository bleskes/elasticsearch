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

package org.elasticsearch.action.admin.indices.refresh;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.support.broadcast.BroadcastRequest;
import org.elasticsearch.action.support.indices.IndicesLevelRequest;

/**
 * A refresh request making all operations performed since the last refresh available for search. The (near) real-time
 * capabilities depends on the index engine used. For example, the internal one requires refresh to be called, but by
 * default a refresh is scheduled periodically.
 *
 * @see org.elasticsearch.client.Requests#refreshRequest(String...)
 * @see org.elasticsearch.client.IndicesAdminClient#refresh(RefreshRequest)
 * @see RefreshResponse
 */
public class RefreshRequest extends IndicesLevelRequest<RefreshRequest> {


    RefreshRequest() {
    }

    /**
     * Copy constructor that creates a new refresh request that is a copy of the one provided as an argument.
     * The new request will inherit though headers and context from the original request that caused it.
     */
    public RefreshRequest(ActionRequest originalRequest) {
        super(originalRequest);
    }

    public RefreshRequest(String... indices) {
        super(indices);
    }

}
