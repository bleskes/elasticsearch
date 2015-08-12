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

package org.elasticsearch.marvel.agent.collector.indices;

import org.elasticsearch.action.admin.indices.recovery.RecoveryResponse;
import org.elasticsearch.marvel.agent.exporter.MarvelDoc;

public class IndexRecoveryMarvelDoc extends MarvelDoc<IndexRecoveryMarvelDoc.Payload> {

    private final Payload payload;

    public IndexRecoveryMarvelDoc(String clusterName, String type, long timestamp, Payload payload) {
        super(clusterName, type, timestamp);
        this.payload = payload;
    }

    @Override
    public IndexRecoveryMarvelDoc.Payload payload() {
        return payload;
    }

    public static IndexRecoveryMarvelDoc createMarvelDoc(String clusterName, String type, long timestamp,
                                                         RecoveryResponse recoveryResponse) {
        return new IndexRecoveryMarvelDoc(clusterName, type, timestamp, new Payload(recoveryResponse));
    }

    public static class Payload {

        RecoveryResponse recoveryResponse;

        public Payload(RecoveryResponse recoveryResponse) {
            this.recoveryResponse = recoveryResponse;
        }

        public RecoveryResponse getRecoveryResponse() {
            return recoveryResponse;
        }

    }
}
