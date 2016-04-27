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

package org.elasticsearch.xpack.watcher.actions.logging;

import org.elasticsearch.common.SuppressLoggerChecks;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Locale;

/**
 *
 */
public enum LoggingLevel implements ToXContent {

    ERROR() {
        @Override
        @SuppressLoggerChecks(reason = "logger delegation")
        void log(ESLogger logger, String text) {
            logger.error(text);
        }
    },
    WARN() {
        @Override
        @SuppressLoggerChecks(reason = "logger delegation")
        void log(ESLogger logger, String text) {
            logger.warn(text);
        }
    },
    INFO() {
        @Override
        @SuppressLoggerChecks(reason = "logger delegation")
        void log(ESLogger logger, String text) {
            logger.info(text);
        }
    },
    DEBUG() {
        @Override
        @SuppressLoggerChecks(reason = "logger delegation")
        void log(ESLogger logger, String text) {
            logger.debug(text);
        }
    },
    TRACE() {
        @Override
        @SuppressLoggerChecks(reason = "logger delegation")
        void log(ESLogger logger, String text) {
            logger.trace(text);
        }
    };

    abstract void log(ESLogger logger, String text);


    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.value(name().toLowerCase(Locale.ROOT));
    }
}
