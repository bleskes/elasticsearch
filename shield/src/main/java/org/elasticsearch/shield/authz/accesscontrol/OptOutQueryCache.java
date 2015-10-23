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

package org.elasticsearch.shield.authz.accesscontrol;

import org.apache.lucene.search.QueryCachingPolicy;
import org.apache.lucene.search.Weight;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.support.broadcast.BroadcastShardRequest;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.index.AbstractIndexComponent;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.cache.query.QueryCache;
import org.elasticsearch.indices.cache.query.IndicesQueryCache;
import org.elasticsearch.search.internal.ShardSearchRequest;
import org.elasticsearch.shield.authz.InternalAuthorizationService;

/**
 * Opts out of the query cache if field level security is active for the current request.
 */
public final class OptOutQueryCache extends AbstractIndexComponent implements QueryCache {

    final IndicesQueryCache indicesQueryCache;

    @Inject
    public OptOutQueryCache(IndexSettings indexSettings, IndicesQueryCache indicesQueryCache) {
        super(indexSettings);
        this.indicesQueryCache = indicesQueryCache;
    }

    @Override
    public void close() throws ElasticsearchException {
        clear("close");
    }

    @Override
    public void clear(String reason) {
        logger.debug("full cache clear, reason [{}]", reason);
        indicesQueryCache.clearIndex(index().getName());
    }

    @Override
    public Weight doCache(Weight weight, QueryCachingPolicy policy) {
        final RequestContext context = RequestContext.current();
        if (context == null) {
            throw new IllegalStateException("opting out of the query cache. current request can't be found");
        }
        final IndicesAccessControl indicesAccessControl = context.getRequest().getFromContext(InternalAuthorizationService.INDICES_PERMISSIONS_KEY);
        if (indicesAccessControl == null) {
            logger.debug("opting out of the query cache. current request doesn't hold indices permissions");
            return weight;
        }

        // At this level only IndicesRequest
        final String index;
        if (context.getRequest() instanceof ShardSearchRequest) {
            index = ((ShardSearchRequest) context.getRequest()).index();
        } else if (context.getRequest() instanceof BroadcastShardRequest) {
            index = ((BroadcastShardRequest) context.getRequest()).shardId().getIndex();
        } else {
            return weight;
        }

        IndicesAccessControl.IndexAccessControl indexAccessControl = indicesAccessControl.getIndexPermissions(index);
        if (indexAccessControl != null && indexAccessControl.getFields() != null) {
            logger.debug("opting out of the query cache. request for index [{}] has field level security enabled", index);
            // If in the future there is a Query#extractFields() then we can be smart on when to skip the query cache.
            // (only cache if all fields in the query also are defined in the role)
            return weight;
        } else {
            logger.trace("not opting out of the query cache. request for index [{}] has field level security disabled", index);
            return indicesQueryCache.doCache(weight, policy);
        }
    }
}
