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

public class IndexRecoveryMarvelDoc extends MarvelDoc {

    private final RecoveryResponse recoveryResponse;

    public IndexRecoveryMarvelDoc(String clusterUUID, String type, long timestamp, RecoveryResponse recoveryResponse) {
        super(clusterUUID, type, timestamp);
        this.recoveryResponse = recoveryResponse;
    }

    public RecoveryResponse getRecoveryResponse() {
        return recoveryResponse;
    }
}
