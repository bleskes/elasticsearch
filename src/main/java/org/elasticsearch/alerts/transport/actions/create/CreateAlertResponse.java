package org.elasticsearch.alerts.transport.actions.create;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 */
public class CreateAlertResponse extends ActionResponse {
    private boolean success;

    public CreateAlertResponse(boolean success) {
        this.success = success;
    }

    public CreateAlertResponse() {
        this.success = success;
    }


    public boolean success() {
        return success;
    }

    public void success(boolean success) {
        this.success = success;
    }


    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeBoolean(success);
    }


    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        success = in.readBoolean();
    }
}
