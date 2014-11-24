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
import org.elasticsearch.alerts.State;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 * The AlertStatsResponse response
 */
public class AlertsStatsResponse extends ActionResponse {

    private long numberOfRegisteredAlerts;
    private State alertManagerState;
    private boolean alertActionManagerStarted;
    private long alertActionManagerQueueSize;


    private long alertActionManagerLargestQueueSize;

    public AlertsStatsResponse() {
    }

    /**
     * Gets the current queue size in the alert action manager
     * @return
     */
    public long getAlertActionManagerQueueSize() {
        return alertActionManagerQueueSize;
    }

    /**
     * Sets the current size of the alert action queue
     * @param alertActionManagerQueueSize
     */
    public void setAlertActionManagerQueueSize(long alertActionManagerQueueSize) {
        this.alertActionManagerQueueSize = alertActionManagerQueueSize;
    }

    /**
     * The number of alerts currently registered in the system
     * @return
     */
    public long getNumberOfRegisteredAlerts() {
        return numberOfRegisteredAlerts;
    }

    /**
     * Set the number of alerts currently registered in the system
     * @param numberOfRegisteredAlerts
     */
    public void setNumberOfRegisteredAlerts(long numberOfRegisteredAlerts) {
        this.numberOfRegisteredAlerts = numberOfRegisteredAlerts;
    }

    /**
     * Returns the state of the alert manager.
     */
    public State getAlertManagerStarted() {
        return alertManagerState;
    }

    void setAlertManagerState(State alertManagerState) {
        this.alertManagerState = alertManagerState;
    }

    /**
     * Returns true if the alert action manager is started
     * @return
     */
    public boolean isAlertActionManagerStarted() {
        return alertActionManagerStarted;
    }

    /**
     * Sets if the alert action manager is started
     * @param alertActionManagerStarted
     */
    public void setAlertActionManagerStarted(boolean alertActionManagerStarted) {
        this.alertActionManagerStarted = alertActionManagerStarted;
    }

    /**
     * Sets the largest queue size the alert action manager queue has grown to
     * @return
     */
    public long getAlertActionManagerLargestQueueSize() {
        return alertActionManagerLargestQueueSize;
    }

    /**
     * Sets the largest alert action manager queue size
     * @param alertActionManagerLargestQueueSize
     */
    public void setAlertActionManagerLargestQueueSize(long alertActionManagerLargestQueueSize) {
        this.alertActionManagerLargestQueueSize = alertActionManagerLargestQueueSize;
    }


    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        numberOfRegisteredAlerts = in.readLong();
        alertActionManagerQueueSize = in.readLong();
        alertActionManagerLargestQueueSize = in.readLong();
        alertManagerState = State.fromId(in.readByte());
        alertActionManagerStarted = in.readBoolean();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeLong(numberOfRegisteredAlerts);
        out.writeLong(alertActionManagerQueueSize);
        out.writeLong(alertActionManagerLargestQueueSize);
        out.writeByte(alertManagerState.getId());
        out.writeBoolean(alertActionManagerStarted);
    }
}
