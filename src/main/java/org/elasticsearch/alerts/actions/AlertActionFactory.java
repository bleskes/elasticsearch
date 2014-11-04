package org.elasticsearch.alerts.actions;


import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.io.stream.StreamInput;

import java.io.IOException;

public interface AlertActionFactory {

    AlertAction createAction(XContentParser parser) throws IOException;


    AlertAction readFrom(StreamInput in) throws IOException;

}
