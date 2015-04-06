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

package org.elasticsearch.watcher.support.template;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.WatcherException;

import java.io.IOException;
import java.util.Map;

/**
 *
 */
public interface Template extends ToXContent {

    String render(Map<String, Object> model);

    interface Parser<T extends Template> {

        T parse(XContentParser parser) throws IOException, ParseException;

        class ParseException extends WatcherException {

            public ParseException(String msg) {
                super(msg);
            }

            public ParseException(String msg, Throwable cause) {
                super(msg, cause);
            }
        }
    }

    interface SourceBuilder extends ToXContent {
    }

    class InstanceSourceBuilder implements SourceBuilder {

        private final Template template;

        public InstanceSourceBuilder(Template template) {
            this.template = template;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return template.toXContent(builder, params);
        }
    }
}
