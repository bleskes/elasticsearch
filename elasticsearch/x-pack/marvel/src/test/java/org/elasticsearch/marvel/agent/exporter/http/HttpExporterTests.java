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

package org.elasticsearch.marvel.agent.exporter.http;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.QueueDispatcher;
import com.squareup.okhttp.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.Version;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.admin.indices.recovery.RecoveryResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.marvel.agent.collector.cluster.ClusterStateCollector;
import org.elasticsearch.marvel.agent.collector.cluster.ClusterStateMarvelDoc;
import org.elasticsearch.marvel.agent.collector.indices.IndexRecoveryCollector;
import org.elasticsearch.marvel.agent.collector.indices.IndexRecoveryMarvelDoc;
import org.elasticsearch.marvel.agent.exporter.Exporters;
import org.elasticsearch.marvel.agent.exporter.MarvelDoc;
import org.elasticsearch.marvel.agent.exporter.MarvelTemplateUtils;
import org.elasticsearch.marvel.agent.settings.MarvelSettings;
import org.elasticsearch.marvel.test.MarvelIntegTestCase;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.ESIntegTestCase.Scope;
import org.hamcrest.Matchers;
import org.joda.time.format.DateTimeFormat;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.net.BindException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.marvel.agent.exporter.MarvelTemplateUtils.dataTemplateName;
import static org.elasticsearch.marvel.agent.exporter.MarvelTemplateUtils.indexTemplateName;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

@ESIntegTestCase.ClusterScope(scope = Scope.TEST, numDataNodes = 0, numClientNodes = 0, transportClientRatio = 0.0)
public class HttpExporterTests extends MarvelIntegTestCase {

    private int webPort;
    private MockWebServer webServer;

    private static final byte[] TIMESTAMPED_TEMPLATE = MarvelTemplateUtils.loadTimestampedIndexTemplate();
    private static final byte[] DATA_TEMPLATE = MarvelTemplateUtils.loadDataIndexTemplate();

    @Before
    public void startWebservice() throws Exception {
        for (webPort = 9250; webPort < 9300; webPort++) {
            try {
                webServer = new MockWebServer();
                QueueDispatcher dispatcher = new QueueDispatcher();
                dispatcher.setFailFast(true);
                webServer.setDispatcher(dispatcher);
                webServer.start(webPort);
                return;
            } catch (BindException be) {
                logger.warn("port [{}] was already in use trying next port", webPort);
            }
        }
        throw new ElasticsearchException("unable to find open port between 9200 and 9300");
    }

    @After
    public void cleanup() throws Exception {
        stopCollection();
        webServer.shutdown();
    }

    public void testExport() throws Exception {
        enqueueGetClusterVersionResponse(Version.CURRENT);
        enqueueResponse(404, "template for timestamped indices does not exist");
        enqueueResponse(201, "template for timestamped indices created");
        enqueueResponse(404, "template for data index does not exist");
        enqueueResponse(201, "template for data index created");
        enqueueResponse(200, "successful bulk request ");

        Settings.Builder builder = Settings.builder()
                .put(MarvelSettings.INTERVAL_SETTING.getKey(), "-1")
                .put("marvel.agent.exporters._http.type", "http")
                .put("marvel.agent.exporters._http.host", webServer.getHostName() + ":" + webServer.getPort())
                .put("marvel.agent.exporters._http.connection.keep_alive", false)
                .put("marvel.agent.exporters._http.update_mappings", false);

        String agentNode = internalCluster().startNode(builder);
        HttpExporter exporter = getExporter(agentNode);

        final int nbDocs = randomIntBetween(1, 25);
        exporter.export(newRandomMarvelDocs(nbDocs));

        assertThat(webServer.getRequestCount(), equalTo(6));

        RecordedRequest recordedRequest = webServer.takeRequest();
        assertThat(recordedRequest.getMethod(), equalTo("GET"));
        assertThat(recordedRequest.getPath(), equalTo("/"));

        recordedRequest = webServer.takeRequest();
        assertThat(recordedRequest.getMethod(), equalTo("GET"));
        assertThat(recordedRequest.getPath(), equalTo("/_template/" + indexTemplateName()));

        recordedRequest = webServer.takeRequest();
        assertThat(recordedRequest.getMethod(), equalTo("PUT"));
        assertThat(recordedRequest.getPath(), equalTo("/_template/" + indexTemplateName()));
        assertThat(recordedRequest.getBody().readByteArray(), equalTo(TIMESTAMPED_TEMPLATE));

        recordedRequest = webServer.takeRequest();
        assertThat(recordedRequest.getMethod(), equalTo("GET"));
        assertThat(recordedRequest.getPath(), equalTo("/_template/" + dataTemplateName()));

        recordedRequest = webServer.takeRequest();
        assertThat(recordedRequest.getMethod(), equalTo("PUT"));
        assertThat(recordedRequest.getPath(), equalTo("/_template/" + dataTemplateName()));
        assertThat(recordedRequest.getBody().readByteArray(), equalTo(DATA_TEMPLATE));

        recordedRequest = webServer.takeRequest();
        assertThat(recordedRequest.getMethod(), equalTo("POST"));
        assertThat(recordedRequest.getPath(), equalTo("/_bulk"));

        assertBulkRequest(recordedRequest.getBody(), nbDocs);
    }

