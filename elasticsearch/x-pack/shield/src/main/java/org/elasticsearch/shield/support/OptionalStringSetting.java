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

import java.util.Optional;
import java.util.function.Function;

public class OptionalStringSetting {

    private OptionalStringSetting() {}

    public static Setting<Optional<String>> create(String key, Property... properties) {
        return create(key, s -> null, properties);
    }

    public static Setting<Optional<String>> create(String key, Function<Settings, String> defaultValue, Property... properties) {
        return new Setting<>(key, defaultValue, Optional::ofNullable, properties);
    }

    public static Setting<Optional<String>> create(String key, Setting<Optional<String>> fallback, Property... properties) {
        return new Setting<>(key, fallback, Optional::ofNullable, properties);
    }
}
