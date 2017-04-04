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

package org.elasticsearch.xpack.monitoring.resolver;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.monitoring.exporter.MonitoringDoc;
import org.elasticsearch.xpack.monitoring.exporter.MonitoringTemplateUtils;

import java.io.IOException;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.elasticsearch.common.transport.LocalTransportAddress.buildUnique;
import static org.hamcrest.Matchers.equalTo;

public class DataResolverTests extends MonitoringIndexNameResolverTestCase {

    @Override
    protected MonitoringIndexNameResolver<MonitoringDoc> newResolver() {
        return newDataResolver();
    }

    @Override
    protected MonitoringDoc newMonitoringDoc() {
        MonitoringDoc doc = new MonitoringDoc(randomMonitoringId(), randomAlphaOfLength(2),
                null, null, randomAlphaOfLength(5), Math.abs(randomLong()),
                new DiscoveryNode("id", buildUnique(), emptyMap(), emptySet(), Version.CURRENT));
        return doc;
    }

    @Override
    protected boolean checkFilters() {
        return false;
    }

    public void testDataResolver() {
        assertThat(newDataResolver().index(newMonitoringDoc()), equalTo(".monitoring-data-" + MonitoringTemplateUtils.TEMPLATE_VERSION));
    }

    private MonitoringIndexNameResolver.Data<MonitoringDoc> newDataResolver() {
        return new MonitoringIndexNameResolver.Data<MonitoringDoc>() {
            @Override
            protected void buildXContent(MonitoringDoc document, XContentBuilder builder, ToXContent.Params params) throws IOException {
            }
        };
    }
}
