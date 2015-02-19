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

package org.elasticsearch.alerts;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.alerts.actions.Action;
import org.elasticsearch.alerts.actions.Actions;
import org.elasticsearch.alerts.actions.email.EmailAction;
import org.elasticsearch.alerts.actions.email.service.Authentication;
import org.elasticsearch.alerts.actions.email.service.Email;
import org.elasticsearch.alerts.actions.email.service.EmailService;
import org.elasticsearch.alerts.actions.email.service.Profile;
import org.elasticsearch.alerts.actions.webhook.HttpClient;
import org.elasticsearch.alerts.actions.webhook.WebhookAction;
import org.elasticsearch.alerts.client.AlertsClient;
import org.elasticsearch.alerts.condition.script.ScriptCondition;
import org.elasticsearch.alerts.history.FiredAlert;
import org.elasticsearch.alerts.history.HistoryStore;
import org.elasticsearch.alerts.input.search.SearchInput;
import org.elasticsearch.alerts.scheduler.schedule.CronSchedule;
import org.elasticsearch.alerts.support.AlertUtils;
import org.elasticsearch.alerts.support.init.proxy.ClientProxy;
import org.elasticsearch.alerts.support.init.proxy.ScriptServiceProxy;
import org.elasticsearch.alerts.support.template.ScriptTemplate;
import org.elasticsearch.alerts.support.template.Template;
import org.elasticsearch.alerts.transform.SearchTransform;
import org.elasticsearch.alerts.transport.actions.stats.AlertsStatsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexTemplateMetaData;
import org.elasticsearch.cluster.routing.IndexRoutingTable;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.hppc.cursors.ObjectObjectCursor;
import org.elasticsearch.common.netty.handler.codec.http.HttpMethod;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.elasticsearch.test.InternalTestCluster;
import org.elasticsearch.test.TestCluster;
import org.junit.After;
import org.junit.Before;

import javax.mail.internet.AddressException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.elasticsearch.search.builder.SearchSourceBuilder.searchSource;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

/**
 */
