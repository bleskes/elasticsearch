package org.elasticsearch.alerting;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;

public interface AlertAction {
    public String getActionName();
    public XContentBuilder toXContent(XContentBuilder builder) throws IOException;

    public boolean doAction(String alertName, AlertResult alert);
}
