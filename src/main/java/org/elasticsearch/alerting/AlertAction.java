package org.elasticsearch.alerting;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

public interface AlertAction extends ToXContent {
    public String getActionName();
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException;

    public boolean doAction(String alertName, AlertResult alert);
}
