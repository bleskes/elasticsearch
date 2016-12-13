/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2016] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.xpack.security.authc;

import org.elasticsearch.common.settings.AbstractScopedSettings;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.extensions.XPackExtension;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.elasticsearch.common.Strings.isNullOrEmpty;
import static org.elasticsearch.xpack.security.Security.setting;

/**
 * Configures the {@link Setting#groupSetting(String, Consumer, Setting.Property...) group setting} for security
 * {@link Realm realms}, with validation according to the realm type.
 * <p>
 * The allowable settings for a given realm are dependent on the {@link Realm#type() realm type}, so it is not possible
 * to simply provide a list of {@link Setting} objects and rely on the global setting validation (e.g. A custom realm-type might
 * define a setting with the same logical key as an internal realm-type, but a different data type).
 * </p> <p>
 * Instead, realm configuration relies on the <code>validator</code> parameter to
 * {@link Setting#groupSetting(String, Consumer, Setting.Property...)} in order to validate each realm in a way that respects the
 * declared <code>type</code>.
 * Internally, this validation delegates to {@link AbstractScopedSettings#validate(Settings)} so that validation is reasonably aligned
 * with the way we validate settings globally.
 * </p>
 * <p>
 * The allowable settings for each realm-type are determined by calls to {@link InternalRealms#getSettings()} and
 * {@link XPackExtension#getRealmSettings()}
 */
public class RealmSettings {

    public static final String PREFIX = setting("authc.realms.");

    static final Setting<String> TYPE_SETTING = Setting.simpleString("type", Setting.Property.NodeScope);
    static final Setting<Boolean> ENABLED_SETTING = Setting.boolSetting("enabled", true, Setting.Property.NodeScope);
    static final Setting<Integer> ORDER_SETTING = Setting.intSetting("order", Integer.MAX_VALUE, Setting.Property.NodeScope);

    /**
     * Add the {@link Setting} configuration for <em>all</em> realms to the provided list.
     */
    public static void addSettings(List<Setting<?>> settingsList, List<XPackExtension> extensions) {
        settingsList.add(getGroupSetting(extensions));
    }

    /**
     * Extract the child {@link Settings} for the {@link #PREFIX realms prefix}.
     * The top level names in the returned <code>Settings</code> will be the names of the configured realms.
     */
    public static Settings get(Settings settings) {
        return settings.getByPrefix(RealmSettings.PREFIX);
    }

    /**
     * Convert the child {@link Setting} for the provided realm into a fully scoped key for use in an error message.
     * @see #PREFIX
     */
    public static String getFullSettingKey(RealmConfig realm, Setting<?> setting) {
        return getFullSettingKey(realm.name(), setting);
    }

    /**
     * @see #getFullSettingKey(RealmConfig, Setting)
     */
    public static String getFullSettingKey(RealmConfig realm, String subKey) {
        return getFullSettingKey(realm.name(), subKey);
    }

    private static String getFullSettingKey(String name, Setting<?> setting) {
        return getFullSettingKey(name, setting.getKey());
    }

    private static String getFullSettingKey(String name, String subKey) {
        return PREFIX + name + "." + subKey;
    }

    private static Setting<Settings> getGroupSetting(List<XPackExtension> extensions) {
        return Setting.groupSetting(PREFIX, getSettingsValidator(extensions), Setting.Property.NodeScope);
    }

    private static Consumer<Settings> getSettingsValidator(List<XPackExtension> extensions) {
        final Map<String, Set<Setting<?>>> childSettings = new HashMap<>(InternalRealms.getSettings());
        if (extensions != null) {
            extensions.forEach(ext -> {
                final Map<String, Set<Setting<?>>> extSettings = ext.getRealmSettings();
                extSettings.keySet().stream().filter(childSettings::containsKey).forEach(type -> {
                    throw new IllegalArgumentException("duplicate realm type " + type);
                });
                childSettings.putAll(extSettings);
            });
        }
        childSettings.forEach(RealmSettings::verify);
        return validator(childSettings);
    }

    private static void verify(String type, Set<Setting<?>> settings) {
        Set<String> keys = new HashSet<>();
        settings.forEach(setting -> {
            final String key = setting.getKey();
            if (keys.contains(key)) {
                throw new IllegalArgumentException("duplicate setting for key " + key + " in realm type " + type);
            }
            keys.add(key);
            if (setting.getProperties().contains(Setting.Property.NodeScope) == false) {
                throw new IllegalArgumentException("setting " + key + " in realm type " + type + " does not have NodeScope");
            }
        });
    }

    private static Consumer<Settings> validator(Map<String, Set<Setting<?>>> validSettings) {
        return (settings) -> settings.names().forEach(n -> validateRealm(n, settings.getAsSettings(n), validSettings));
    }

    private static void validateRealm(String name, Settings settings, Map<String, Set<Setting<?>>> validSettings) {
        final String type = TYPE_SETTING.get(settings);
        if (isNullOrEmpty(type)) {
            throw new IllegalArgumentException("missing realm type [" + getFullSettingKey(name, TYPE_SETTING) + "] for realm");
        }
        validateRealm(name, type, settings, validSettings.get(type));
    }

    private static void validateRealm(String name, String type, Settings settings, Set<Setting<?>> validSettings) {
        if (validSettings == null) {
            // For backwards compatibility, we assume that is we don't know the valid settings for a realm.type then everything
            // is valid. Ideally we would reject these, but XPackExtension doesn't enforce that realm-factories and realm-settings are
            // perfectly aligned
            return;
        }
        Set<Setting<?>> settingSet = new HashSet<>(validSettings);
        settingSet.add(TYPE_SETTING);
        settingSet.add(ENABLED_SETTING);
        settingSet.add(ORDER_SETTING);
        final AbstractScopedSettings validator = new AbstractScopedSettings(settings, settingSet, Setting.Property.NodeScope) { };
        try {
            validator.validate(settings);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("incorrect configuration for realm [" + getFullSettingKey(name, "")
                    + "] of type " + type, e);
        }
    }

    private RealmSettings() {
    }
}
