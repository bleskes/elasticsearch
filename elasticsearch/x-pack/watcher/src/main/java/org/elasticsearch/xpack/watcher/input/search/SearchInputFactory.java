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

import java.io.IOException;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.query.QueryParseContext;
import org.elasticsearch.indices.query.IndicesQueriesRegistry;
import org.elasticsearch.search.aggregations.AggregatorParsers;
import org.elasticsearch.search.suggest.Suggesters;
import org.elasticsearch.xpack.security.InternalClient;
import org.elasticsearch.xpack.watcher.input.InputFactory;
import org.elasticsearch.xpack.watcher.input.simple.ExecutableSimpleInput;
import org.elasticsearch.xpack.watcher.support.init.proxy.WatcherClientProxy;

/**
 *
 */
public class SearchInputFactory extends InputFactory<SearchInput, SearchInput.Result, ExecutableSearchInput> {

    private final WatcherClientProxy client;
    private final TimeValue defaultTimeout;
    private final IndicesQueriesRegistry queryRegistry;
    private final AggregatorParsers aggParsers;
    private final Suggesters suggesters;
    private final ParseFieldMatcher parseFieldMatcher;

    @Inject
    public SearchInputFactory(Settings settings, InternalClient client, IndicesQueriesRegistry queryRegistry,
                              AggregatorParsers aggParsers, Suggesters suggesters) {
        this(settings, new WatcherClientProxy(settings, client), queryRegistry, aggParsers, suggesters);
    }

    public SearchInputFactory(Settings settings, WatcherClientProxy client, IndicesQueriesRegistry queryRegistry,
                              AggregatorParsers aggParsers, Suggesters suggesters) {
        super(Loggers.getLogger(ExecutableSimpleInput.class, settings));
        this.parseFieldMatcher = new ParseFieldMatcher(settings);
        this.client = client;
        this.queryRegistry = queryRegistry;
        this.aggParsers = aggParsers;
        this.suggesters = suggesters;
        this.defaultTimeout = settings.getAsTime("xpack.watcher.input.search.default_timeout", null);
    }

    @Override
    public String type() {
        return SearchInput.TYPE;
    }

    @Override
    public SearchInput parseInput(String watchId, XContentParser parser) throws IOException {
        QueryParseContext context = new QueryParseContext(queryRegistry, parser, parseFieldMatcher);
        return SearchInput.parse(watchId, parser, context, aggParsers, suggesters);
    }

    @Override
    public ExecutableSearchInput createExecutable(SearchInput input) {
        return new ExecutableSearchInput(input, inputLogger, client, defaultTimeout);
    }
}
