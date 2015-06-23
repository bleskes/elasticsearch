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

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.actions.ActionFactory;
import org.elasticsearch.watcher.actions.email.ExecutableEmailAction;
import org.elasticsearch.watcher.support.DynamicIndexName;
import org.elasticsearch.watcher.support.init.proxy.ClientProxy;

import java.io.IOException;

/**
 *
 */
public class IndexActionFactory extends ActionFactory<IndexAction, ExecutableIndexAction> {

    private final ClientProxy client;
    private final DynamicIndexName.Parser indexNamesParser;

    @Inject
    public IndexActionFactory(Settings settings, ClientProxy client) {
        super(Loggers.getLogger(ExecutableEmailAction.class, settings));
        this.client = client;
        String defaultDateFormat = DynamicIndexName.defaultDateFormat(settings, "watcher.actions.index");
        this.indexNamesParser = new DynamicIndexName.Parser(defaultDateFormat);
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
        return new ExecutableIndexAction(action, actionLogger, client, indexNamesParser);
    }
}
