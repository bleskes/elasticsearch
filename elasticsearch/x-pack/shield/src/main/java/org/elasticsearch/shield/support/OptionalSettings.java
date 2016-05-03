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

package org.elasticsearch.shield.support;

import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Setting.Property;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;

import java.util.Optional;
import java.util.function.Function;

public class OptionalSettings {

    private OptionalSettings() {}

    public static Setting<Optional<Integer>> createInt(String key, Property... properties) {
        return new Setting<>(key, s -> null, s -> {
            if (s != null) {
                return Optional.of(Integer.parseInt(s));
            } else {
                return Optional.<Integer>ofNullable(null);
            }
        }, properties);
    }

    public static Setting<Optional<String>> createString(String key, Property... properties) {
        return createString(key, s -> null, properties);
    }

    public static Setting<Optional<String>> createString(String key, Function<Settings, String> defaultValue, Property... properties) {
        return new Setting<>(key, defaultValue, Optional::ofNullable, properties);
    }

    public static Setting<Optional<String>> createString(String key, Setting<Optional<String>> fallback, Property... properties) {
        return new Setting<>(key, fallback, Optional::ofNullable, properties);
    }

    public static Setting<Optional<TimeValue>> createTimeValue(String key, Property... properties) {
        return new Setting<>(key, s-> null, s -> {
            if (s != null) {
                return Optional.of(TimeValue.parseTimeValue(s, key));
            } else {
                return Optional.<TimeValue>ofNullable(null);
            }
        }, properties);
    }
}
