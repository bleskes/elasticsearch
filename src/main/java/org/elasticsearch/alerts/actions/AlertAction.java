package org.elasticsearch.alerts.actions;

import org.elasticsearch.alerts.Alert;
import org.elasticsearch.alerts.triggers.TriggerResult;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

public interface AlertAction extends ToXContent {

    public String getActionName();

    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException;

    public void writeTo(StreamOutput out) throws IOException;
    public void readFrom(StreamInput in) throws IOException;

    public boolean doAction(Alert alert, TriggerResult result);


}
