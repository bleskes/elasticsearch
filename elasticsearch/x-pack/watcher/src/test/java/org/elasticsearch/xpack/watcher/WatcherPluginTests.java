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

package org.elasticsearch.xpack.watcher;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ESTestCase;

import static org.hamcrest.Matchers.containsString;

public class WatcherPluginTests extends ESTestCase {

    public void testValidAutoCreateIndex() {
        Watcher.validAutoCreateIndex(Settings.EMPTY);
        Watcher.validAutoCreateIndex(Settings.builder().put("action.auto_create_index", true).build());
        try {
            Watcher.validAutoCreateIndex(Settings.builder().put("action.auto_create_index", false).build());
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("[.watches,.triggered_watches,.watcher-history*]"));
        }
        Watcher.validAutoCreateIndex(Settings.builder().put("action.auto_create_index",
                ".watches,.triggered_watches,.watcher-history*").build());
        Watcher.validAutoCreateIndex(Settings.builder().put("action.auto_create_index", "*w*").build());
        Watcher.validAutoCreateIndex(Settings.builder().put("action.auto_create_index", ".w*,.t*").build());
        try {
            Watcher.validAutoCreateIndex(Settings.builder().put("action.auto_create_index", ".watches").build());
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("[.watches,.triggered_watches,.watcher-history*]"));
        }
        try {
            Watcher.validAutoCreateIndex(Settings.builder().put("action.auto_create_index", ".triggered_watch").build());
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("[.watches,.triggered_watches,.watcher-history*]"));
        }
        try {
            Watcher.validAutoCreateIndex(Settings.builder().put("action.auto_create_index", ".watcher-history*").build());
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("[.watches,.triggered_watches,.watcher-history*]"));
        }
    }

}
