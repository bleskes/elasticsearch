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

package org.elasticsearch.xpack.watcher.transform.search;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.script.ScriptSettings;
import org.elasticsearch.search.SearchRequestParsers;
import org.elasticsearch.xpack.security.InternalClient;
import org.elasticsearch.xpack.watcher.support.init.proxy.WatcherClientProxy;
import org.elasticsearch.xpack.watcher.support.search.WatcherSearchTemplateService;
import org.elasticsearch.xpack.watcher.transform.TransformFactory;

import java.io.IOException;

public class SearchTransformFactory extends TransformFactory<SearchTransform, SearchTransform.Result, ExecutableSearchTransform> {
    private final Settings settings;
    protected final WatcherClientProxy client;
    private final TimeValue defaultTimeout;
    private final SearchRequestParsers searchRequestParsers;
    private final ParseFieldMatcher parseFieldMatcher;
    private final WatcherSearchTemplateService searchTemplateService;

    public SearchTransformFactory(Settings settings, InternalClient client, SearchRequestParsers searchRequestParsers,
            NamedXContentRegistry xContentRegistry, ScriptService scriptService) {
        this(settings, new WatcherClientProxy(settings, client), searchRequestParsers, xContentRegistry, scriptService);
    }

    public SearchTransformFactory(Settings settings, WatcherClientProxy client, SearchRequestParsers searchRequestParsers,
            NamedXContentRegistry xContentRegistry, ScriptService scriptService) {
        super(Loggers.getLogger(ExecutableSearchTransform.class, settings));
        this.settings = settings;
        this.client = client;
        this.parseFieldMatcher = new ParseFieldMatcher(settings);
        this.searchRequestParsers = searchRequestParsers;
        this.defaultTimeout = settings.getAsTime("xpack.watcher.transform.search.default_timeout", null);
        this.searchTemplateService = new WatcherSearchTemplateService(settings, scriptService, searchRequestParsers, xContentRegistry);
    }

    @Override
    public String type() {
        return SearchTransform.TYPE;
    }

    @Override
    public SearchTransform parseTransform(String watchId, XContentParser parser, boolean upgradeTransformSource) throws IOException {
        String defaultLegacyScriptLanguage = ScriptSettings.getLegacyDefaultLang(settings);
        return SearchTransform.parse(transformLogger, watchId, parser, upgradeTransformSource, defaultLegacyScriptLanguage,
                parseFieldMatcher, searchRequestParsers);
    }

    @Override
    public ExecutableSearchTransform createExecutable(SearchTransform transform) {
        return new ExecutableSearchTransform(transform, transformLogger, client,  searchTemplateService, defaultTimeout);
    }
}
