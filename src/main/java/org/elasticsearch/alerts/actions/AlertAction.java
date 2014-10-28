package org.elasticsearch.alerts.actions;

import org.elasticsearch.alerts.Alert;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

public interface AlertAction extends ToXContent {

    public String getActionName();

    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException;

    public boolean doAction(Alert alert, AlertActionEntry actionEntry);
}
