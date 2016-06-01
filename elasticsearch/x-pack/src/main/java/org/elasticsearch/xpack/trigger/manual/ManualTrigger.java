/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */

package org.elasticsearch.xpack.trigger.manual;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.trigger.Trigger;

import java.io.IOException;

/**
 */
public class ManualTrigger implements Trigger {

    @Override
    public String type() {
        return ManualTriggerEngine.TYPE;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject().endObject();
    }

    static ManualTrigger parse(XContentParser parser) throws IOException{
        if (parser.currentToken() != XContentParser.Token.START_OBJECT){
            throw new ElasticsearchParseException("unable to parse [" + ManualTriggerEngine.TYPE +
                    "] trigger. expected a start object token, found [" + parser.currentToken() + "]");
        }
        XContentParser.Token token = parser.nextToken();
        if (token != XContentParser.Token.END_OBJECT) {
            throw new ElasticsearchParseException("unable to parse [" + ManualTriggerEngine.TYPE +
                    "] trigger. expected an empty object, but found an object with [" + token + "]");
        }
        return new ManualTrigger();
    }
}
