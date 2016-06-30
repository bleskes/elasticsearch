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

package org.elasticsearch.xpack.monitoring.action;

import org.elasticsearch.Version;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.monitoring.agent.exporter.MonitoringDoc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.elasticsearch.test.VersionUtils.randomVersion;
import static org.hamcrest.Matchers.equalTo;

public class MonitoringBulkDocTests extends ESTestCase {

    public void testSerialization() throws IOException {
        int iterations = randomIntBetween(5, 50);
        for (int i = 0; i < iterations; i++) {
            MonitoringBulkDoc doc = newRandomMonitoringBulkDoc();

            boolean hasSourceNode = randomBoolean();
            if (hasSourceNode) {
                doc.setSourceNode(newRandomSourceNode());
            }

            BytesStreamOutput output = new BytesStreamOutput();
            Version outputVersion = randomVersion(random());
            output.setVersion(outputVersion);
            doc.writeTo(output);

            StreamInput streamInput = output.bytes().streamInput();
            streamInput.setVersion(randomVersion(random()));
            MonitoringBulkDoc doc2 = new MonitoringBulkDoc(streamInput);

            assertThat(doc2.getMonitoringId(), equalTo(doc.getMonitoringId()));
            assertThat(doc2.getMonitoringVersion(), equalTo(doc.getMonitoringVersion()));
            assertThat(doc2.getClusterUUID(), equalTo(doc.getClusterUUID()));
            assertThat(doc2.getTimestamp(), equalTo(doc.getTimestamp()));
            assertThat(doc2.getSourceNode(), equalTo(doc.getSourceNode()));
            assertThat(doc2.getIndex(), equalTo(doc.getIndex()));
            assertThat(doc2.getType(), equalTo(doc.getType()));
            assertThat(doc2.getId(), equalTo(doc.getId()));
            if (doc.getSource() == null) {
                assertThat(doc2.getSource(), equalTo(BytesArray.EMPTY));
            } else {
                assertThat(doc2.getSource(), equalTo(doc.getSource()));
            }
        }
    }

    private MonitoringBulkDoc newRandomMonitoringBulkDoc() {
        MonitoringBulkDoc doc = new MonitoringBulkDoc(randomAsciiOfLength(2), randomAsciiOfLength(2));
        if (frequently()) {
            doc.setClusterUUID(randomAsciiOfLength(5));
            doc.setType(randomAsciiOfLength(5));
        }
        if (randomBoolean()) {
            doc.setTimestamp(System.currentTimeMillis());
            doc.setSource(new BytesArray("{\"key\" : \"value\"}"));
        }
        if (rarely()) {
            doc.setIndex(MonitoringIndex.DATA);
            doc.setId(randomAsciiOfLength(2));
        }
        return doc;
    }

    private MonitoringDoc.Node newRandomSourceNode() {
        String uuid = null;
        String name = null;
        String ip = null;
        String transportAddress = null;
        String host = null;
        Map<String, String> attributes = null;

        if (frequently()) {
            uuid = randomAsciiOfLength(5);
            name = randomAsciiOfLength(5);
        }
        if (randomBoolean()) {
            ip = randomAsciiOfLength(5);
            transportAddress = randomAsciiOfLength(5);
            host = randomAsciiOfLength(3);
        }
        if (rarely()) {
            int nbAttributes = randomIntBetween(0, 5);
            attributes = new HashMap<>();
            for (int i = 0; i < nbAttributes; i++) {
                attributes.put("key#" + i, String.valueOf(i));
            }
        }
        return new MonitoringDoc.Node(uuid, host, transportAddress, ip, name, attributes);
    }
}
