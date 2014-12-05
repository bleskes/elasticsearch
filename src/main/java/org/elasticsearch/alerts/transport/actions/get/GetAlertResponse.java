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

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 * The GetAlertResponse the response class wraps a GetResponse containing the alert source
 */
public class GetAlertResponse extends ActionResponse {

    private GetResponse getResponse;

    public GetAlertResponse() {

    }

    public GetAlertResponse(GetResponse getResponse) {
        this.getResponse = getResponse;
    }

    /**
     * The GetResponse containing the alert source
     * @param getResponse
     */
    public void getResponse(GetResponse getResponse) {
        this.getResponse = getResponse;
    }

    public GetResponse getResponse() {
        return this.getResponse;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        if (in.readBoolean()) {
            getResponse = GetResponse.readGetResponse(in);
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeBoolean(getResponse != null);
        if (getResponse != null) {
            getResponse.writeTo(out);
        }
    }
}