    public void testDynamicHostChange() {
        // disable exporting to be able to use non valid hosts
        Settings.Builder builder = Settings.builder()
                .put(MarvelSettings.INTERVAL_SETTING.getKey(), "-1")
                .put("marvel.agent.exporters._http.type", "http")
                .put("marvel.agent.exporters._http.host", "test0");

        String nodeName = internalCluster().startNode(builder);

        assertAcked(client().admin().cluster().prepareUpdateSettings().setTransientSettings(Settings.builder()
                .putArray("marvel.agent.exporters._http.host", "test1")));
        assertThat(getExporter(nodeName).hosts, Matchers.arrayContaining("test1"));

        // wipes the non array settings
        assertAcked(client().admin().cluster().prepareUpdateSettings().setTransientSettings(Settings.builder()
                .putArray("marvel.agent.exporters._http.host", "test2")
                .put("marvel.agent.exporters._http.host", "")));
        assertThat(getExporter(nodeName).hosts, Matchers.arrayContaining("test2"));

        assertAcked(client().admin().cluster().prepareUpdateSettings().setTransientSettings(Settings.builder()
                .putArray("marvel.agent.exporters._http.host", "test3")));
        assertThat(getExporter(nodeName).hosts, Matchers.arrayContaining("test3"));
    }

