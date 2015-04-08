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
import org.elasticsearch.action.support.master.MasterNodeOperationRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.watcher.watch.WatchStore;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * An execute watch request to execute a watch by id
 */
public class ExecuteWatchRequest extends MasterNodeOperationRequest<ExecuteWatchRequest> {

    private String id;
    private boolean ignoreCondition = true;
    private boolean ignoreThrottle = false;
    private boolean recordInHistory = false;
    private Map<String, Object> alternativeInput = null;
    private Map<String, Object> triggerData = null;
    private Set<String> simulatedActionIds = new HashSet<>();

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
     * @return Should the throttle be ignored for this execution
     */
    public boolean isIgnoreThrottle() {
        return ignoreThrottle;
    }

    /**
     * @param ignoreThrottle Sets if the throttle should be ignored for this execution
     */
    public void setIgnoreThrottle(boolean ignoreThrottle) {
        this.ignoreThrottle = ignoreThrottle;
    }

    /**
     * @return Should this execution be recorded in the history index
     */
    public boolean isRecordInHistory() {
        return recordInHistory;
    }

    /**
     * @param recordInHistory Sets if this execution be recorded in the history index
     */
    public void setRecordInHistory(boolean recordInHistory) {
        this.recordInHistory = recordInHistory;
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
     * @return the alternative input to use
     */
    public Map<String, Object> getTriggerData() {
        return triggerData;
    }

    /**
     * @param triggerData the trigger data to use
     */
    public void setTriggerData(Map<String, Object> triggerData) {
        this.triggerData = triggerData;
    }

    /**
     * @return the trigger data to use
     */
    public Set<String> getSimulatedActionIds() {
        return simulatedActionIds;
    }

    /**
     * @param simulatedActionIds a list of action ids to run in simulations for this execution
     */
    public void addSimulatedActions(String ... simulatedActionIds) {
        for (String simulatedActionId : simulatedActionIds) {
            this.simulatedActionIds.add(simulatedActionId);
        }
    }

    /**
     * @return true if all actions should be simulated
     */
    public boolean isSimulateAllActions() {
        return this.simulatedActionIds.contains("_all");
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = null;
        if (id == null){
            validationException = ValidateActions.addValidationError("watch id is missing", validationException);
        }
        return validationException;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        id = in.readString();
        ignoreCondition = in.readBoolean();
        ignoreThrottle = in.readBoolean();
        recordInHistory = in.readBoolean();
        if (in.readBoolean()){
            alternativeInput = in.readMap();
        }
        if (in.readBoolean()) {
            triggerData = in.readMap();
        }
        long simulatedIdCount = in.readLong();
        for(long i = 0; i < simulatedIdCount; ++i) {
            simulatedActionIds.add(in.readString());
        }
    }


    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(id);
        out.writeBoolean(ignoreCondition);
        out.writeBoolean(ignoreThrottle);
        out.writeBoolean(recordInHistory);
        out.writeBoolean(alternativeInput != null);
        if (alternativeInput != null) {
            out.writeMap(alternativeInput);
        }
        out.writeBoolean(triggerData != null);
        if (triggerData != null){
            out.writeMap(triggerData);
        }
        out.writeLong(simulatedActionIds.size());
        for (String simulatedId : simulatedActionIds) {
            out.writeString(simulatedId);
        }
    }

    @Override
    public String toString() {
        return "execute {[" + WatchStore.INDEX + "][" + id + "]}";
    }
}
