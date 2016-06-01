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

package org.elasticsearch.messy.tests;

import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse.FieldMappingMetaData;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.script.groovy.GroovyPlugin;
import org.elasticsearch.xpack.watcher.execution.ExecutionState;
import org.elasticsearch.xpack.watcher.test.AbstractWatcherIntegrationTestCase;
import org.elasticsearch.xpack.watcher.transport.actions.put.PutWatchResponse;

import java.util.List;
import java.util.Map;

import static org.elasticsearch.xpack.watcher.actions.ActionBuilders.loggingAction;
import static org.elasticsearch.xpack.watcher.client.WatchSourceBuilders.watchBuilder;
import static org.elasticsearch.xpack.watcher.condition.ConditionBuilders.alwaysCondition;
import static org.elasticsearch.xpack.watcher.input.InputBuilders.simpleInput;
import static org.elasticsearch.xpack.watcher.transform.TransformBuilders.scriptTransform;
import static org.elasticsearch.xpack.trigger.TriggerBuilders.schedule;
import static org.elasticsearch.xpack.trigger.schedule.Schedules.interval;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

public class HistoryTemplateTransformMappingsIT extends AbstractWatcherIntegrationTestCase {

    @Override
    protected List<Class<? extends Plugin>> pluginTypes() {
        List<Class<? extends Plugin>> types = super.pluginTypes();
        types.add(GroovyPlugin.class);
        return types;
    }

    @Override
    protected boolean timeWarped() {
        return true; // just to have better control over the triggers
    }

    @Override
    protected boolean enableShield() {
        return false; // remove shield noise from this test
    }

    public void testTransformFields() throws Exception {
        String index = "the-index";
        String type = "the-type";
        createIndex(index);
        index(index, type, "{}");
        flush();
        refresh();

        PutWatchResponse putWatchResponse = watcherClient().preparePutWatch("_id1").setSource(watchBuilder()
                .trigger(schedule(interval("5s")))
                .input(simpleInput())
                .condition(alwaysCondition())
                .transform(scriptTransform("return [ 'key' : 'value1' ];"))
                .addAction("logger", scriptTransform("return [ 'key' : 'value2' ];"), loggingAction("indexed")))
                .get();
        assertThat(putWatchResponse.isCreated(), is(true));
        timeWarp().scheduler().trigger("_id1");

        // adding another watch which with a transform that should conflict with the preview watch. Since the
        // mapping for the transform construct is disabled, there should be nor problems.
        putWatchResponse = watcherClient().preparePutWatch("_id2").setSource(watchBuilder()
                .trigger(schedule(interval("5s")))
                .input(simpleInput())
                .condition(alwaysCondition())
                .transform(scriptTransform("return [ 'key' : [ 'key1' : 'value1' ] ];"))
                .addAction("logger", scriptTransform("return [ 'key' : [ 'key1' : 'value2' ] ];"), loggingAction("indexed")))
                .get();
        assertThat(putWatchResponse.isCreated(), is(true));
        timeWarp().scheduler().trigger("_id2");

        flush();
        refresh();

        assertWatchWithMinimumActionsCount("_id1", ExecutionState.EXECUTED, 1);
        assertWatchWithMinimumActionsCount("_id2", ExecutionState.EXECUTED, 1);

        refresh();

        assertBusy(() -> {
            GetFieldMappingsResponse getFieldMappingsResponse = client().admin().indices()
                    .prepareGetFieldMappings(".watcher-history*").setFields("result.actions.transform.payload")
                    .setTypes("watch_record").includeDefaults(true).get();

            for (Map<String, Map<String, FieldMappingMetaData>> map : getFieldMappingsResponse.mappings().values()) {
                Map<String, FieldMappingMetaData> watchRecord = map.get("watch_record");
                assertThat(watchRecord, hasKey("result.actions.transform.payload"));
                FieldMappingMetaData fieldMappingMetaData = watchRecord.get("result.actions.transform.payload");
                assertThat(fieldMappingMetaData.isNull(), is(true));
            }
        });
    }
}
