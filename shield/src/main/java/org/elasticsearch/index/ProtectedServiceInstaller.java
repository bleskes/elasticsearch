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

package org.elasticsearch.index;

import org.elasticsearch.shield.ShieldPlugin;
import org.elasticsearch.shield.authz.accesscontrol.OptOutQueryCache;
import org.elasticsearch.shield.authz.accesscontrol.ShieldIndexSearcherWrapper;

/**
 * This class installs package protected extension points on the {@link IndexModule}
 */
public class ProtectedServiceInstaller {

    public static void install(IndexModule module, boolean clientMode) {
        module.indexSearcherWrapper = ShieldIndexSearcherWrapper.class;
        if (clientMode == false) {
            module.registerQueryCache(ShieldPlugin.OPT_OUT_QUERY_CACHE, OptOutQueryCache::new);
        }
    }

}
