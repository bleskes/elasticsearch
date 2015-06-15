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

package org.elasticsearch.watcher.transport.actions.execute;

import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ValidateActions;
import org.elasticsearch.action.support.master.MasterNodeReadRequest;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.watcher.execution.ActionExecutionMode;
import org.elasticsearch.watcher.support.validation.Validation;
import org.elasticsearch.watcher.trigger.TriggerEvent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * An execute watch request to execute a watch by id
 */
public class ExecuteWatchRequest extends MasterNodeReadRequest<ExecuteWatchRequest> {

    private String id;
    private boolean ignoreCondition = false;
    private boolean recordExecution = false;
    private @Nullable Map<String, Object> triggerData = null;
    private @Nullable Map<String, Object> alternativeInput = null;
    private Map<String, ActionExecutionMode> actionModes = new HashMap<>();

    ExecuteWatchRequest() {
    }

    /**
     * @param id the id of the watch to execute
     */
    public ExecuteWatchRequest(String id) {
        this.id = id;
    }

    /**
     * @return The id of the watch to be executed
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the id of the watch to be executed
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return Should the condition for this execution be ignored
     */
    public boolean isIgnoreCondition() {
        return ignoreCondition;
    }

    /**
     * @param ignoreCondition set if the condition for this execution be ignored
     */
    public void setIgnoreCondition(boolean ignoreCondition) {
        this.ignoreCondition = ignoreCondition;
    }

    /**
     * @return Should this execution be recorded in the history index
     */
    public boolean isRecordExecution() {
        return recordExecution;
    }

    /**
     * @param recordExecution Sets if this execution be recorded in the history index
     */
    public void setRecordExecution(boolean recordExecution) {
        this.recordExecution = recordExecution;
    }

    /**
     * @return The alertnative input to use (may be null)
     */
    public Map<String, Object> getAlternativeInput() {
        return alternativeInput;
    }

    /**
     * @param alternativeInput Set's the alernative input
     */
    public void setAlternativeInput(Map<String, Object> alternativeInput) {
        this.alternativeInput = alternativeInput;
    }

    /**
     * @param data The data that should be associated with the trigger event.
     * @throws IOException
     */
    public void setTriggerData(Map<String, Object> data) throws IOException {
        this.triggerData = data;
    }

    /**
     * @param event the trigger event to use
     * @throws IOException
     */
    public void setTriggerEvent(TriggerEvent event) throws IOException {
        setTriggerData(event.data());
    }

    /**
     * @return the trigger to use
     */
    public Map<String, Object> getTriggerData() {
        return triggerData;
    }


    /**
     *
     * @return  the execution modes for the actions. These modes determine the nature of the execution
     *          of the watch actions while the watch is executing.
     */
    public Map<String, ActionExecutionMode> getActionModes() {
        return actionModes;
    }

    /**
     * Sets the action execution mode for the give action (identified by its id).
     *
     * @param actionId      the action id.
     * @param actionMode    the execution mode of the action.
     */
    public void setActionMode(String actionId, ActionExecutionMode actionMode) {
        actionModes.put(actionId, actionMode);
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = null;
        if (id == null){
            validationException = ValidateActions.addValidationError("watch id is missing", validationException);
        }
        Validation.Error error = Validation.watchId(id);
        if (error != null) {
            validationException = ValidateActions.addValidationError(error.message(), validationException);
        }
        for (Map.Entry<String, ActionExecutionMode> modes : actionModes.entrySet()) {
            error = Validation.actionId(modes.getKey());
            if (error != null) {
                validationException = ValidateActions.addValidationError(error.message(), validationException);
            }
        }
        return validationException;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        id = in.readString();
        ignoreCondition = in.readBoolean();
        recordExecution = in.readBoolean();
        if (in.readBoolean()){
            alternativeInput = in.readMap();
        }
        if (in.readBoolean()) {
            triggerData = in.readMap();
        }
        long actionModesCount = in.readLong();
        actionModes = new HashMap<>();
        for (int i = 0; i < actionModesCount; i++) {
            actionModes.put(in.readString(), ActionExecutionMode.resolve(in.readByte()));
        }
    }


    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(id);
        out.writeBoolean(ignoreCondition);
        out.writeBoolean(recordExecution);
        out.writeBoolean(alternativeInput != null);
        if (alternativeInput != null) {
            out.writeMap(alternativeInput);
        }
        out.writeBoolean(triggerData != null);
        if (triggerData != null) {
            out.writeMap(triggerData);
        }
        out.writeLong(actionModes.size());
        for (Map.Entry<String, ActionExecutionMode> entry : actionModes.entrySet()) {
            out.writeString(entry.getKey());
            out.writeByte(entry.getValue().id());
        }
    }

    @Override
    public String toString() {
        return "execute[" + id + "]";
    }
}
