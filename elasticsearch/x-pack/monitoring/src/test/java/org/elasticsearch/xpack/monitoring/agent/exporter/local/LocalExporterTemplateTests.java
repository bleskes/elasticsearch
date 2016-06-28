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

package org.elasticsearch.xpack.monitoring.agent.exporter.local;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.monitoring.agent.exporter.AbstractExporterTemplateTestCase;

import java.util.Collections;

import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;

public class LocalExporterTemplateTests extends AbstractExporterTemplateTestCase {

    @Override
    protected Settings exporterSettings() {
        return Settings.builder().put("type", LocalExporter.TYPE).build();
    }

    @Override
    protected void deleteTemplates() throws Exception {
        waitNoPendingTasksOnAll();
        cluster().wipeAllTemplates(Collections.emptySet());
    }

    @Override
    protected void putTemplate(String name) throws Exception {
        waitNoPendingTasksOnAll();
        assertAcked(client().admin().indices().preparePutTemplate(name).setSource(generateTemplateSource(name)).get());
    }

    @Override
    protected void assertTemplateExist(String name) throws Exception {
        waitNoPendingTasksOnAll();
        waitForMonitoringTemplate(name);
    }

    @Override
    protected void assertTemplateNotUpdated(String name) throws Exception {
        waitNoPendingTasksOnAll();
        assertTemplateExist(name);
    }
}