@ElasticsearchIntegrationTest.ClusterScope(scope = ElasticsearchIntegrationTest.Scope.SUITE, numClientNodes = 0, transportClientRatio = 0, randomDynamicTemplates = false)
public abstract class AbstractAlertingTests extends ElasticsearchIntegrationTest {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return ImmutableSettings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put("scroll.size", randomIntBetween(1, 100))
                .put("plugin.types", AlertsPlugin.class.getName())
                .build();
    }

    public boolean randomizeNumberOfShardsAndReplicas() {
        return false;
    }

    @Override
    protected TestCluster buildTestCluster(Scope scope, long seed) throws IOException {
        // This overwrites the wipe logic of the test cluster to not remove the alerts and alerthistory templates. By default all templates are removed
        // TODO: We should have the notion of a hidden template (like hidden index / type) that only gets removed when specifically mentioned.
        final TestCluster testCluster = super.buildTestCluster(scope, seed);
        return new AlertingWrappingCluster(seed, testCluster);
    }

    @Before
    public void startAlertsIfNodesExist() throws Exception {
        if (internalTestCluster().size() > 0) {
            AlertsStatsResponse response = alertClient().prepareAlertsStats().get();
            if (response.getAlertManagerStarted() == AlertsService.State.STOPPED) {
                logger.info("[{}#{}]: starting alerts", getTestClass().getSimpleName(), getTestName());
                startAlerting();
            } else {
                logger.info("[{}#{}]: not starting alerts, because alerts is in state [{}]", getTestClass().getSimpleName(), getTestName(), response.getAlertManagerStarted());
            }
        } else {
            logger.info("[{}#{}]: not starting alerts, because test cluster has no nodes", getTestClass().getSimpleName(), getTestName());
        }
    }

    @After
    public void clearAlerts() throws Exception {
        // Clear all internal alerting state for the next test method:
        logger.info("[{}#{}]: clearing alerts", getTestClass().getSimpleName(), getTestName());
        stopAlerting();
    }

    protected BytesReference createAlertSource(String cron, SearchRequest conditionRequest, String conditionScript) throws IOException {
        return createAlertSource(cron, conditionRequest, conditionScript, null);
    }

    protected BytesReference createAlertSource(String cron, SearchRequest conditionRequest, String conditionScript, Map<String,Object> metadata) throws IOException {
        XContentBuilder builder = jsonBuilder();
        builder.startObject();
        {
            builder.startObject("schedule")
                    .field("cron", cron)
                    .endObject();

            if (metadata != null) {
                builder.field("meta", metadata);
            }

            builder.startObject("input");
            {
                builder.startObject("search");
                builder.field("request");
                AlertUtils.writeSearchRequest(conditionRequest, builder, ToXContent.EMPTY_PARAMS);
                builder.endObject();
            }
            builder.endObject();

            builder.startObject("condition");
            {
                builder.startObject("script");
                builder.field("script", conditionScript);
                builder.endObject();
            }
            builder.endObject();


            builder.startArray("actions");
            {
                builder.startObject();
                {
                    builder.startObject("index");
                    builder.field("index", "my-index");
                    builder.field("type", "trail");
                    builder.endObject();
                }
                builder.endObject();
            }
            builder.endArray();
        }
        builder.endObject();

        return builder.bytes();
    }

    public static SearchRequest createConditionSearchRequest(String... indices) {
        SearchRequest request = new SearchRequest(indices);
        request.indicesOptions(AlertUtils.DEFAULT_INDICES_OPTIONS);
        request.searchType(SearchInput.DEFAULT_SEARCH_TYPE);
        return request;
    }

    protected Alert createTestAlert(String alertName) throws AddressException {
        SearchRequest conditionRequest = createConditionSearchRequest("my-condition-index").source(searchSource().query(matchAllQuery()));
        SearchRequest transformRequest = createConditionSearchRequest("my-payload-index").source(searchSource().query(matchAllQuery()));
        transformRequest.searchType(SearchTransform.DEFAULT_SEARCH_TYPE);
        conditionRequest.searchType(SearchInput.DEFAULT_SEARCH_TYPE);

        List<Action> actions = new ArrayList<>();

        Template url = new ScriptTemplate(scriptService(), "http://localhost/foobarbaz/{{alert_name}}");
        Template body = new ScriptTemplate(scriptService(), "{{alert_name}} executed with {{response.hits.total}} hits");

        actions.add(new WebhookAction(logger, httpClient(), HttpMethod.GET, url, body));

        Email.Address from = new Email.Address("from@test.com");
        List<Email.Address> emailAddressList = new ArrayList<>();
        emailAddressList.add(new Email.Address("to@test.com"));
        Email.AddressList to = new Email.AddressList(emailAddressList);


        Email.Builder emailBuilder = Email.builder().id("prototype");
        emailBuilder.from(from);
        emailBuilder.to(to);


        EmailAction emailAction = new EmailAction(logger, noopEmailService(), emailBuilder.build(),
                new Authentication("testname", "testpassword"), Profile.STANDARD, "testaccount", body, body, null, true);

        actions.add(emailAction);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("foo", "bar");

        return new Alert(
                alertName,
                new CronSchedule("0/5 * * * * ? *"),
                new SearchInput(logger, scriptService(), ClientProxy.of(client()),
                        conditionRequest),
                new ScriptCondition(logger, scriptService(), "return true", ScriptService.ScriptType.INLINE, "groovy"),
                new SearchTransform(logger, scriptService(), ClientProxy.of(client()), transformRequest), new Actions(actions), metadata, new Alert.Status(), new TimeValue(0)
        );
    }


    protected AlertsClient alertClient() {
        return internalTestCluster().getInstance(AlertsClient.class);
    }

    protected ScriptServiceProxy scriptService() {
        return internalTestCluster().getInstance(ScriptServiceProxy.class);
    }

    protected Template.Parser templateParser() {
        return internalTestCluster().getInstance(Template.Parser.class);
    }

    protected HttpClient httpClient() {
        return internalTestCluster().getInstance(HttpClient.class);
    }

    protected EmailService noopEmailService() {
        return new NoopEmailService();
    }

    protected FiredAlert.Parser firedAlertParser() {
        return internalTestCluster().getInstance(FiredAlert.Parser.class);
    }

    protected void assertAlertWithExactPerformedActionsCount(final String alertName, final long expectedAlertActionsWithActionPerformed) throws Exception {
        assertBusy(new Runnable() {
            @Override
            public void run() {
                ClusterState state = client().admin().cluster().prepareState().get().getState();
                String[] alertHistoryIndices = state.metaData().concreteIndices(IndicesOptions.lenientExpandOpen(), HistoryStore.ALERT_HISTORY_INDEX_PREFIX + "*");
                assertThat(alertHistoryIndices, not(emptyArray()));
                for (String index : alertHistoryIndices) {
                    IndexRoutingTable routingTable = state.getRoutingTable().index(index);
                    assertThat(routingTable, notNullValue());
                    assertThat(routingTable.allPrimaryShardsActive(), is(true));
                }

                assertThat(findNumberOfPerformedActions(alertName), equalTo(expectedAlertActionsWithActionPerformed));
            }
        });
    }

    protected void assertAlertWithMinimumPerformedActionsCount(final String alertName, final long minimumExpectedAlertActionsWithActionPerformed) throws Exception {
        assertAlertWithMinimumPerformedActionsCount(alertName, minimumExpectedAlertActionsWithActionPerformed, true);
    }

    protected void assertAlertWithMinimumPerformedActionsCount(final String alertName, final long minimumExpectedAlertActionsWithActionPerformed, final boolean assertConditionMet) throws Exception {
        assertBusy(new Runnable() {
            @Override
            public void run() {
                ClusterState state = client().admin().cluster().prepareState().get().getState();
                String[] alertHistoryIndices = state.metaData().concreteIndices(IndicesOptions.lenientExpandOpen(), HistoryStore.ALERT_HISTORY_INDEX_PREFIX + "*");
                assertThat(alertHistoryIndices, not(emptyArray()));
                for (String index : alertHistoryIndices) {
                    IndexRoutingTable routingTable = state.getRoutingTable().index(index);
                    assertThat(routingTable, notNullValue());
                    assertThat(routingTable.allPrimaryShardsActive(), is(true));
                }

                SearchResponse searchResponse = client().prepareSearch(HistoryStore.ALERT_HISTORY_INDEX_PREFIX + "*")
                        .setIndicesOptions(IndicesOptions.lenientExpandOpen())
                        .setQuery(boolQuery().must(matchQuery("alert_name", alertName)).must(matchQuery("state", FiredAlert.State.EXECUTED.id())))
                        .get();
                assertThat(searchResponse.getHits().getTotalHits(), greaterThanOrEqualTo(minimumExpectedAlertActionsWithActionPerformed));
                if (assertConditionMet) {
                    assertThat((Integer) XContentMapValues.extractValue("alert_execution.input_result.search.payload.hits.total", searchResponse.getHits().getAt(0).sourceAsMap()), greaterThanOrEqualTo(1));
                }
            }
        });
    }

    protected long findNumberOfPerformedActions(String alertName) {
        SearchResponse searchResponse = client().prepareSearch(HistoryStore.ALERT_HISTORY_INDEX_PREFIX + "*")
                .setIndicesOptions(IndicesOptions.lenientExpandOpen())
                .setQuery(boolQuery().must(matchQuery("alert_name", alertName)).must(matchQuery("state", FiredAlert.State.EXECUTED.id())))
                .get();
        return searchResponse.getHits().getTotalHits();
    }

    protected void assertAlertWithNoActionNeeded(final String alertName, final long expectedAlertActionsWithNoActionNeeded) throws Exception {
        assertBusy(new Runnable() {
            @Override
            public void run() {
                // The alerthistory index gets created in the background when the first alert fires, so we to check first is this index is created and shards are started
                ClusterState state = client().admin().cluster().prepareState().get().getState();
                String[] alertHistoryIndices = state.metaData().concreteIndices(IndicesOptions.lenientExpandOpen(), HistoryStore.ALERT_HISTORY_INDEX_PREFIX + "*");
                assertThat(alertHistoryIndices, not(emptyArray()));
                for (String index : alertHistoryIndices) {
                    IndexRoutingTable routingTable = state.getRoutingTable().index(index);
                    assertThat(routingTable, notNullValue());
                    assertThat(routingTable.allPrimaryShardsActive(), is(true));
                }

                SearchResponse searchResponse = client().prepareSearch(HistoryStore.ALERT_HISTORY_INDEX_PREFIX + "*")
                        .setIndicesOptions(IndicesOptions.lenientExpandOpen())
                        .setQuery(boolQuery().must(matchQuery("alert_name", alertName)).must(matchQuery("state", FiredAlert.State.EXECUTION_NOT_NEEDED.id())))
                        .get();
                assertThat(searchResponse.getHits().getTotalHits(), greaterThanOrEqualTo(expectedAlertActionsWithNoActionNeeded));
            }
        });
    }

    protected void ensureAlertingStarted() throws Exception {
        assertBusy(new Runnable() {
            @Override
            public void run() {
                assertThat(alertClient().prepareAlertsStats().get().getAlertManagerStarted(), is(AlertsService.State.STARTED));
            }
        });
    }

    protected void ensureAlertingStopped() throws Exception {
        assertBusy(new Runnable() {
            @Override
            public void run() {
                assertThat(alertClient().prepareAlertsStats().get().getAlertManagerStarted(), is(AlertsService.State.STOPPED));
            }
        });
    }

    protected void startAlerting() throws Exception {
        alertClient().prepareAlertService().start().get();
        ensureAlertingStarted();
    }

    protected void stopAlerting() throws Exception {
        alertClient().prepareAlertService().stop().get();
        ensureAlertingStopped();
    }

    protected static InternalTestCluster internalTestCluster() {
        return (InternalTestCluster) ((AlertingWrappingCluster) cluster()).testCluster;
    }

    private final class AlertingWrappingCluster extends TestCluster {

        private final TestCluster testCluster;

        private AlertingWrappingCluster(long seed, TestCluster testCluster) {
            super(seed);
            this.testCluster = testCluster;
        }

        @Override
        public void beforeTest(Random random, double transportClientRatio) throws IOException {
            testCluster.beforeTest(random, transportClientRatio);
        }

        @Override
        public void wipe() {
            wipeIndices("_all");
            wipeRepositories();

            if (size() > 0) {
                List<String> templatesToWipe = new ArrayList<>();
                ClusterState state = client().admin().cluster().prepareState().get().getState();
                for (ObjectObjectCursor<String, IndexTemplateMetaData> cursor : state.getMetaData().templates()) {
                    if (cursor.key.equals("alerts") || cursor.key.equals("alerthistory")) {
                        continue;
                    }
                    templatesToWipe.add(cursor.key);
                }
                if (!templatesToWipe.isEmpty()) {
                    wipeTemplates(templatesToWipe.toArray(new String[templatesToWipe.size()]));
                }
            }
        }

        @Override
        public void afterTest() throws IOException {
            testCluster.afterTest();
        }

        @Override
        public Client client() {
            return testCluster.client();
        }

        @Override
        public int size() {
            return testCluster.size();
        }

        @Override
        public int numDataNodes() {
            return testCluster.numDataNodes();
        }

        @Override
        public int numDataAndMasterNodes() {
            return testCluster.numDataAndMasterNodes();
        }

        @Override
        public int numBenchNodes() {
            return testCluster.numBenchNodes();
        }

        @Override
        public InetSocketAddress[] httpAddresses() {
            return testCluster.httpAddresses();
        }

        @Override
        public void close() throws IOException {
            InternalTestCluster _testCluster = (InternalTestCluster) testCluster;
            Set<String> nodes = new HashSet<>(Arrays.asList(_testCluster.getNodeNames()));
            String masterNode = _testCluster.getMasterName();
            nodes.remove(masterNode);

            // First manually stop alerting on non elected master node, this will prevent that alerting becomes active
            // on these nodes
            for (String node : nodes) {
                AlertsService alertsService = _testCluster.getInstance(AlertsService.class, node);
                assertThat(alertsService.state(), equalTo(AlertsService.State.STOPPED));
                alertsService.stop(); // Prevents these nodes from starting alerting when new elected master node is picked.
            }

            // Then stop alerting on elected master node and wait until alerting has stopped on it.
            final AlertsService alertsService = _testCluster.getInstance(AlertsService.class, masterNode);
            try {
                assertBusy(new Runnable() {
                    @Override
                    public void run() {
                        assertThat(alertsService.state(), not(equalTo(AlertsService.State.STARTING)));
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            alertsService.stop();
            try {
                assertBusy(new Runnable() {
                    @Override
                    public void run() {
                        assertThat(alertsService.state(), equalTo(AlertsService.State.STOPPED));
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // Now when can close nodes, without alerting trying to become active while nodes briefly become master
            // during cluster shutdown.
            testCluster.close();
        }

        @Override
        public void ensureEstimatedStats() {
            testCluster.ensureEstimatedStats();
        }

        @Override
        public boolean hasFilterCache() {
            return testCluster.hasFilterCache();
        }

        @Override
        public String getClusterName() {
            return testCluster.getClusterName();
        }

        @Override
        public Iterator<Client> iterator() {
            return testCluster.iterator();
        }

    }

    private static class NoopEmailService implements EmailService {

        @Override
        public EmailSent send(Email email, Authentication auth, Profile profile) {
            return new EmailSent(auth.user(), email);
        }

        @Override
        public EmailSent send(Email email, Authentication auth, Profile profile, String accountName) {
            return new EmailSent(accountName, email);
        }
    }

}
