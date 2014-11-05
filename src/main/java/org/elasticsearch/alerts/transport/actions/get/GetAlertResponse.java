package org.elasticsearch.alerts.transport.actions.get;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.alerts.Alert;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 */
public class GetAlertResponse extends ActionResponse {
    private boolean found = false;
    private Alert alert = null;

    public GetAlertResponse() {

    }

    public boolean found() {
        return this.found;
    }

    public void found(boolean found) {
        this.found = found;
    }

    public Alert alert() {
        return alert;
    }

    public void alert(Alert alert){
        this.alert = alert;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        found = in.readBoolean();
        if (found) {
            alert = new Alert();
            alert.readFrom(in);
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeBoolean(found);
        if (found && alert != null){
            alert.writeTo(out);
        }
    }
}