    public void testHostChangeReChecksTemplate() throws Exception {

        Settings.Builder builder = Settings.builder()
                .put(MarvelSettings.INTERVAL_SETTING.getKey(), "-1")
                .put("marvel.agent.exporters._http.type", "http")
                .put("marvel.agent.exporters._http.host", webServer.getHostName() + ":" + webServer.getPort())
                .put("marvel.agent.exporters._http.connection.keep_alive", false)
                .put("marvel.agent.exporters._http.update_mappings", false);

        logger.info("--> starting node");

        enqueueGetClusterVersionResponse(Version.CURRENT);
        enqueueResponse(404, "template for timestamped indices does not exist");
        enqueueResponse(201, "template for timestamped indices created");
        enqueueResponse(404, "template for data index does not exist");
        enqueueResponse(201, "template for data index created");
        enqueueResponse(200, "successful bulk request ");

        String agentNode = internalCluster().startNode(builder);

        logger.info("--> exporting data");
        HttpExporter exporter = getExporter(agentNode);
        assertThat(exporter.supportedClusterVersion, is(false));
        exporter.export(Collections.singletonList(newRandomMarvelDoc()));

        assertThat(exporter.supportedClusterVersion, is(true));
        assertThat(webServer.getRequestCount(), equalTo(6));

        RecordedRequest recordedRequest = webServer.takeRequest();
        assertThat(recordedRequest.getMethod(), equalTo("GET"));
        assertThat(recordedRequest.getPath(), equalTo("/"));

        recordedRequest = webServer.takeRequest();
        assertThat(recordedRequest.getMethod(), equalTo("GET"));
        assertThat(recordedRequest.getPath(), equalTo("/_template/" + indexTemplateName()));

        recordedRequest = webServer.takeRequest();
        assertThat(recordedRequest.getMethod(), equalTo("PUT"));
        assertThat(recordedRequest.getPath(), equalTo("/_template/" + indexTemplateName()));
        assertThat(recordedRequest.getBody().readByteArray(), equalTo(TIMESTAMPED_TEMPLATE));

        recordedRequest = webServer.takeRequest();
        assertThat(recordedRequest.getMethod(), equalTo("GET"));
        assertThat(recordedRequest.getPath(), equalTo("/_template/" + dataTemplateName()));

        recordedRequest = webServer.takeRequest();
        assertThat(recordedRequest.getMethod(), equalTo("PUT"));
        assertThat(recordedRequest.getPath(), equalTo("/_template/" + dataTemplateName()));
        assertThat(recordedRequest.getBody().readByteArray(), equalTo(DATA_TEMPLATE));

        recordedRequest = webServer.takeRequest();
        assertThat(recordedRequest.getMethod(), equalTo("POST"));
        assertThat(recordedRequest.getPath(), equalTo("/_bulk"));

        logger.info("--> setting up another web server");
        MockWebServer secondWebServer = null;
        int secondWebPort;

        try {
            for (secondWebPort = 9250; secondWebPort < 9300; secondWebPort++) {
                try {
                    secondWebServer = new MockWebServer();
                    QueueDispatcher dispatcher = new QueueDispatcher();
                    dispatcher.setFailFast(true);
                    secondWebServer.setDispatcher(dispatcher);
                    secondWebServer.start(secondWebPort);
                    break;
                } catch (BindException be) {
                    logger.warn("port [{}] was already in use trying next port", secondWebPort);
                }
            }

            assertNotNull("Unable to start the second mock web server", secondWebServer);

            assertAcked(client().admin().cluster().prepareUpdateSettings().setTransientSettings(
                    Settings.builder().putArray("marvel.agent.exporters._http.host", secondWebServer.getHostName() + ":" + secondWebServer.getPort())).get());

            // a new exporter is created on update, so we need to re-fetch it
            exporter = getExporter(agentNode);

            enqueueGetClusterVersionResponse(secondWebServer, Version.CURRENT);
            enqueueResponse(secondWebServer, 404, "template for timestamped indices does not exist");
            enqueueResponse(secondWebServer, 201, "template for timestamped indices created");
            enqueueResponse(secondWebServer, 200, "template for data index exist");
            enqueueResponse(secondWebServer, 200, "successful bulk request ");

            logger.info("--> exporting a second event");
            exporter.export(Collections.singletonList(newRandomMarvelDoc()));

            assertThat(secondWebServer.getRequestCount(), equalTo(5));

            recordedRequest = secondWebServer.takeRequest();
            assertThat(recordedRequest.getMethod(), equalTo("GET"));
            assertThat(recordedRequest.getPath(), equalTo("/"));

            recordedRequest = secondWebServer.takeRequest();
            assertThat(recordedRequest.getMethod(), equalTo("GET"));
            assertThat(recordedRequest.getPath(), equalTo("/_template/" + indexTemplateName()));

            recordedRequest = secondWebServer.takeRequest();
            assertThat(recordedRequest.getMethod(), equalTo("PUT"));
            assertThat(recordedRequest.getPath(), equalTo("/_template/" + indexTemplateName()));
            assertThat(recordedRequest.getBody().readByteArray(), equalTo(TIMESTAMPED_TEMPLATE));

            recordedRequest = secondWebServer.takeRequest();
            assertThat(recordedRequest.getMethod(), equalTo("GET"));
            assertThat(recordedRequest.getPath(), equalTo("/_template/" + dataTemplateName()));;

            recordedRequest = secondWebServer.takeRequest();
            assertThat(recordedRequest.getMethod(), equalTo("POST"));
            assertThat(recordedRequest.getPath(), equalTo("/_bulk"));

        } finally {
            if (secondWebServer != null) {
                secondWebServer.shutdown();
            }
        }
    }

    public void testUnsupportedClusterVersion() throws Exception {
        Settings.Builder builder = Settings.builder()
                .put(MarvelSettings.INTERVAL_SETTING.getKey(), "-1")
                .put("marvel.agent.exporters._http.type", "http")
                .put("marvel.agent.exporters._http.host", webServer.getHostName() + ":" + webServer.getPort())
                .put("marvel.agent.exporters._http.connection.keep_alive", false);

        logger.info("--> starting node");

        // returning an unsupported cluster version
        enqueueGetClusterVersionResponse(randomFrom(Version.V_0_18_0, Version.V_1_0_0, Version.V_1_4_0));

        String agentNode = internalCluster().startNode(builder);

        logger.info("--> exporting data");
        HttpExporter exporter = getExporter(agentNode);
        assertThat(exporter.supportedClusterVersion, is(false));
        exporter.export(Collections.singletonList(newRandomMarvelDoc()));

        assertThat(exporter.supportedClusterVersion, is(false));
        assertThat(webServer.getRequestCount(), equalTo(1));

        RecordedRequest recordedRequest = webServer.takeRequest();
        assertThat(recordedRequest.getMethod(), equalTo("GET"));
        assertThat(recordedRequest.getPath(), equalTo("/"));
    }

