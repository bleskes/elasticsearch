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

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.security.InternalClient;
import org.elasticsearch.xpack.watcher.actions.ActionFactory;
import org.elasticsearch.xpack.watcher.support.init.proxy.WatcherClientProxy;

import java.io.IOException;

/**
 *
 */
public class IndexActionFactory extends ActionFactory<IndexAction, ExecutableIndexAction> {

    private final WatcherClientProxy client;
    private final TimeValue defaultTimeout;

    @Inject
    public IndexActionFactory(Settings settings, InternalClient client) {
        this(settings, new WatcherClientProxy(settings, client));
    }

    public IndexActionFactory(Settings settings, WatcherClientProxy client ) {
        super(Loggers.getLogger(IndexActionFactory.class, settings));
        this.client = client;
        this.defaultTimeout = settings.getAsTime("xpack.watcher.actions.index.default_timeout", null);
    }

    @Override
    public String type() {
        return IndexAction.TYPE;
    }

    @Override
    public IndexAction parseAction(String watchId, String actionId, XContentParser parser) throws IOException {
        return IndexAction.parse(watchId, actionId, parser);
    }

    @Override
    public ExecutableIndexAction createExecutable(IndexAction action) {
        return new ExecutableIndexAction(action, actionLogger, client, defaultTimeout);
    }
}
