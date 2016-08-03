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

package org.elasticsearch.xpack.monitoring.agent.exporter;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.LicenseService;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.test.ESIntegTestCase.ClusterScope;
import org.elasticsearch.xpack.monitoring.MonitoredSystem;
import org.elasticsearch.xpack.monitoring.MonitoringSettings;
import org.elasticsearch.xpack.monitoring.agent.collector.Collector;
import org.elasticsearch.xpack.monitoring.agent.collector.cluster.ClusterStatsCollector;
import org.elasticsearch.xpack.monitoring.test.MonitoringIntegTestCase;
import org.elasticsearch.xpack.security.InternalClient;

import java.io.IOException;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.test.ESIntegTestCase.Scope.TEST;
import static org.hamcrest.Matchers.notNullValue;

@ClusterScope(scope = TEST, numDataNodes = 0, numClientNodes = 0, transportClientRatio = 0.0)
public abstract class AbstractExporterTemplateTestCase extends MonitoringIntegTestCase {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        Settings.Builder settings = Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(MonitoringSettings.INTERVAL.getKey(), "-1");

        for (Map.Entry<String, String> setting : exporterSettings().getAsMap().entrySet()) {
            settings.put("xpack.monitoring.exporters._exporter." + setting.getKey(), setting.getValue());
        }
        return settings.build();
    }

    protected abstract Settings exporterSettings();

    protected abstract void deleteTemplates() throws Exception;

    protected abstract void putTemplate(String name) throws Exception;

    protected abstract void assertTemplateExist(String name) throws Exception;

    protected abstract void assertTemplateNotUpdated(String name) throws Exception;

    public void testCreateWhenNoExistingTemplates() throws Exception {
        internalCluster().startNode();

        deleteTemplates();
        doExporting();

        logger.debug("--> templates does not exist: it should have been created in the current version");
        for (String template : monitoringTemplates().keySet()) {
            assertTemplateExist(template);
        }

        doExporting();

        logger.debug("--> indices should have been created");
        awaitIndexExists(currentDataIndexName());
        awaitIndexExists(currentTimestampedIndexName());
    }

    public void testCreateWhenExistingTemplatesAreOld() throws Exception {
        internalCluster().startNode();

        putTemplate(indexTemplateName());
        putTemplate(dataTemplateName());

        doExporting();

        logger.debug("--> existing templates are old");
        assertTemplateExist(dataTemplateName());
        assertTemplateExist(indexTemplateName());

        logger.debug("--> existing templates are old: new templates should be created");
        for (String template : monitoringTemplates().keySet()) {
            assertTemplateExist(template);
        }

        doExporting();

        logger.debug("--> indices should have been created");
        awaitIndexExists(currentDataIndexName());
        awaitIndexExists(currentTimestampedIndexName());
    }

    public void testCreateWhenExistingTemplateAreUpToDate() throws Exception {
        internalCluster().startNode();

        putTemplate(indexTemplateName());
        putTemplate(dataTemplateName());

        doExporting();

        logger.debug("--> existing templates are up to date");
        for (String template : monitoringTemplates().keySet()) {
            assertTemplateExist(template);
        }

        logger.debug("--> existing templates has the same version: they should not be changed");
        assertTemplateNotUpdated(indexTemplateName());
        assertTemplateNotUpdated(dataTemplateName());

        doExporting();

        logger.debug("--> indices should have been created");
        awaitIndexExists(currentDataIndexName());
        awaitIndexExists(currentTimestampedIndexName());
    }

    protected void doExporting() throws Exception {
        // TODO: these should be unit tests, not using guice
        ClusterService clusterService = internalCluster().getInstance(ClusterService.class);
        XPackLicenseState licenseState = internalCluster().getInstance(XPackLicenseState.class);
        LicenseService licenseService = internalCluster().getInstance(LicenseService.class);
        InternalClient client = internalCluster().getInstance(InternalClient.class);
        Collector collector = new ClusterStatsCollector(clusterService.getSettings(), clusterService,
            new MonitoringSettings(clusterService.getSettings(), clusterService.getClusterSettings()),
            licenseState, client, licenseService);

        Exporters exporters = internalCluster().getInstance(Exporters.class);
        assertNotNull(exporters);

        // Wait for exporting bulks to be ready to export
        Runnable busy = () -> assertThat(exporters.openBulk(), notNullValue());
        assertBusy(busy);
        exporters.export(collector.collect());
    }

    private String dataTemplateName() {
        MockDataIndexNameResolver resolver = new MockDataIndexNameResolver(MonitoringTemplateUtils.TEMPLATE_VERSION);
        return resolver.templateName();
    }

    private String indexTemplateName() {
        MockTimestampedIndexNameResolver resolver =
                new MockTimestampedIndexNameResolver(MonitoredSystem.ES, exporterSettings(), MonitoringTemplateUtils.TEMPLATE_VERSION);
        return resolver.templateName();
    }

    private String currentDataIndexName() {
        MockDataIndexNameResolver resolver = new MockDataIndexNameResolver(MonitoringTemplateUtils.TEMPLATE_VERSION);
        return resolver.index(null);
    }

    private String currentTimestampedIndexName() {
        MonitoringDoc doc = new MonitoringDoc(MonitoredSystem.ES.getSystem(), Version.CURRENT.toString());
        doc.setTimestamp(System.currentTimeMillis());

        MockTimestampedIndexNameResolver resolver =
                new MockTimestampedIndexNameResolver(MonitoredSystem.ES, exporterSettings(), MonitoringTemplateUtils.TEMPLATE_VERSION);
        return resolver.index(doc);
    }

    /** Generates a basic template **/
    protected static BytesReference generateTemplateSource(String name) throws IOException {
        return jsonBuilder().startObject()
                                .field("template", name)
                                .startObject("settings")
                                    .field("index.number_of_shards", 1)
                                    .field("index.number_of_replicas", 1)
                                .endObject()
                                .startObject("mappings")
                                    .startObject("_default_")
                                        .startObject("_all")
                                            .field("enabled", false)
                                        .endObject()
                                        .field("date_detection", false)
                                        .startObject("properties")
                                            .startObject("cluster_uuid")
                                                .field("type", "keyword")
                                            .endObject()
                                            .startObject("timestamp")
                                                .field("type", "date")
                                                .field("format", "date_time")
                                            .endObject()
                                        .endObject()
                                    .endObject()
                                    .startObject("cluster_info")
                                        .field("enabled", false)
                                    .endObject()
                                    .startObject("cluster_stats")
                                        .startObject("properties")
                                            .startObject("cluster_stats")
                                                .field("type", "object")
                                            .endObject()
                                        .endObject()
                                    .endObject()
                                .endObject()
                            .endObject().bytes();
    }
}
