package org.elasticsearch.prelert.action;

import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.support.master.MasterNodeRequest;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

public class PutJobRequest extends MasterNodeRequest<PutJobRequest> {

    private BytesReference jobConfiguration;
    private boolean overwrite;

    public BytesReference getJobConfiguration() {
        return jobConfiguration;
    }

    public void setJobConfiguration(BytesReference jobConfiguration) {
        this.jobConfiguration = jobConfiguration;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        jobConfiguration = in.readBytesReference();
        overwrite = in.readBoolean();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeBytesReference(jobConfiguration);
        out.writeBoolean(overwrite);
    }
}
