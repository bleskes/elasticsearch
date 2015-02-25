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

package org.elasticsearch.alerts.transport.actions.put;


import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ValidateActions;
import org.elasticsearch.action.support.master.MasterNodeOperationRequest;
import org.elasticsearch.alerts.client.AlertSourceBuilder;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;

/**
 * This request class contains the data needed to create an alert along with the name of the alert
 * the name of the alert will become the ID of the indexed document.
 */
public class PutAlertRequest extends MasterNodeOperationRequest<PutAlertRequest> {

    private String alertName;
    private BytesReference alertSource;
    private boolean alertSourceUnsafe;

    public PutAlertRequest() {
    }

    /**
     * @param alertSource The alertSource
     */
    public PutAlertRequest(BytesReference alertSource) {
        this.alertSource = alertSource;
    }

    /**
     * @return The name that will be the ID of the indexed document
     */
    public String getAlertName() {
        return alertName;
    }

    /**
     * Set the alert name
     */
    public void setAlertName(String alertName) {
        this.alertName = alertName;
    }

    /**
     * @return The source of the alert
     */
    public BytesReference getAlertSource() {
        return alertSource;
    }

    /**
     * Set the source of the alert
     */
    public void source(AlertSourceBuilder source) {
        source(source.buildAsBytes(XContentType.JSON));
    }

    /**
     * Set the source of the alert
     */
    public void source(BytesReference alertSource) {
        this.alertSource = alertSource;
        this.alertSourceUnsafe = false;
    }

    /**
     * Set the source of the alert with boolean to control source safety
     */
    public void source(BytesReference alertSource, boolean alertSourceUnsafe) {
        this.alertSource = alertSource;
        this.alertSourceUnsafe = alertSourceUnsafe;
    }

    public void beforeLocalFork() {
        if (alertSourceUnsafe) {
            alertSource = alertSource.copyBytesArray();
            alertSourceUnsafe = false;
        }
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = null;
        if (alertName == null) {
            validationException = ValidateActions.addValidationError("alertName is missing", validationException);
        }
        if (alertSource == null) {
            validationException = ValidateActions.addValidationError("alertSource is missing", validationException);
        }
        return validationException;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        alertName = in.readString();
        alertSource = in.readBytesReference();
        alertSourceUnsafe = false;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(alertName);
        out.writeBytesReference(alertSource);
    }

}