    public void testDynamicIndexFormatChange() throws Exception {
        Settings.Builder builder = Settings.builder()
                .put(MarvelSettings.INTERVAL_SETTING.getKey(), "-1")
                .put("marvel.agent.exporters._http.type", "http")
                .put("marvel.agent.exporters._http.host", webServer.getHostName() + ":" + webServer.getPort())
                .put("marvel.agent.exporters._http.connection.keep_alive", false)
                .put("marvel.agent.exporters._http.update_mappings", false);

        String agentNode = internalCluster().startNode(builder);

        logger.info("--> exporting a first event");

        enqueueGetClusterVersionResponse(Version.CURRENT);
        enqueueResponse(404, "template for timestamped indices does not exist");
        enqueueResponse(201, "template for timestamped indices created");
        enqueueResponse(404, "template for data index does not exist");
        enqueueResponse(201, "template for data index created");
        enqueueResponse(200, "successful bulk request ");

        HttpExporter exporter = getExporter(agentNode);

        MarvelDoc doc = newRandomMarvelDoc();
        exporter.export(Collections.singletonList(doc));

        assertThat(webServer.getRequestCount(), equalTo(6));

        RecordedRequest recordedRequest = webServer.takeRequest();
        assertThat(recordedRequest.getMethod(), equalTo("GET"));
        assertThat(recordedRequest.getPath(), equalTo("/"));

        recordedRequest = webServer.takeRequest();
        assertThat(recordedRequest.getMethod(), equalTo("GET"));
        assertThat(recordedRequest.getPath(), equalTo("/_template/" + indexTemplateName()));

        recordedRequest = webServer.takeRequest();
        assertThat(recordedRequest.getMethod(), equalTo("PUT"));
        assertThat(recordedRequest.getPath(), equalTo("/_template/" + indexTemplateName()));
        assertThat(recordedRequest.getBody().readByteArray(), equalTo(TIMESTAMPED_TEMPLATE));

        recordedRequest = webServer.takeRequest();
        assertThat(recordedRequest.getMethod(), equalTo("GET"));
        assertThat(recordedRequest.getPath(), equalTo("/_template/" + dataTemplateName()));

        recordedRequest = webServer.takeRequest();
        assertThat(recordedRequest.getMethod(), equalTo("PUT"));
        assertThat(recordedRequest.getPath(), equalTo("/_template/" + dataTemplateName()));
        assertThat(recordedRequest.getBody().readByteArray(), equalTo(DATA_TEMPLATE));

        recordedRequest = webServer.takeRequest();
        assertThat(recordedRequest.getMethod(), equalTo("POST"));
        assertThat(recordedRequest.getPath(), equalTo("/_bulk"));

        String indexName = exporter.indexNameResolver().resolve(doc);
        logger.info("--> checks that the document in the bulk request is indexed in [{}]", indexName);

        byte[] bytes = recordedRequest.getBody().readByteArray();
        Map<String, Object> data = XContentHelper.convertToMap(new BytesArray(bytes), false).v2();
        Map<String, Object> index = (Map<String, Object>) data.get("index");
        assertThat(index.get("_index"), equalTo(indexName));

        String newTimeFormat = randomFrom("YY", "YYYY", "YYYY.MM", "YYYY-MM", "MM.YYYY", "MM");
        logger.info("--> updating index time format setting to {}", newTimeFormat);
        assertAcked(client().admin().cluster().prepareUpdateSettings().setTransientSettings(Settings.builder()
                .put("marvel.agent.exporters._http.index.name.time_format", newTimeFormat)));


        logger.info("--> exporting a second event");

        enqueueGetClusterVersionResponse(Version.CURRENT);
        enqueueResponse(200, "template for timestamped indices exist");
        enqueueResponse(200, "template for data index exist");
        enqueueResponse(200, "successful bulk request ");

        doc = newRandomMarvelDoc();
        exporter = getExporter(agentNode);
        exporter.export(Collections.singletonList(doc));

        String expectedMarvelIndex = MarvelSettings.MARVEL_INDICES_PREFIX + MarvelTemplateUtils.TEMPLATE_VERSION + "-"
                + DateTimeFormat.forPattern(newTimeFormat).withZoneUTC().print(doc.getTimestamp());

        assertThat(webServer.getRequestCount(), equalTo(6 + 4));

        recordedRequest = webServer.takeRequest();
        assertThat(recordedRequest.getMethod(), equalTo("GET"));
        assertThat(recordedRequest.getPath(), equalTo("/"));

        recordedRequest = webServer.takeRequest();
        assertThat(recordedRequest.getMethod(), equalTo("GET"));
        assertThat(recordedRequest.getPath(), equalTo("/_template/" + indexTemplateName()));

        recordedRequest = webServer.takeRequest();
        assertThat(recordedRequest.getMethod(), equalTo("GET"));
        assertThat(recordedRequest.getPath(), equalTo("/_template/" + dataTemplateName()));

        recordedRequest = webServer.takeRequest();
        assertThat(recordedRequest.getMethod(), equalTo("POST"));
        assertThat(recordedRequest.getPath(), equalTo("/_bulk"));

        logger.info("--> checks that the document in the bulk request is indexed in [{}]", expectedMarvelIndex);

        bytes = recordedRequest.getBody().readByteArray();
        data = XContentHelper.convertToMap(new BytesArray(bytes), false).v2();
        index = (Map<String, Object>) data.get("index");
        assertThat(index.get("_index"), equalTo(expectedMarvelIndex));
    }

