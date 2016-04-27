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

import org.elasticsearch.common.settings.Settings;

/**
 *
 */
public abstract class ThreadPoolSettingsBuilder<B extends ThreadPoolSettingsBuilder> {

    public static Same same(String name) {
        return new Same(name);
    }

    protected final String name;
    private final Settings.Builder builder = Settings.builder();

    protected ThreadPoolSettingsBuilder(String name, String type) {
        this.name = name;
        put("type", type);
    }

    public Settings build() {
        return builder.build();
    }

    protected B put(String setting, Object value) {
        builder.put("threadpool." + name + "." + setting, value);
        return (B) this;
    }

    protected B put(String setting, int value) {
        builder.put("threadpool." + name + "." + setting, value);
        return (B) this;
    }

    public static class Same extends ThreadPoolSettingsBuilder<Same> {
        public Same(String name) {
            super(name, "same");
        }
    }

    public static class Fixed extends ThreadPoolSettingsBuilder<Fixed> {

        public Fixed(String name) {
            super(name, "fixed");
        }

        public Fixed size(int size) {
            return put("size", size);
        }

        public Fixed queueSize(int queueSize) {
            return put("queue_size", queueSize);
        }
    }

}
