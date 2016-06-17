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

package org.elasticsearch.shield.action.interceptor;

import org.elasticsearch.action.fieldstats.FieldStatsRequest;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportRequest;

/**
 * Intercepts requests to shards to field level stats and strips fields that the user is not allowed to access from the response.
 */
public class FieldStatsRequestInterceptor extends FieldAndDocumentLevelSecurityRequestInterceptor<FieldStatsRequest> {
    @Inject
    public FieldStatsRequestInterceptor(Settings settings, ThreadPool threadPool) {
        super(settings, threadPool.getThreadContext());
    }

    @Override
    public boolean supports(TransportRequest request) {
        return request instanceof FieldStatsRequest;
    }

    @Override
    protected void disableFeatures(FieldStatsRequest request, boolean fieldLevelSecurityEnabled, boolean documentLevelSecurityEnabled) {
        if (fieldLevelSecurityEnabled) {
            request.setUseCache(false);
        }
    }
}
