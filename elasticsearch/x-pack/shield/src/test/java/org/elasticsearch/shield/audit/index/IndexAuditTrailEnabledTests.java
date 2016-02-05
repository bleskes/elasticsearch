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

import org.apache.lucene.util.LuceneTestCase.BadApple;
import org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateResponse;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.cluster.metadata.IndexTemplateMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.audit.logfile.LoggingAuditTrail;
import org.elasticsearch.test.ESIntegTestCase.ClusterScope;
import org.elasticsearch.test.ESIntegTestCase.Scope;
import org.elasticsearch.test.ShieldIntegTestCase;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Collections;
import java.util.Set;

import static org.hamcrest.Matchers.is;

//test is just too slow, please fix it to not be sleep-based
@BadApple(bugUrl = "https://github.com/elastic/x-plugins/issues/1007")
@ClusterScope(scope = Scope.TEST, randomDynamicTemplates = false)
public class IndexAuditTrailEnabledTests extends ShieldIntegTestCase {
    IndexNameResolver.Rollover rollover = randomFrom(IndexNameResolver.Rollover.values());

    @Override
    protected Set<String> excludeTemplates() {
        return Collections.singleton(IndexAuditTrail.INDEX_TEMPLATE_NAME);
    }

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

    public void testAuditTrailIndexAndTemplateExists() throws Exception {
        awaitIndexTemplateCreation();

        // Wait for the index to be created since we have our own startup
        awaitAuditDocumentCreation();
    }

    public void testAuditTrailTemplateIsRecreatedAfterDelete() throws Exception {
        // this is already "tested" by the test framework since we wipe the templates before and after, but lets be explicit about the
        // behavior
        awaitIndexTemplateCreation();

        // delete the template
        DeleteIndexTemplateResponse deleteResponse = client().admin().indices()
                .prepareDeleteTemplate(IndexAuditTrail.INDEX_TEMPLATE_NAME).execute().actionGet();
        assertThat(deleteResponse.isAcknowledged(), is(true));
        awaitIndexTemplateCreation();
    }

    void awaitAuditDocumentCreation() throws Exception {
        final String indexName = IndexNameResolver.resolve(IndexAuditTrail.INDEX_NAME_PREFIX, DateTime.now(DateTimeZone.UTC), rollover);
        boolean success = awaitBusy(() -> {
            try {
                SearchResponse searchResponse = client().prepareSearch(indexName).setSize(0).setTerminateAfter(1).execute().actionGet();
                return searchResponse.getHits().totalHits() > 0;
            } catch (Exception e) {
                return false;
            }
        });

        assertThat("no audit document exists!", success, is(true));
    }

    void awaitIndexTemplateCreation() throws InterruptedException {
        boolean found = awaitBusy(() -> {
            GetIndexTemplatesResponse response = client().admin().indices()
                    .prepareGetTemplates(IndexAuditTrail.INDEX_TEMPLATE_NAME).execute().actionGet();
            if (response.getIndexTemplates().size() > 0) {
                for (IndexTemplateMetaData indexTemplateMetaData : response.getIndexTemplates()) {
                    if (IndexAuditTrail.INDEX_TEMPLATE_NAME.equals(indexTemplateMetaData.name())) {
                        return true;
                    }
                }
            }
            return false;
        });

        assertThat("index template [" + IndexAuditTrail.INDEX_TEMPLATE_NAME + "] was not created", found, is(true));
    }
}
