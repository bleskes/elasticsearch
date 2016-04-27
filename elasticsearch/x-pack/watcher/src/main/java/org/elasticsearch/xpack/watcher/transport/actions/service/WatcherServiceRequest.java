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

package org.elasticsearch.xpack.watcher.transport.actions.service;

import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ValidateActions;
import org.elasticsearch.action.support.master.MasterNodeRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.Locale;

/**
 */
public class WatcherServiceRequest extends MasterNodeRequest<WatcherServiceRequest> {

    enum Command { START, STOP, RESTART }

    private Command command;

    public WatcherServiceRequest() {
    }

    /**
     * Starts the watcher service if not already started.
     */
    public WatcherServiceRequest start() {
        command = Command.START;
        return this;
    }

    /**
     * Stops the watcher service if not already stopped.
     */
    public WatcherServiceRequest stop() {
        command = Command.STOP;
        return this;
    }

    /**
     * Starts and stops the watcher.
     */
    public WatcherServiceRequest restart() {
        command = Command.RESTART;
        return this;
    }

    Command getCommand() {
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
        command = Command.valueOf(in.readString().toUpperCase(Locale.ROOT));
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(command.name().toLowerCase(Locale.ROOT));
    }
}
