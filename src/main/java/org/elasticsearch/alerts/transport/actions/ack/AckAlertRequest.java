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

import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ValidateActions;
import org.elasticsearch.action.support.master.MasterNodeOperationRequest;
import org.elasticsearch.alerts.AlertsStore;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 * A delete alert request to delete an alert by name (id)
 */
public class AckAlertRequest extends MasterNodeOperationRequest<AckAlertRequest> {

    private String alertName;

    public AckAlertRequest() {
    }

    /**
     * The constructor for the requests that takes the name of the alert to ack
     * @param alertName
     */
    public AckAlertRequest(String alertName) {
        this.alertName = alertName;
    }

    /**
     * The name of the alert to be acked
     * @return
     */
    public String getAlertName() {
        return alertName;
    }

    /**
     * The name of the alert to be acked
     * @param alertName
     */
    public void setAlertName(String alertName) {
        this.alertName = alertName;
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = null;
        if (alertName == null){
            validationException = ValidateActions.addValidationError("alertName is missing", validationException);
        }
        return validationException;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        alertName = in.readString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(alertName);
    }

    @Override
    public String toString() {
        return "ack {[" + AlertsStore.ALERT_INDEX + "][" +  alertName + "]}";
    }
}