    public void testLoadRemoteClusterVersion() throws IOException {
        final String host = webServer.getHostName() + ":" + webServer.getPort();

        Settings.Builder builder = Settings.builder()
                .put(MarvelSettings.INTERVAL_SETTING.getKey(), "-1")
                .put("marvel.agent.exporters._http.type", "http")
                .put("marvel.agent.exporters._http.host", host)
                .put("marvel.agent.exporters._http.connection.keep_alive", false);

        String agentNode = internalCluster().startNode(builder);
        HttpExporter exporter = getExporter(agentNode);

        enqueueGetClusterVersionResponse(Version.CURRENT);
        Version resolved = exporter.loadRemoteClusterVersion(host);
        assertTrue(resolved.equals(Version.CURRENT));

        final Version expected = randomFrom(Version.CURRENT, Version.V_0_18_0, Version.V_1_1_0, Version.V_1_2_5, Version.V_1_4_5, Version.V_1_6_0);
        enqueueGetClusterVersionResponse(expected);
        resolved = exporter.loadRemoteClusterVersion(host);
        assertTrue(resolved.equals(expected));
    }

    private HttpExporter getExporter(String nodeName) {
        Exporters exporters = internalCluster().getInstance(Exporters.class, nodeName);
        return (HttpExporter) exporters.iterator().next();
    }

    private MarvelDoc newRandomMarvelDoc() {
        if (randomBoolean()) {
            IndexRecoveryMarvelDoc doc = new IndexRecoveryMarvelDoc();
            doc.setClusterUUID(internalCluster().getClusterName());
            doc.setType(IndexRecoveryCollector.TYPE);
            doc.setTimestamp(System.currentTimeMillis());
            doc.setRecoveryResponse(new RecoveryResponse());
            return doc;
        } else {
            ClusterStateMarvelDoc doc = new ClusterStateMarvelDoc();
            doc.setClusterUUID(internalCluster().getClusterName());
            doc.setType(ClusterStateCollector.TYPE);
            doc.setTimestamp(System.currentTimeMillis());
            doc.setClusterState(ClusterState.PROTO);
            doc.setStatus(ClusterHealthStatus.GREEN);
            return doc;
        }
    }

    private List<MarvelDoc> newRandomMarvelDocs(int nb) {
        List<MarvelDoc> docs = new ArrayList<>(nb);
        for (int i = 0; i < nb; i++) {
            docs.add(newRandomMarvelDoc());
        }
        return docs;
    }

    private void enqueueGetClusterVersionResponse(Version v) throws IOException {
        enqueueGetClusterVersionResponse(webServer, v);
    }

    private void enqueueGetClusterVersionResponse(MockWebServer mockWebServer, Version v) throws IOException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(jsonBuilder().startObject().startObject("version").field("number", v.number()).endObject().endObject().bytes().toUtf8()));
    }

    private void enqueueResponse(int responseCode, String body) throws IOException {
        enqueueResponse(webServer, responseCode, body);
    }

    private void enqueueResponse(MockWebServer mockWebServer, int responseCode, String body) throws IOException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(responseCode).setBody(body));
    }

    private void assertBulkRequest(Buffer requestBody, int numberOfActions) throws Exception {
        BulkRequest bulkRequest = Requests.bulkRequest().add(new BytesArray(requestBody.readByteArray()), null, null);
        assertThat(bulkRequest.numberOfActions(), equalTo(numberOfActions));
        for (ActionRequest actionRequest : bulkRequest.requests()) {
            assertThat(actionRequest, instanceOf(IndexRequest.class));
        }
    }
}
