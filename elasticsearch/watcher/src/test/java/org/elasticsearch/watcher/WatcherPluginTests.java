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

package org.elasticsearch.watcher;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ESTestCase;

public class WatcherPluginTests extends ESTestCase {

    public void testValidAutoCreateIndex() {
        WatcherPlugin.validAutoCreateIndex(Settings.EMPTY);
        WatcherPlugin.validAutoCreateIndex(Settings.builder().put("action.auto_create_index", true).build());
        try {
            WatcherPlugin.validAutoCreateIndex(Settings.builder().put("action.auto_create_index", false).build());
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
        WatcherPlugin.validAutoCreateIndex(Settings.builder().put("action.auto_create_index", ".watches,.triggered_watches,.watch_history*").build());
        WatcherPlugin.validAutoCreateIndex(Settings.builder().put("action.auto_create_index", "*w*").build());
        WatcherPlugin.validAutoCreateIndex(Settings.builder().put("action.auto_create_index", ".w*,.t*").build());
        try {
            WatcherPlugin.validAutoCreateIndex(Settings.builder().put("action.auto_create_index", ".watches").build());
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
        try {
            WatcherPlugin.validAutoCreateIndex(Settings.builder().put("action.auto_create_index", ".triggered_watch").build());
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
        try {
            WatcherPlugin.validAutoCreateIndex(Settings.builder().put("action.auto_create_index", ".watch_history*").build());
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
    }

}
