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

package org.elasticsearch.xpack.monitoring.exporter;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.transport.LocalTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.test.ESTestCase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.elasticsearch.xpack.monitoring.exporter.MonitoringDoc.Node.fromDiscoveryNode;

public class MonitoringDocTests extends ESTestCase {

    public void testFromDiscoveryNode() {
        assertEquals(null, fromDiscoveryNode(null));

        String nodeId = randomAlphaOfLength(5);
        TransportAddress address = LocalTransportAddress.buildUnique();
        Version version = randomFrom(Version.V_2_4_1, Version.V_5_0_1, Version.CURRENT);

        String name = randomBoolean() ? randomAlphaOfLength(5) : "";
        Map<String, String> attributes = new HashMap<>();
        if (randomBoolean()) {
            int nbAttrs = randomIntBetween(1, 5);
            for (int i = 0; i < nbAttrs; i++) {
                attributes.put("attr_" + String.valueOf(i), String.valueOf(i));
            }
        }
        Set<DiscoveryNode.Role> roles = new HashSet<>();
        if (randomBoolean()) {
            randomSubsetOf(Arrays.asList(DiscoveryNode.Role.values())).forEach(roles::add);
        }
        final MonitoringDoc.Node expectedNode = new MonitoringDoc.Node(nodeId,
                address.getHost(), address.toString(),
                address.getAddress(), name, attributes);

        DiscoveryNode discoveryNode =
                new DiscoveryNode(name, nodeId, address, attributes, roles, version);
        assertEquals(expectedNode, fromDiscoveryNode(discoveryNode));
    }
}
