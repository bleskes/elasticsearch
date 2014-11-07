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

package org.elasticsearch.alerts.transport.actions.delete;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 */
public class DeleteAlertResponse extends ActionResponse {

    private DeleteResponse deleteResponse;

    public DeleteAlertResponse() {
    }

    public DeleteAlertResponse(@Nullable DeleteResponse deleteResponse) {
        this.deleteResponse = deleteResponse;
    }

    public DeleteResponse deleteResponse() {
        return deleteResponse;
    }

    public void deleteResponse(DeleteResponse deleteResponse){
        this.deleteResponse = deleteResponse;
    }


    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        deleteResponse = new DeleteResponse();
        if (in.readBoolean()) {
            deleteResponse.readFrom(in);
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeBoolean(deleteResponse != null);
        if (deleteResponse != null) {
            deleteResponse.writeTo(out);
        }
    }
}
