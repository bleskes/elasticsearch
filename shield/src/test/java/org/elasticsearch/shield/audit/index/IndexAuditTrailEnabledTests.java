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
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesResponse;
import org.elasticsearch.action.exists.ExistsResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.audit.logfile.LoggingAuditTrail;
import org.elasticsearch.test.ElasticsearchIntegrationTest.ClusterScope;
import org.elasticsearch.test.ElasticsearchIntegrationTest.Scope;
import org.elasticsearch.test.ShieldIntegrationTest;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.hamcrest.Matchers.*;

@ClusterScope(scope = Scope.TEST, randomDynamicTemplates = false)
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

    @Override
    public void beforeIndexDeletion() {
        // For this test, this is a NO-OP because the index audit trail will continue to capture events and index after
        // the tests have completed. The default implementation of this method expects that nothing is performing operations
        // after the test has completed
    }

    @Test
    public void testAuditTrailIndexAndTemplateExists() throws Exception {
        GetIndexTemplatesResponse response = client().admin().indices().prepareGetTemplates(IndexAuditTrail.INDEX_TEMPLATE_NAME).execute().actionGet();
        assertThat(response.getIndexTemplates().size(), is(1));
        assertThat(response.getIndexTemplates().get(0).name(), is(IndexAuditTrail.INDEX_TEMPLATE_NAME));

        // Wait for the index to be created since we have our own startup
        awaitIndexCreation();
    }

    void awaitIndexCreation() throws Exception {
        final String indexName = IndexNameResolver.resolve(IndexAuditTrail.INDEX_NAME_PREFIX, DateTime.now(DateTimeZone.UTC), rollover);
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
