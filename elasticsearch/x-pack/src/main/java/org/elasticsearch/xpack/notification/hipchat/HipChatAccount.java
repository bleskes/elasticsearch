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

package org.elasticsearch.xpack.notification.hipchat;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.support.http.HttpClient;
import org.elasticsearch.watcher.support.text.TextTemplateEngine;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 *
 */
public abstract class HipChatAccount  {

    public static final String AUTH_TOKEN_SETTING = "auth_token";
    public static final String ROOM_SETTING = HipChatMessage.Field.ROOM.getPreferredName();
    public static final String DEFAULT_ROOM_SETTING = "message_defaults." + HipChatMessage.Field.ROOM.getPreferredName();
    public static final String DEFAULT_USER_SETTING = "message_defaults." + HipChatMessage.Field.USER.getPreferredName();
    public static final String DEFAULT_FROM_SETTING = "message_defaults." + HipChatMessage.Field.FROM.getPreferredName();
    public static final String DEFAULT_FORMAT_SETTING = "message_defaults." + HipChatMessage.Field.FORMAT.getPreferredName();
    public static final String DEFAULT_COLOR_SETTING = "message_defaults." + HipChatMessage.Field.COLOR.getPreferredName();
    public static final String DEFAULT_NOTIFY_SETTING = "message_defaults." + HipChatMessage.Field.NOTIFY.getPreferredName();

    protected final ESLogger logger;
    protected final String name;
    protected final Profile profile;
    protected final HipChatServer server;
    protected final HttpClient httpClient;
    protected final String authToken;

    protected HipChatAccount(String name, Profile profile, Settings settings, HipChatServer defaultServer, HttpClient httpClient,
                             ESLogger logger) {
        this.name = name;
        this.profile = profile;
        this.server = new HipChatServer(settings, defaultServer);
        this.httpClient = httpClient;
        this.authToken = settings.get(AUTH_TOKEN_SETTING);
        if (this.authToken == null || this.authToken.length() == 0) {
            throw new SettingsException("hipchat account [" + name + "] missing required [" + AUTH_TOKEN_SETTING + "] setting");
        }
        this.logger = logger;
    }

    public abstract String type();

    public abstract void validateParsedTemplate(String watchId, String actionId, HipChatMessage.Template message) throws SettingsException;

    public abstract HipChatMessage render(String watchId, String actionId, TextTemplateEngine engine, HipChatMessage.Template template,
                                          Map<String, Object> model);

    public abstract SentMessages send(HipChatMessage message);

    public enum Profile implements ToXContent {

        V1() {
            @Override
            HipChatAccount createAccount(String name, Settings settings, HipChatServer defaultServer, HttpClient httpClient,
                                         ESLogger logger) {
                return new V1Account(name, settings, defaultServer, httpClient, logger);
            }
        },
        INTEGRATION() {
            @Override
            HipChatAccount createAccount(String name, Settings settings, HipChatServer defaultServer, HttpClient httpClient,
                                         ESLogger logger) {
                return new IntegrationAccount(name, settings, defaultServer, httpClient, logger);
            }
        },
        USER() {
            @Override
            HipChatAccount createAccount(String name, Settings settings, HipChatServer defaultServer, HttpClient httpClient,
                                         ESLogger logger) {
                return new UserAccount(name, settings, defaultServer, httpClient, logger);
            }
        };

        abstract HipChatAccount createAccount(String name, Settings settings, HipChatServer defaultServer, HttpClient httpClient,
                                              ESLogger logger);

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return builder.value(name().toLowerCase(Locale.ROOT));
        }

        public String value() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Profile parse(XContentParser parser) throws IOException {
            return Profile.valueOf(parser.text().toUpperCase(Locale.ROOT));
        }

        public static Profile resolve(String value, Profile defaultValue) {
            if (value == null) {
                return defaultValue;
            }
            return Profile.valueOf(value.toUpperCase(Locale.ROOT));
        }

        public static Profile resolve(Settings settings, String setting, Profile defaultValue) {
            return resolve(settings.get(setting), defaultValue);
        }

        public static boolean validate(String value) {
            try {
                Profile.valueOf(value.toUpperCase(Locale.ROOT));
                return true;
            } catch (IllegalArgumentException ilae) {
                return false;
            }
        }
    }
}
