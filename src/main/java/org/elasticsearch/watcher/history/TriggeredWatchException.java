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

package org.elasticsearch.watcher.history;

import org.elasticsearch.watcher.WatcherException;

public class TriggeredWatchException extends WatcherException {

    public TriggeredWatchException(String msg, Object... args) {
        super(msg, args);
    }

    public TriggeredWatchException(String msg, Throwable cause, Object... args) {
        super(msg, cause, args);
    }

}
