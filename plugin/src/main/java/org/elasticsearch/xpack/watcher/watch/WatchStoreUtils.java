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

package org.elasticsearch.xpack.watcher.watch;

import org.elasticsearch.cluster.metadata.AliasOrIndex;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.index.IndexNotFoundException;

public class WatchStoreUtils {

    /**
     * Method to get indexmetadata of a index, that potentially is behind an alias.
     *
     * @param name Name of the index or the alias
     * @param metaData Metadata to search for the name
     * @return IndexMetaData of the concrete index
     * @throws IllegalStateException If an alias points to two indices
     * @throws IndexNotFoundException If no index exists
     */
    public static IndexMetaData getConcreteIndex(String name, MetaData metaData) {
        AliasOrIndex aliasOrIndex = metaData.getAliasAndIndexLookup().get(name);
        if (aliasOrIndex == null) {
            throw new IndexNotFoundException(name);
        }

        if (aliasOrIndex.isAlias() && aliasOrIndex.getIndices().size() > 1) {
            throw new IllegalStateException("Alias [" + name + "] points to more than one index");
        }

        return aliasOrIndex.getIndices().get(0);
    }

}
