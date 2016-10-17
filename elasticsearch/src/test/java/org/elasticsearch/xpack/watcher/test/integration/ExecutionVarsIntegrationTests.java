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

package org.elasticsearch.xpack.watcher.test.integration;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.script.MockScriptPlugin;
import org.elasticsearch.script.Script;
import org.elasticsearch.xpack.XPackPlugin;
import org.elasticsearch.xpack.watcher.client.WatcherClient;
import org.elasticsearch.xpack.watcher.condition.script.ScriptCondition;
import org.elasticsearch.xpack.watcher.support.xcontent.ObjectPath;
import org.elasticsearch.xpack.watcher.support.xcontent.XContentSource;
import org.elasticsearch.xpack.watcher.test.AbstractWatcherIntegrationTestCase;
import org.elasticsearch.xpack.watcher.transport.actions.execute.ExecuteWatchResponse;
import org.elasticsearch.xpack.watcher.transport.actions.put.PutWatchResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.elasticsearch.xpack.watcher.actions.ActionBuilders.loggingAction;
import static org.elasticsearch.xpack.watcher.client.WatchSourceBuilders.watchBuilder;
import static org.elasticsearch.xpack.watcher.input.InputBuilders.simpleInput;
import static org.elasticsearch.xpack.watcher.transform.TransformBuilders.scriptTransform;
import static org.elasticsearch.xpack.watcher.trigger.TriggerBuilders.schedule;
import static org.elasticsearch.xpack.watcher.trigger.schedule.Schedules.cron;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ExecutionVarsIntegrationTests extends AbstractWatcherIntegrationTestCase {

    @Override
    protected boolean timeWarped() {
        return true;
    }

    @Override
    protected List<Class<? extends Plugin>> pluginTypes() {
        List<Class<? extends Plugin>> types = super.pluginTypes();
        types.add(CustomScriptPlugin.class);
        return types;
    }

    public static class CustomScriptPlugin extends MockScriptPlugin {

        @Override
        @SuppressWarnings("unchecked")
        protected Map<String, Function<Map<String, Object>, Object>> pluginScripts() {
            Map<String, Function<Map<String, Object>, Object>> scripts = new HashMap<>();

            scripts.put("ctx.vars.condition_value = ctx.payload.value + 5; return ctx.vars.condition_value > 5;", vars -> {
                int value = (int) XContentMapValues.extractValue("ctx.payload.value", vars);

                Map<String, Object> ctxVars = (Map<String, Object>) XContentMapValues.extractValue("ctx.vars", vars);
                ctxVars.put("condition_value", value + 5);

                return (int) XContentMapValues.extractValue("condition_value", ctxVars) > 5;
            });

            scripts.put("ctx.vars.watch_transform_value = ctx.vars.condition_value + 5; return ctx.payload;", vars -> {
                Map<String, Object> ctxVars = (Map<String, Object>) XContentMapValues.extractValue("ctx.vars", vars);
                ctxVars.put("watch_transform_value", (int) XContentMapValues.extractValue("condition_value", ctxVars) + 5);

                return XContentMapValues.extractValue("ctx.payload", vars);
            });

            // Transforms the value of a1, equivalent to:
            //      ctx.vars.a1_transform_value = ctx.vars.watch_transform_value + 10;
            //      ctx.payload.a1_transformed_value = ctx.vars.a1_transform_value;
            //      return ctx.payload;
            scripts.put("transform a1", vars -> {
                Map<String, Object> ctxVars = (Map<String, Object>) XContentMapValues.extractValue("ctx.vars", vars);
                Map<String, Object> ctxPayload = (Map<String, Object>) XContentMapValues.extractValue("ctx.payload", vars);

                int value = (int) XContentMapValues.extractValue("watch_transform_value", ctxVars);
                ctxVars.put("a1_transform_value", value + 10);

                value = (int) XContentMapValues.extractValue("a1_transform_value", ctxVars);
                ctxPayload.put("a1_transformed_value", value);

                return XContentMapValues.extractValue("ctx.payload", vars);
            });

            // Transforms the value of a2, equivalent to:
            //      ctx.vars.a2_transform_value = ctx.vars.watch_transform_value + 20;
            //      ctx.payload.a2_transformed_value = ctx.vars.a2_transform_value;
            //      return ctx.payload;
            scripts.put("transform a2", vars -> {
                Map<String, Object> ctxVars = (Map<String, Object>) XContentMapValues.extractValue("ctx.vars", vars);
                Map<String, Object> ctxPayload = (Map<String, Object>) XContentMapValues.extractValue("ctx.payload", vars);

                int value = (int) XContentMapValues.extractValue("watch_transform_value", ctxVars);
                ctxVars.put("a2_transform_value", value + 20);

                value = (int) XContentMapValues.extractValue("a2_transform_value", ctxVars);
                ctxPayload.put("a2_transformed_value", value);

                return XContentMapValues.extractValue("ctx.payload", vars);
            });

            return scripts;
        }

        @Override
        public String pluginScriptLang() {
            return WATCHER_LANG;
        }
    }

    public void testVars() throws Exception {
        WatcherClient watcherClient = watcherClient();

        PutWatchResponse putWatchResponse = watcherClient.preparePutWatch("_id").setSource(watchBuilder()
                .trigger(schedule(cron("0/1 * * * * ?")))
                .input(simpleInput("value", 5))
                .condition(new ScriptCondition(
                        new Script("ctx.vars.condition_value = ctx.payload.value + 5; return ctx.vars.condition_value > 5;")))
                .transform(scriptTransform("ctx.vars.watch_transform_value = ctx.vars.condition_value + 5; return ctx.payload;"))
                .addAction(
                        "a1",
                        scriptTransform("transform a1"),
                        loggingAction("_text"))
                .addAction(
                        "a2",
                        scriptTransform("transform a2"),
                        loggingAction("_text")))
                .get();

        assertThat(putWatchResponse.isCreated(), is(true));

        timeWarp().scheduler().trigger("_id");

        flush();
        refresh();

        SearchResponse searchResponse = searchWatchRecords(builder -> {
            // defaults to match all;
        });

        assertThat(searchResponse.getHits().getTotalHits(), is(1L));

        Map<String, Object> source = searchResponse.getHits().getAt(0).getSource();

        assertValue(source, "watch_id", is("_id"));
        assertValue(source, "state", is("executed"));

        // we don't store the computed vars in history
        assertValue(source, "vars", nullValue());

        assertValue(source, "result.condition.status", is("success"));
        assertValue(source, "result.transform.status", is("success"));

        List<Map<String, Object>> actions = ObjectPath.eval("result.actions", source);
        for (Map<String, Object> action : actions) {
            String id = (String) action.get("id");
            switch (id) {
                case "a1":
                    assertValue(action, "status", is("success"));
                    assertValue(action, "transform.status", is("success"));
                    assertValue(action, "transform.payload.a1_transformed_value", equalTo(25));
                    break;
                case "a2":
                    assertValue(action, "status", is("success"));
                    assertValue(action, "transform.status", is("success"));
                    assertValue(action, "transform.payload.a2_transformed_value", equalTo(35));
                    break;
                default:
                    fail("there should not be an action result for action with an id other than a1 or a2");
            }
        }
    }

    public void testVarsManual() throws Exception {
        WatcherClient watcherClient = watcherClient();

        PutWatchResponse putWatchResponse = watcherClient.preparePutWatch("_id").setSource(watchBuilder()
                .trigger(schedule(cron("0/1 * * * * ? 2020")))
                .input(simpleInput("value", 5))
                .condition(new ScriptCondition(
                        new Script("ctx.vars.condition_value = ctx.payload.value + 5; return ctx.vars.condition_value > 5;")))
                .transform(scriptTransform("ctx.vars.watch_transform_value = ctx.vars.condition_value + 5; return ctx.payload;"))
                .addAction(
                        "a1",
                        scriptTransform("transform a1"),
                        loggingAction("_text"))
                .addAction(
                        "a2",
                        scriptTransform("transform a2"),
                        loggingAction("_text")))
                .get();

        assertThat(putWatchResponse.isCreated(), is(true));

        boolean debug = randomBoolean();

        ExecuteWatchResponse executeWatchResponse = watcherClient
                .prepareExecuteWatch("_id")
                .setDebug(debug)
                .get();
        assertThat(executeWatchResponse.getRecordId(), notNullValue());
        XContentSource source = executeWatchResponse.getRecordSource();

        assertValue(source, "watch_id", is("_id"));
        assertValue(source, "state", is("executed"));

        if (debug) {
            assertValue(source, "vars.condition_value", is(10));
            assertValue(source, "vars.watch_transform_value", is(15));
            assertValue(source, "vars.a1_transform_value", is(25));
            assertValue(source, "vars.a2_transform_value", is(35));
        }

        assertValue(source, "result.condition.status", is("success"));
        assertValue(source, "result.transform.status", is("success"));

        List<Map<String, Object>> actions = source.getValue("result.actions");
        for (Map<String, Object> action : actions) {
            String id = (String) action.get("id");
            switch (id) {
                case "a1":
                    assertValue(action, "status", is("success"));
                    assertValue(action, "transform.status", is("success"));
                    assertValue(action, "transform.payload.a1_transformed_value", equalTo(25));
                    break;
                case "a2":
                    assertValue(action, "status", is("success"));
                    assertValue(action, "transform.status", is("success"));
                    assertValue(action, "transform.payload.a2_transformed_value", equalTo(35));
                    break;
                default:
                    fail("there should not be an action result for action with an id other than a1 or a2");
            }
        }
    }
}
