/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.alerts.transport.actions.delete;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.IndicesRequest;
import org.elasticsearch.action.ValidateActions;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.master.MasterNodeOperationRequest;
import org.elasticsearch.alerts.AlertManager;
import org.elasticsearch.alerts.AlertsStore;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.lucene.uid.Versions;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.script.ScriptService;

import java.io.IOException;

import static org.elasticsearch.action.ValidateActions.addValidationError;

/**
 */
public class DeleteAlertRequest extends MasterNodeOperationRequest<DeleteAlertRequest> implements IndicesRequest {

    private long version = Versions.MATCH_ANY;

    private String alertName;


    public DeleteAlertRequest() {
    }

    public DeleteAlertRequest(String alertName) {
        this.alertName = alertName;
    }

    public void alertName(String alertName) {
        this.alertName = alertName;
    }

    public String alertName() {
        return alertName;
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
    public String[] indices() {
        return new String[]{AlertsStore.ALERT_INDEX};
    }

    @Override
    public IndicesOptions indicesOptions() {
        return IndicesOptions.strictSingleIndexNoExpandForbidClosed();
    }


    /**
     * Sets the version, which will cause the delete operation to only be performed if a matching
     * version exists and no changes happened on the doc since then.
     */
    public DeleteAlertRequest version(long version) {
        this.version = version;
        return this;
    }

    public long version() {
        return this.version;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        version = Versions.readVersion(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        Versions.writeVersion(version, out);

    }

    @Override
    public String toString() {
        return "delete {[" + AlertsStore.ALERT_INDEX + "][" +  alertName + "]}";
    }
}
