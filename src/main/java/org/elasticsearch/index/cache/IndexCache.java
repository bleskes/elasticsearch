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

package org.elasticsearch.index.cache;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterStateListener;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.component.CloseableComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.AbstractIndexComponent;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.cache.filter.FilterCache;
import org.elasticsearch.index.cache.fixedbitset.FixedBitSetFilterCache;
import org.elasticsearch.index.cache.query.parser.QueryParserCache;
import org.elasticsearch.index.settings.IndexSettings;

/**
 *
 */
public class IndexCache extends AbstractIndexComponent implements CloseableComponent, ClusterStateListener {

    private final FilterCache filterCache;
    private final QueryParserCache queryParserCache;
    private final FixedBitSetFilterCache fixedBitSetFilterCache;

    private ClusterService clusterService;

    @Inject
    public IndexCache(Index index, @IndexSettings Settings indexSettings, FilterCache filterCache, QueryParserCache queryParserCache, FixedBitSetFilterCache fixedBitSetFilterCache) {
        super(index, indexSettings);
        this.filterCache = filterCache;
        this.queryParserCache = queryParserCache;
        this.fixedBitSetFilterCache = fixedBitSetFilterCache;
    }

    @Inject(optional = true)
    public void setClusterService(@Nullable ClusterService clusterService) {
        this.clusterService = clusterService;
        if (clusterService != null) {
            clusterService.add(this);
        }
    }

    public FilterCache filter() {
        return filterCache;
    }

    /**
     * Return the {@link FixedBitSetFilterCache} for this index.
     */
    public FixedBitSetFilterCache fixedBitSetFilterCache() {
        return fixedBitSetFilterCache;
    }

    public QueryParserCache queryParserCache() {
        return this.queryParserCache;
    }

    @Override
    public void close() throws ElasticsearchException {
        filterCache.close();
        queryParserCache.close();
        fixedBitSetFilterCache.close();
        if (clusterService != null) {
            clusterService.remove(this);
        }
    }

    public void clear(String reason) {
        filterCache.clear(reason);
        queryParserCache.clear();
        fixedBitSetFilterCache.clear(reason);
    }

    @Override
    public void clusterChanged(ClusterChangedEvent event) {
        // clear the query parser cache if the metadata (mappings) changed...
        // this have to be done after the new state is live (i.e., via a ClusterStateListener
        if (event.metaDataChanged()) {
            queryParserCache.clear();
        }
    }
}
