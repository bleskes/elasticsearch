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

package org.elasticsearch.alerts.transport.actions.index;


import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ValidateActions;
import org.elasticsearch.action.support.master.MasterNodeOperationRequest;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 */
public class IndexAlertRequest extends MasterNodeOperationRequest<IndexAlertRequest> {

    private String alertName;
    private BytesReference alertSource;
    private boolean alertSourceUnsafe;

    public IndexAlertRequest() {
    }

    public IndexAlertRequest(BytesReference alertSource) {
        this.alertSource = alertSource;
    }

    public String getAlertName() {
        return alertName;
    }

    public void setAlertName(String alertName) {
        this.alertName = alertName;
    }

    public BytesReference getAlertSource() {
        return alertSource;
    }

    public void setAlertSource(BytesReference alertSource) {
        this.alertSource = alertSource;
        this.alertSourceUnsafe = false;
    }

    public void setAlertSource(BytesReference alertSource, boolean alertSourceUnsafe) {
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
