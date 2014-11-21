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

package org.elasticsearch.alerts.transport.actions.service;

import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ValidateActions;
import org.elasticsearch.action.support.master.MasterNodeOperationRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 */
public class AlertsServiceRequest extends MasterNodeOperationRequest<AlertsServiceRequest> {

    private String command;

    /**
     * Starts alerting if not already started.
     */
    public void start() {
        command = "start";
    }

    /**
     * Stops alerting if not already stopped.
     */
    public void stop() {
        command = "stop";
    }

    /**
     * Starts and stops alerting.
     */
    public void restart() {
        command = "restart";
    }

    String getCommand() {
        return command;
    }

    @Override
    public ActionRequestValidationException validate() {
        if (command == null) {
            return ValidateActions.addValidationError("no command specified", null);
        } else {
            return null;
        }
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        command = in.readString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(command);
    }
}
