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

package org.elasticsearch.xpack.prelert.action.list;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.StatusToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.xpack.prelert.utils.SingleDocument;

import java.io.IOException;

public class GetListResponse extends AcknowledgedResponse implements StatusToXContent {

    private SingleDocument response;

    public GetListResponse(SingleDocument document) throws JsonProcessingException {
        super(true);
        this.response = document;
    }

    public GetListResponse() {
    }

    public SingleDocument getResponse() {
        return response;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        response = new SingleDocument(in.readString(), in.readBytesReference());
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(response.getType());
        out.writeBytesReference(response.getDocument());
    }

    @Override
    public RestStatus status() {
        return response.status();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return response.toXContent(builder, params);
    }
}