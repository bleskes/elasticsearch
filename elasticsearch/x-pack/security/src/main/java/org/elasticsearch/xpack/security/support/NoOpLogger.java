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

package org.elasticsearch.xpack.security.support;

import org.elasticsearch.common.logging.ESLogger;

/**
 * A logger that doesn't log anything.
 */
public class NoOpLogger extends ESLogger {

    public static final ESLogger INSTANCE = new NoOpLogger();

    private NoOpLogger() {
        super(null, null);
    }

    @Override
    public String getPrefix() {
        return "";
    }

    @Override
    public String getName() {
        return "_no_op";
    }

    @Override
    public void setLevel(String level) {
    }

    @Override
    public String getLevel() {
        return "NONE";
    }

    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public boolean isInfoEnabled() {
        return false;
    }

    @Override
    public boolean isWarnEnabled() {
        return false;
    }

    @Override
    public boolean isErrorEnabled() {
        return false;
    }

    @Override
    public void trace(String msg, Object... params) {
    }

    @Override
    public void trace(String msg, Throwable cause, Object... params) {
    }

    @Override
    public void debug(String msg, Object... params) {
    }

    @Override
    public void debug(String msg, Throwable cause, Object... params) {
    }

    @Override
    public void info(String msg, Object... params) {
    }

    @Override
    public void info(String msg, Throwable cause, Object... params) {
    }

    @Override
    public void warn(String msg, Object... params) {
    }

    @Override
    public void warn(String msg, Throwable cause, Object... params) {
    }

    @Override
    public void error(String msg, Object... params) {
    }

    @Override
    public void error(String msg, Throwable cause, Object... params) {
    }
}
