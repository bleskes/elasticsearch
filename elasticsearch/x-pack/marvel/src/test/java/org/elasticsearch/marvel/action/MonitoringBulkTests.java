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

package org.elasticsearch.marvel.action;

import org.elasticsearch.Version;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.AbstractRunnable;
import org.elasticsearch.marvel.MonitoredSystem;
import org.elasticsearch.marvel.agent.resolver.bulk.MonitoringBulkResolver;
import org.elasticsearch.marvel.test.MarvelIntegTestCase;
import org.elasticsearch.search.SearchHit;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertHitCount;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class MonitoringBulkTests extends MarvelIntegTestCase {

    @Override
    protected Settings transportClientSettings() {
        return super.transportClientSettings();
    }

    public void testMonitoringBulkIndexing() throws Exception {
        MonitoringBulkRequestBuilder requestBuilder = monitoringClient().prepareMonitoringBulk();
        String[] types = {"type1", "type2", "type3"};

        int numDocs = scaledRandomIntBetween(100, 5000);
        for (int i = 0; i < numDocs; i++) {
            MonitoringBulkDoc doc = new MonitoringBulkDoc(MonitoredSystem.KIBANA.getSystem(), Version.CURRENT.toString());
            doc.setType(randomFrom(types));
            doc.setSource(jsonBuilder().startObject().field("num", numDocs).endObject().bytes());
            requestBuilder.add(doc);
        }

        MonitoringBulkResponse response = requestBuilder.get();
        assertThat(response.getError(), is(nullValue()));
        refresh();

        SearchResponse searchResponse = client().prepareSearch().setTypes(types).setSize(numDocs).get();
        assertHitCount(searchResponse, numDocs);

        for (SearchHit searchHit : searchResponse.getHits()) {
            Map<String, Object> source = searchHit.sourceAsMap();
            assertNotNull(source.get(MonitoringBulkResolver.Fields.CLUSTER_UUID.underscore().toString()));
            assertNotNull(source.get(MonitoringBulkResolver.Fields.TIMESTAMP.underscore().toString()));
            assertNotNull(source.get(MonitoringBulkResolver.Fields.SOURCE_NODE.underscore().toString()));
        }
    }

    /**
     * This test creates N threads that execute a random number of monitoring bulk requests.
     */
    public void testConcurrentRequests() throws Exception {
        final Thread[] threads = new Thread[3 + randomInt(7)];
        final List<Throwable> exceptions = new CopyOnWriteArrayList<>();

        AtomicInteger total = new AtomicInteger(0);

        logger.info("--> using {} concurrent clients to execute requests", threads.length);
        for (int i = 0; i < threads.length; i++) {
            final int nbRequests = randomIntBetween(3, 10);

            threads[i] = new Thread(new AbstractRunnable() {
                @Override
                public void onFailure(Throwable t) {
                    logger.error("unexpected error in exporting thread", t);
                    exceptions.add(t);
                }

                @Override
                protected void doRun() throws Exception {
                    for (int j = 0; j < nbRequests; j++) {
                        MonitoringBulkRequestBuilder requestBuilder = monitoringClient().prepareMonitoringBulk();

                        int numDocs = scaledRandomIntBetween(10, 1000);
                        for (int k = 0; k < numDocs; k++) {
                            MonitoringBulkDoc doc = new MonitoringBulkDoc(MonitoredSystem.KIBANA.getSystem(), Version.CURRENT.toString());
                            doc.setType("concurrent");
                            doc.setSource(jsonBuilder().startObject().field("num", k).endObject().bytes());
                            requestBuilder.add(doc);
                        }

                        total.addAndGet(numDocs);
                        MonitoringBulkResponse response = requestBuilder.get();
                        assertThat(response.getError(), is(nullValue()));
                    }
                }
            }, "export_thread_" + i);
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertThat(exceptions, empty());
        refresh();

        SearchResponse countResponse = client().prepareSearch().setTypes("concurrent").setSize(0).get();
        assertHitCount(countResponse, total.get());
    }

    public void testUnsupportedSystem() throws Exception {
        MonitoringBulkRequestBuilder requestBuilder = monitoringClient().prepareMonitoringBulk();
        String[] types = {"type1", "type2", "type3"};

        int totalDocs = randomIntBetween(10, 1000);
        int unsupportedDocs = 0;

        for (int i = 0; i < totalDocs; i++) {
            MonitoringBulkDoc doc;
            if (randomBoolean()) {
                doc = new MonitoringBulkDoc("unknown", Version.CURRENT.toString());
                unsupportedDocs++;
            } else {
                doc = new MonitoringBulkDoc(MonitoredSystem.KIBANA.getSystem(), Version.CURRENT.toString());
            }
            doc.setType(randomFrom(types));
            doc.setSource(jsonBuilder().startObject().field("num", i).endObject().bytes());
            requestBuilder.add(doc);
        }

        MonitoringBulkResponse response = requestBuilder.get();
        if (unsupportedDocs == 0) {
            assertThat(response.getError(), is(nullValue()));
        } else {
            assertThat(response.getError(), is(notNullValue()));
        }
        refresh();

        SearchResponse countResponse = client().prepareSearch().setTypes(types).setSize(0).get();
        assertHitCount(countResponse, totalDocs - unsupportedDocs);
    }
}
