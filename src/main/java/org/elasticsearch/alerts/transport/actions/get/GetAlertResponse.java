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

package org.elasticsearch.alerts.transport.actions.get;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.alerts.Alert;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 */
public class GetAlertResponse extends ActionResponse {
    boolean found = false;
    Alert alert = null;

    public GetAlertResponse() {

    }

    public boolean found() {
        return this.found;
    }

    public void found(boolean found) {
        this.found = found;
    }

    public Alert alert() {
        return alert;
    }

    public void alert(Alert alert){
        this.alert = alert;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        found = in.readBoolean();
        if (found) {
            alert = new Alert();
            alert.readFrom(in);
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeBoolean(found);
        if (found && alert != null){
            alert.writeTo(out);
        }
    }
}
