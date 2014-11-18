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

package org.elasticsearch.alerts.transport.actions.get;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.IndicesRequest;
import org.elasticsearch.action.ValidateActions;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.alerts.AlertsStore;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.lucene.uid.Versions;
import org.elasticsearch.index.VersionType;

import java.io.IOException;

/**
 * The request to get the alert by name (id)
 */
public class GetAlertRequest extends ActionRequest<GetAlertRequest> implements IndicesRequest {

    private String alertName;
    private long version = Versions.MATCH_ANY;
    private VersionType versionType = VersionType.INTERNAL;


    public GetAlertRequest() {
    }

    /**
     * Constructor taking name (id) of the alert to retrieve
     * @param alertName
     */
    public GetAlertRequest(String alertName) {
        this.alertName = alertName;
    }


    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = null;
        if (alertName == null) {
            validationException = ValidateActions.addValidationError("alertName is missing", validationException);
        }
        return validationException;
    }

    @Override
    public String[] indices() {
        return new String[]{AlertsStore.ALERT_INDEX};
    }

    @Override
    public IndicesOptions indicesOptions() {
        return IndicesOptions.strictSingleIndexNoExpandForbidClosed();
    }


    /**
     * The name of the alert to retrieve
     * @return
     */
    public String alertName() {
        return alertName;
    }

    public GetAlertRequest alertName(String alertName){
        this.alertName = alertName;
        return this;
    }

    /**
     * Sets the version, which will cause the delete operation to only be performed if a matching
     * version exists and no changes happened on the doc since then.
     */
    public GetAlertRequest version(long version) {
        this.version = version;
        return this;
    }

    public long version() {
        return this.version;
    }

    public GetAlertRequest versionType(VersionType versionType) {
        this.versionType = versionType;
        return this;
    }

    public VersionType versionType() {
        return this.versionType;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        version = Versions.readVersion(in);
        versionType = VersionType.fromValue(in.readByte());
        alertName = in.readString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        Versions.writeVersion(version, out);
        out.writeByte(versionType.getValue());
        out.writeString(alertName);
    }

    @Override
    public String toString() {
        return "delete {[" + AlertsStore.ALERT_INDEX + "][" + alertName +"]}";
    }
}
