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
package org.elasticsearch.xpack.prelert.action.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.persistence.QueryPage;

import java.io.IOException;

public class GetJobsResponse extends AcknowledgedResponse {

    private BytesReference response;

    public GetJobsResponse(QueryPage<JobDetails> jobsPage, ObjectMapper objectMapper) throws JsonProcessingException {
        super(true);
        this.response = new BytesArray(objectMapper.writeValueAsString(jobsPage));
    }

    public GetJobsResponse() {}

    public BytesReference getResponse() {
        return response;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        response = in.readBytesReference();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeBytesReference(response);
    }
}
