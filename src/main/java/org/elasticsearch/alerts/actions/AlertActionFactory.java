package org.elasticsearch.alerts.actions;

import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;

public interface AlertActionFactory {

    AlertAction createAction(XContentParser parser) throws IOException;

}
