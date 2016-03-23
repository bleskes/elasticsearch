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

package org.elasticsearch.marvel.agent.resolver.bulk;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.transport.DummyTransportAddress;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.marvel.MonitoredSystem;
import org.elasticsearch.marvel.action.MonitoringBulkDoc;
import org.elasticsearch.marvel.agent.resolver.MonitoringIndexNameResolverTestCase;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class MonitoringBulkResolverTests extends MonitoringIndexNameResolverTestCase<MonitoringBulkDoc, MonitoringBulkResolver> {

    @Override
    protected MonitoringBulkDoc newMarvelDoc() {
        MonitoringBulkDoc doc = new MonitoringBulkDoc(MonitoredSystem.KIBANA.getSystem(), Version.CURRENT.toString());
        doc.setClusterUUID(randomAsciiOfLength(5));
        doc.setTimestamp(Math.abs(randomLong()));
        doc.setSourceNode(new DiscoveryNode("id", DummyTransportAddress.INSTANCE, emptyMap(), emptySet(), Version.CURRENT));
        doc.setType("kibana_stats");
        doc.setSource(new BytesArray("{\"field1\" : \"value1\"}"));
        return doc;
    }

    @Override
    protected boolean checkResolvedId() {
        return false;
    }

    @Override
    protected boolean checkFilters() {
        return false;
    }

    public void testMonitoringBulkResolver() throws Exception {
        MonitoringBulkDoc doc = newMarvelDoc();
        doc.setTimestamp(1437580442979L);
        if (randomBoolean()) {
            doc.setIndex(randomAsciiOfLength(5));
        }
        if (randomBoolean()) {
            doc.setId(randomAsciiOfLength(35));
        }
        if (randomBoolean()) {
            doc.setClusterUUID(randomAsciiOfLength(5));
        }

        MonitoringBulkResolver resolver = newResolver();
        assertThat(resolver.index(doc), equalTo(".monitoring-kibana-2-2015.07.22"));
        assertThat(resolver.type(doc), equalTo(doc.getType()));
        assertThat(resolver.id(doc), nullValue());

        assertSource(resolver.source(doc, XContentType.JSON),
                "cluster_uuid",
                "timestamp",
                "source_node",
                "kibana_stats",
                "kibana_stats.field1");
    }
}
