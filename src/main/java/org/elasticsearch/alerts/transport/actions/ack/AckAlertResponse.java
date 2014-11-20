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

package org.elasticsearch.alerts.transport.actions.ack;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.alerts.AlertAckState;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 */
public class AckAlertResponse extends ActionResponse {

    private AlertAckState alertAckState;

    public AckAlertResponse() {
    }

    public AckAlertResponse(@Nullable AlertAckState alertAckState) {
        this.alertAckState = alertAckState;
    }

    public AlertAckState getAlertAckState() {
        return alertAckState;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        if (in.readBoolean()) {
            AlertAckState.fromString(in.readString());
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeBoolean(alertAckState != null);
        if (alertAckState != null) {
            out.writeString(alertAckState.toString());
        }
    }
}
