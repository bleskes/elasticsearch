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

package org.elasticsearch.xpack.monitoring.resolver.bulk;

import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.xpack.monitoring.MonitoredSystem;
import org.elasticsearch.xpack.monitoring.action.MonitoringBulkDoc;
import org.elasticsearch.xpack.monitoring.action.MonitoringBulkDocTests;
import org.elasticsearch.xpack.monitoring.action.MonitoringIndex;
import org.elasticsearch.xpack.monitoring.exporter.MonitoringTemplateUtils;
import org.elasticsearch.xpack.monitoring.resolver.MonitoringIndexNameResolverTestCase;

import static org.hamcrest.Matchers.equalTo;

/**
 * Tests {@link MonitoringBulkDataResolver}.
 */
public class MonitoringBulkDataResolverTests extends MonitoringIndexNameResolverTestCase<MonitoringBulkDoc, MonitoringBulkDataResolver> {

    private final String id = randomBoolean() ? randomAlphaOfLength(35) : null;

    @Override
    protected MonitoringBulkDoc newMonitoringDoc() {
        MonitoringBulkDoc doc = new MonitoringBulkDoc(MonitoredSystem.KIBANA.getSystem(),
                MonitoringTemplateUtils.TEMPLATE_VERSION, MonitoringIndex.DATA, "kibana", id,
                randomAlphaOfLength(5), Math.abs(randomLong()),
                MonitoringBulkDocTests.newRandomSourceNode(),
                new BytesArray("{\"field1\" : \"value1\"}"), XContentType.JSON);
        return doc;
    }

    @Override
    protected boolean checkFilters() {
        return false;
    }

    public void testMonitoringBulkResolver() throws Exception {
        MonitoringBulkDoc doc = newMonitoringDoc();

        MonitoringBulkDataResolver resolver = newResolver(doc);
        assertThat(resolver.index(doc), equalTo(".monitoring-data-2"));

        assertSource(resolver.source(doc, XContentType.JSON),
                Sets.newHashSet(
                        "cluster_uuid",
                        "timestamp",
                        "source_node",
                        "kibana",
                        "kibana.field1"), XContentType.JSON);
    }
}
