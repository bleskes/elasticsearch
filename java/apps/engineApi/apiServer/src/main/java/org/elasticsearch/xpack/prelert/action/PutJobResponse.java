package org.elasticsearch.xpack.prelert.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prelert.job.JobDetails;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

public class PutJobResponse extends AcknowledgedResponse {

    private BytesReference response;

    public PutJobResponse(JobDetails jobDetails, ObjectMapper objectMapper) throws JsonProcessingException {
        super(true);
        this.response = new BytesArray(objectMapper.writeValueAsString(jobDetails));
    }

    public PutJobResponse() {
    }

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
