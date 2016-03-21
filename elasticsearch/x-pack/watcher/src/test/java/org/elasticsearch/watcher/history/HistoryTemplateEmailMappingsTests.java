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

package org.elasticsearch.watcher.history;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.watcher.actions.email.service.EmailTemplate;
import org.elasticsearch.watcher.actions.email.service.support.EmailServer;
import org.elasticsearch.watcher.execution.ExecutionState;
import org.elasticsearch.watcher.test.AbstractWatcherIntegrationTestCase;
import org.elasticsearch.watcher.transport.actions.put.PutWatchResponse;
import org.junit.After;

import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;
import static org.elasticsearch.search.builder.SearchSourceBuilder.searchSource;
import static org.elasticsearch.watcher.actions.ActionBuilders.emailAction;
import static org.elasticsearch.watcher.client.WatchSourceBuilders.watchBuilder;
import static org.elasticsearch.watcher.condition.ConditionBuilders.alwaysCondition;
import static org.elasticsearch.watcher.input.InputBuilders.simpleInput;
import static org.elasticsearch.watcher.trigger.TriggerBuilders.schedule;
import static org.elasticsearch.watcher.trigger.schedule.Schedules.interval;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * This test makes sure that the email address fields in the watch_record action result are
 * not analyzed so they can be used in aggregations
 */
public class HistoryTemplateEmailMappingsTests extends AbstractWatcherIntegrationTestCase {
    static final String USERNAME = "_user";
    static final String PASSWORD = "_passwd";

    private EmailServer server;

    @After
    public void cleanup() throws Exception {
        server.stop();
    }

    @Override
    protected boolean timeWarped() {
        return true; // just to have better control over the triggers
    }

    @Override
    protected boolean enableShield() {
        return false; // remove shield noise from this test
    }

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        if(server == null) {
            //Need to construct the Email Server here as this happens before init()
            server = EmailServer.localhost("2500-2600", USERNAME, PASSWORD, logger);
        }
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))

                // email
                .put("watcher.actions.email.service.account.test.smtp.auth", true)
                .put("watcher.actions.email.service.account.test.smtp.user", USERNAME)
                .put("watcher.actions.email.service.account.test.smtp.password", PASSWORD)
                .put("watcher.actions.email.service.account.test.smtp.port", server.port())
                .put("watcher.actions.email.service.account.test.smtp.host", "localhost")

                .build();
    }

    public void testEmailFields() throws Exception {
        PutWatchResponse putWatchResponse = watcherClient().preparePutWatch("_id").setSource(watchBuilder()
                .trigger(schedule(interval("5s")))
                .input(simpleInput())
                .condition(alwaysCondition())
                .addAction("_email", emailAction(EmailTemplate.builder()
                        .from("from@example.com")
                        .to("to1@example.com", "to2@example.com")
                        .cc("cc1@example.com", "cc2@example.com")
                        .bcc("bcc1@example.com", "bcc2@example.com")
                        .replyTo("rt1@example.com", "rt2@example.com")
                        .subject("_subject")
                        .textBody("_body"))))
                .get();

        assertThat(putWatchResponse.isCreated(), is(true));
        timeWarp().scheduler().trigger("_id");
        flush();
        refresh();

        // the action should fail as no email server is available
        assertWatchWithMinimumActionsCount("_id", ExecutionState.EXECUTED, 1);

        SearchResponse response = client().prepareSearch(HistoryStore.INDEX_PREFIX + "*").setSource(searchSource()
                .aggregation(terms("from").field("result.actions.email.message.from"))
                .aggregation(terms("to").field("result.actions.email.message.to"))
                .aggregation(terms("cc").field("result.actions.email.message.cc"))
                .aggregation(terms("bcc").field("result.actions.email.message.bcc"))
                .aggregation(terms("reply_to").field("result.actions.email.message.reply_to")))
                .get();

        assertThat(response, notNullValue());
        assertThat(response.getHits().getTotalHits(), is(1L));
        Aggregations aggs = response.getAggregations();
        assertThat(aggs, notNullValue());

        Terms terms = aggs.get("from");
        assertThat(terms, notNullValue());
        assertThat(terms.getBuckets().size(), is(1));
        assertThat(terms.getBucketByKey("from@example.com"), notNullValue());
        assertThat(terms.getBucketByKey("from@example.com").getDocCount(), is(1L));

        terms = aggs.get("to");
        assertThat(terms, notNullValue());
        assertThat(terms.getBuckets().size(), is(2));
        assertThat(terms.getBucketByKey("to1@example.com"), notNullValue());
        assertThat(terms.getBucketByKey("to1@example.com").getDocCount(), is(1L));
        assertThat(terms.getBucketByKey("to2@example.com"), notNullValue());
        assertThat(terms.getBucketByKey("to2@example.com").getDocCount(), is(1L));

        terms = aggs.get("cc");
        assertThat(terms, notNullValue());
        assertThat(terms.getBuckets().size(), is(2));
        assertThat(terms.getBucketByKey("cc1@example.com"), notNullValue());
        assertThat(terms.getBucketByKey("cc1@example.com").getDocCount(), is(1L));
        assertThat(terms.getBucketByKey("cc2@example.com"), notNullValue());
        assertThat(terms.getBucketByKey("cc2@example.com").getDocCount(), is(1L));

        terms = aggs.get("bcc");
        assertThat(terms, notNullValue());
        assertThat(terms.getBuckets().size(), is(2));
        assertThat(terms.getBucketByKey("bcc1@example.com"), notNullValue());
        assertThat(terms.getBucketByKey("bcc1@example.com").getDocCount(), is(1L));
        assertThat(terms.getBucketByKey("bcc2@example.com"), notNullValue());
        assertThat(terms.getBucketByKey("bcc2@example.com").getDocCount(), is(1L));

        terms = aggs.get("reply_to");
        assertThat(terms, notNullValue());
        assertThat(terms.getBuckets().size(), is(2));
        assertThat(terms.getBucketByKey("rt1@example.com"), notNullValue());
        assertThat(terms.getBucketByKey("rt1@example.com").getDocCount(), is(1L));
        assertThat(terms.getBucketByKey("rt2@example.com"), notNullValue());
        assertThat(terms.getBucketByKey("rt2@example.com").getDocCount(), is(1L));
    }
}
