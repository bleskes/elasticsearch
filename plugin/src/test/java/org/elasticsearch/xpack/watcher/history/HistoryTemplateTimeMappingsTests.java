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

package org.elasticsearch.xpack.watcher.history;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.xpack.watcher.condition.AlwaysCondition;
import org.elasticsearch.xpack.watcher.execution.ExecutionState;
import org.elasticsearch.xpack.watcher.test.AbstractWatcherIntegrationTestCase;
import org.elasticsearch.xpack.watcher.transport.actions.put.PutWatchResponse;

import java.io.IOException;
import java.util.Map;

import static org.elasticsearch.common.xcontent.support.XContentMapValues.extractValue;
import static org.elasticsearch.xpack.watcher.actions.ActionBuilders.loggingAction;
import static org.elasticsearch.xpack.watcher.client.WatchSourceBuilders.watchBuilder;
import static org.elasticsearch.xpack.watcher.input.InputBuilders.simpleInput;
import static org.elasticsearch.xpack.watcher.trigger.TriggerBuilders.schedule;
import static org.elasticsearch.xpack.watcher.trigger.schedule.Schedules.interval;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * This test makes sure that the different time fields in the watch_record are mapped as date types
 */
public class HistoryTemplateTimeMappingsTests extends AbstractWatcherIntegrationTestCase {
    @Override
    protected boolean timeWarped() {
        return true; // just to have better control over the triggers
    }

    @Override
    protected boolean enableSecurity() {
        return false; // remove security noise from this test
    }

    public void testTimeFields() throws Exception {
        PutWatchResponse putWatchResponse = watcherClient().preparePutWatch("_id").setSource(watchBuilder()
                .trigger(schedule(interval("5s")))
                .input(simpleInput())
                .condition(AlwaysCondition.INSTANCE)
                .addAction("_logging", loggingAction("foobar")))
                .get();

        assertThat(putWatchResponse.isCreated(), is(true));
        timeWarp().scheduler().trigger("_id");
        flush();
        refresh();

        // the action should fail as no email server is available
        assertWatchWithMinimumActionsCount("_id", ExecutionState.EXECUTED, 1);

        assertBusy(() -> {
            GetMappingsResponse mappingsResponse = client().admin().indices().prepareGetMappings().get();
            assertThat(mappingsResponse, notNullValue());
            assertThat(mappingsResponse.getMappings().isEmpty(), is(false));
            for (ObjectObjectCursor<String, ImmutableOpenMap<String, MappingMetaData>> metadatas : mappingsResponse.getMappings()) {
                if (!metadatas.key.startsWith(HistoryStore.INDEX_PREFIX)) {
                    continue;
                }
                MappingMetaData metadata = metadatas.value.get("doc");

                assertThat(metadata, notNullValue());
                try {
                    Map<String, Object> source = metadata.getSourceAsMap();
                    logger.info("checking index [{}] with metadata:\n[{}]", metadatas.key, metadata.source().toString());
                    assertThat(extractValue("properties.trigger_event.properties.type.type", source), is((Object) "keyword"));
                    assertThat(extractValue("properties.trigger_event.properties.triggered_time.type", source), is((Object) "date"));
                    assertThat(extractValue("properties.trigger_event.properties.schedule.properties.scheduled_time.type", source),
                            is((Object) "date"));
                    assertThat(extractValue("properties.result.properties.execution_time.type", source), is((Object) "date"));
                } catch (ElasticsearchParseException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
