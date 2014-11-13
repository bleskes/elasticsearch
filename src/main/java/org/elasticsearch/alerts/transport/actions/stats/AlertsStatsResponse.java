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

package org.elasticsearch.alerts.transport.actions.stats;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 */
public class AlertsStatsResponse extends ActionResponse {

    private long numberOfRegisteredAlerts;
    private boolean alertManagerStarted;
    private boolean alertActionManagerStarted;
    private long alertActionManagerQueueSize;

    public AlertsStatsResponse() {
    }



    public long getAlertActionManagerQueueSize() {
        return alertActionManagerQueueSize;
    }

    public void setAlertActionManagerQueueSize(long alertActionManagerQueueSize) {
        this.alertActionManagerQueueSize = alertActionManagerQueueSize;
    }

    public long getNumberOfRegisteredAlerts() {
        return numberOfRegisteredAlerts;
    }

    public void setNumberOfRegisteredAlerts(long numberOfRegisteredAlerts) {
        this.numberOfRegisteredAlerts = numberOfRegisteredAlerts;
    }

    public boolean isAlertManagerStarted() {
        return alertManagerStarted;
    }

    public void setAlertManagerStarted(boolean alertManagerStarted) {
        this.alertManagerStarted = alertManagerStarted;
    }

    public boolean isAlertActionManagerStarted() {
        return alertActionManagerStarted;
    }

    public void setAlertActionManagerStarted(boolean alertActionManagerStarted) {
        this.alertActionManagerStarted = alertActionManagerStarted;
    }


    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        numberOfRegisteredAlerts = in.readLong();
        alertActionManagerQueueSize = in.readLong();
        alertManagerStarted = in.readBoolean();
        alertActionManagerStarted = in.readBoolean();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeLong(numberOfRegisteredAlerts);
        out.writeLong(alertActionManagerQueueSize);
        out.writeBoolean(alertManagerStarted);
        out.writeBoolean(alertActionManagerStarted);
    }
}
