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

package org.elasticsearch.xpack.watcher.support;

import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.script.ScriptException;

import java.io.IOException;

import static org.elasticsearch.common.logging.LoggerMessageFormat.format;

/**
 *
 */
public class Exceptions {

    private Exceptions() {
    }

    public static IllegalArgumentException illegalArgument(String msg, Object... args) {
        return new IllegalArgumentException(format(msg, args));
    }

    public static IllegalArgumentException illegalArgument(String msg, Throwable cause, Object... args) {
        return new IllegalArgumentException(format(msg, args), cause);
    }

    public static IllegalStateException illegalState(String msg, Object... args) {
        return new IllegalStateException(format(msg, args));
    }

    public static IllegalStateException illegalState(String msg, Throwable cause, Object... args) {
        return new IllegalStateException(format(msg, args), cause);
    }

    public static IOException ioException(String msg, Object... args) {
        return new IOException(format(msg, args));
    }

    public static IOException ioException(String msg, Throwable cause, Object... args) {
        return new IOException(format(msg, args), cause);
    }


    //todo remove once ScriptException supports varargs
    public static ScriptException invalidScript(String msg, Object... args) {
        throw new ScriptException(format(msg, args));
    }

    //todo remove once SettingsException supports varargs
    public static SettingsException invalidSettings(String msg, Object... args) {
        throw new SettingsException(format(msg, args));
    }
}
