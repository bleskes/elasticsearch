/*
 * ELASTICSEARCH CONFIDENTIAL
 *  __________________
 *
 * [2014] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.xpack.security.user;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Setting.Property;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.security.support.MetadataUtils;

import java.util.Collections;
import java.util.List;

import static org.elasticsearch.xpack.security.Security.setting;

/**
 * The user object for the anonymous user.
 */
public class AnonymousUser extends User {

    public static final String DEFAULT_ANONYMOUS_USERNAME = "_anonymous";
    public static final Setting<String> USERNAME_SETTING =
            new Setting<>(setting("authc.anonymous.username"), DEFAULT_ANONYMOUS_USERNAME, s -> s, Property.NodeScope);
    public static final Setting<List<String>> ROLES_SETTING =
            Setting.listSetting(setting("authc.anonymous.roles"), Collections.emptyList(), s -> s, Property.NodeScope);

    public AnonymousUser(Settings settings) {
        super(USERNAME_SETTING.get(settings), ROLES_SETTING.get(settings).toArray(Strings.EMPTY_ARRAY), null, null,
                MetadataUtils.DEFAULT_RESERVED_METADATA, isAnonymousEnabled(settings));
    }

    public static boolean isAnonymousEnabled(Settings settings) {
        return ROLES_SETTING.exists(settings) && ROLES_SETTING.get(settings).isEmpty() == false;
    }

    public static boolean isAnonymousUsername(String username, Settings settings) {
        // this is possibly the same check but we should not let anything use the default name either
        return USERNAME_SETTING.get(settings).equals(username) || DEFAULT_ANONYMOUS_USERNAME.equals(username);
    }

    public static void addSettings(List<Setting<?>> settingsList) {
        settingsList.add(USERNAME_SETTING);
        settingsList.add(ROLES_SETTING);
    }
}
