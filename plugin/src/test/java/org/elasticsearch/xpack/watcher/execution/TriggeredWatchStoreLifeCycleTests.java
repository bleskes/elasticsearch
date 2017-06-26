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

package org.elasticsearch.xpack.watcher.execution;

import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.xpack.watcher.condition.AlwaysCondition;
import org.elasticsearch.xpack.watcher.condition.Condition;
import org.elasticsearch.xpack.watcher.input.none.ExecutableNoneInput;
import org.elasticsearch.xpack.watcher.test.AbstractWatcherIntegrationTestCase;
import org.elasticsearch.xpack.watcher.trigger.schedule.ScheduleTriggerEvent;
import org.elasticsearch.xpack.watcher.watch.Watch;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class TriggeredWatchStoreLifeCycleTests extends AbstractWatcherIntegrationTestCase {

    public void testPutLoadUpdate() throws Exception {
        Condition condition = AlwaysCondition.INSTANCE;
        TriggeredWatchStore triggeredWatchStore = getInstanceFromMaster(TriggeredWatchStore.class);
        Watch watch = new Watch("_name", null, new ExecutableNoneInput(logger), condition, null, null, null, null, null);

        // Put watch records and verify that these are stored
        TriggeredWatch[] triggeredWatches = new TriggeredWatch[randomIntBetween(1, 50)];
        for (int i = 0; i < triggeredWatches.length; i++) {
            DateTime dateTime = new DateTime(i, DateTimeZone.UTC);
            ScheduleTriggerEvent event = new ScheduleTriggerEvent(watch.id(), dateTime, dateTime);
            Wid wid = new Wid("record_" + i, DateTime.now(DateTimeZone.UTC));
            triggeredWatches[i] = new TriggeredWatch(wid, event);
        }
        triggeredWatchStore.putAll(Arrays.asList(triggeredWatches));

        MultiGetRequest request = new MultiGetRequest();
        for (int i = 0; i < triggeredWatches.length; i++) {
            request.add(TriggeredWatchStore.INDEX_NAME, TriggeredWatchStore.DOC_TYPE, triggeredWatches[i].id().value());
        }
        Iterator<MultiGetItemResponse> iterator = client().multiGet(request).get().iterator();
        while (iterator.hasNext()) {
            assertThat(iterator.next().getResponse().isExists(), equalTo(true));
        }

        // Load the stored watch records
        ClusterService clusterService = getInstanceFromMaster(ClusterService.class);
        Collection<TriggeredWatch> loadedTriggeredWatches = triggeredWatchStore.loadTriggeredWatches(clusterService.state());
        assertThat(loadedTriggeredWatches, notNullValue());
        assertThat(loadedTriggeredWatches, hasSize(triggeredWatches.length));

        // Change the state to executed and update the watch records and then verify if the changes have been persisted too
        request = new MultiGetRequest();
        for (TriggeredWatch triggeredWatch : triggeredWatches) {
            assertThat(loadedTriggeredWatches.contains(triggeredWatch), is(true));
            triggeredWatchStore.delete(triggeredWatch.id());
            request.add(TriggeredWatchStore.INDEX_NAME, TriggeredWatchStore.DOC_TYPE, triggeredWatch.id().value());
        }

        iterator = client().multiGet(request).get().iterator();
        while (iterator.hasNext()) {
            assertThat(iterator.next().getResponse().isExists(), equalTo(false));
        }

        // try to load watch records, but none are in the await state, so no watch records are loaded.
        loadedTriggeredWatches = triggeredWatchStore.loadTriggeredWatches(clusterService.state());
        assertThat(loadedTriggeredWatches, notNullValue());
        assertThat(loadedTriggeredWatches, hasSize(0));
    }
}
