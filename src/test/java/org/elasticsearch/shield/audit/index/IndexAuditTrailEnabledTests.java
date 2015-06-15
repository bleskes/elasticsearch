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

package org.elasticsearch.shield.audit.index;

import com.google.common.base.Predicate;
import org.elasticsearch.action.exists.ExistsResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.audit.logfile.LoggingAuditTrail;
import org.elasticsearch.test.ElasticsearchIntegrationTest.ClusterScope;
import org.elasticsearch.test.ElasticsearchIntegrationTest.Scope;
import org.elasticsearch.test.ShieldIntegrationTest;
import org.junit.Test;

@ClusterScope(scope = Scope.TEST)
public class IndexAuditTrailEnabledTests extends ShieldIntegrationTest {

    IndexNameResolver.Rollover rollover = randomFrom(IndexNameResolver.Rollover.values());

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        Settings.Builder builder = Settings.builder()
                .put(super.nodeSettings(nodeOrdinal));
        builder.put("shield.audit.enabled", true);
        if (randomBoolean()) {
            builder.putArray("shield.audit.outputs", LoggingAuditTrail.NAME, IndexAuditTrail.NAME);
        } else {
            builder.putArray("shield.audit.outputs", IndexAuditTrail.NAME);
        }
        builder.put(IndexAuditTrail.ROLLOVER_SETTING, rollover);

        return builder.build();
    }

    @Test
    public void testIndexAuditTrailIndexExists() throws Exception {
        awaitIndexCreation();
    }

    void awaitIndexCreation() throws Exception {
        IndexNameResolver resolver = new IndexNameResolver();
        final String indexName = resolver.resolve(IndexAuditTrail.INDEX_NAME_PREFIX, System.currentTimeMillis(), rollover);
        boolean success = awaitBusy(new Predicate<Void>() {
            @Override
            public boolean apply(Void o) {
                try {
                    ExistsResponse response =
                            client().prepareExists(indexName).execute().actionGet();
                    return response.exists();
                } catch (Exception e) {
                    return false;
                }
            }
        });

        if (!success) {
            fail("index [" + indexName + "] was not created");
        }
    }
}
