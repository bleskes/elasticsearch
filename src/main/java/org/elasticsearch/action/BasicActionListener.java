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
package org.elasticsearch.action;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.transport.TransportChannel;
import org.elasticsearch.transport.TransportResponse;

public class BasicActionListener<Response extends TransportResponse> implements ActionListener<Response> {

    final TransportChannel channel;
    final String name;
    final ESLogger logger;

    public BasicActionListener(TransportChannel channel, String name, ESLogger logger) {
        this.channel = channel;
        this.name = name;
        this.logger = logger;
    }

    @Override
    public void onResponse(Response result) {
        try {
            channel.sendResponse(result);
        } catch (Throwable e) {
            onFailure(e);
        }
    }

    @Override
    public void onFailure(Throwable e) {
        try {
            channel.sendResponse(e);
        } catch (Throwable e1) {
            logger.warn("Failed to send failure for [{}]. original failure [{}]", e1, name, e);
        }
    }
}

