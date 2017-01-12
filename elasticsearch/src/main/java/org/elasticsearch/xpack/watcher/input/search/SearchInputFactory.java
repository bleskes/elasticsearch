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

package org.elasticsearch.xpack.watcher.input.search;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.script.ScriptSettings;
import org.elasticsearch.xpack.security.InternalClient;
import org.elasticsearch.xpack.watcher.input.InputFactory;
import org.elasticsearch.xpack.watcher.input.simple.ExecutableSimpleInput;
import org.elasticsearch.xpack.watcher.support.init.proxy.WatcherClientProxy;
import org.elasticsearch.xpack.watcher.support.search.WatcherSearchTemplateService;

import java.io.IOException;

public class SearchInputFactory extends InputFactory<SearchInput, SearchInput.Result, ExecutableSearchInput> {
    private final Settings settings;
    private final WatcherClientProxy client;
    private final TimeValue defaultTimeout;
    private final WatcherSearchTemplateService searchTemplateService;

    @Inject
    public SearchInputFactory(Settings settings, InternalClient client,
            NamedXContentRegistry xContentRegistry, ScriptService scriptService) {
        this(settings, new WatcherClientProxy(settings, client), xContentRegistry, scriptService);
    }

    public SearchInputFactory(Settings settings, WatcherClientProxy client, NamedXContentRegistry xContentRegistry,
            ScriptService scriptService) {
        super(Loggers.getLogger(ExecutableSimpleInput.class, settings));
        this.settings = settings;
        this.client = client;
        this.defaultTimeout = settings.getAsTime("xpack.watcher.input.search.default_timeout", null);
        this.searchTemplateService = new WatcherSearchTemplateService(settings, scriptService, xContentRegistry);
    }

    @Override
    public String type() {
        return SearchInput.TYPE;
    }

    @Override
    public SearchInput parseInput(String watchId, XContentParser parser, boolean upgradeInputSource) throws IOException {
        String defaultLegacyScriptLanguage = ScriptSettings.getLegacyDefaultLang(settings);
        return SearchInput.parse(inputLogger, watchId, parser, upgradeInputSource, defaultLegacyScriptLanguage);
    }

    @Override
    public ExecutableSearchInput createExecutable(SearchInput input) {
        return new ExecutableSearchInput(input, inputLogger, client, searchTemplateService, defaultTimeout);
    }
}
