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

package org.elasticsearch.xpack.notification.jira;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.xpack.common.http.HttpClient;
import org.elasticsearch.xpack.common.http.HttpMethod;
import org.elasticsearch.xpack.common.http.HttpProxy;
import org.elasticsearch.xpack.common.http.HttpRequest;
import org.elasticsearch.xpack.common.http.HttpResponse;
import org.elasticsearch.xpack.common.http.Scheme;
import org.elasticsearch.xpack.common.http.auth.basic.BasicAuth;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;

public class JiraAccount {

    /**
     * Default JIRA REST API path for create issues
     **/
    public static final String DEFAULT_PATH = "/rest/api/2/issue";

    static final String USER_SETTING = "user";
    static final String PASSWORD_SETTING = "password";
    static final String URL_SETTING = "url";
    static final String ISSUE_DEFAULTS_SETTING = "issue_defaults";
    static final String ALLOW_HTTP_SETTING = "allow_http";

    private final HttpClient httpClient;
    private final String name;
    private final String user;
    private final String password;
    private final URI url;
    private final Map<String, Object> issueDefaults;

    public JiraAccount(String name, Settings settings, HttpClient httpClient) {
        this.httpClient = httpClient;
        this.name = name;
        String url = settings.get(URL_SETTING);
        if (url == null) {
            throw requiredSettingException(name, URL_SETTING);
        }
        try {
            URI uri = new URI(url);
            Scheme protocol = Scheme.parse(uri.getScheme());
            if ((protocol == Scheme.HTTP) && (settings.getAsBoolean(ALLOW_HTTP_SETTING, false) == false)) {
                throw new SettingsException("invalid jira [" + name + "] account settings. unsecure scheme [" + protocol + "]");
            }
            this.url = uri;
        } catch (URISyntaxException | IllegalArgumentException e) {
            throw new SettingsException("invalid jira [" + name + "] account settings. invalid [" + URL_SETTING + "] setting", e);
        }
        this.user = settings.get(USER_SETTING);
        if (Strings.isEmpty(this.user)) {
            throw requiredSettingException(name, USER_SETTING);
        }
        this.password = settings.get(PASSWORD_SETTING);
        if (Strings.isEmpty(this.password)) {
            throw requiredSettingException(name, PASSWORD_SETTING);
        }
        this.issueDefaults = Collections.unmodifiableMap(settings.getAsSettings(ISSUE_DEFAULTS_SETTING).getAsStructuredMap());
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getDefaults() {
        return issueDefaults;
    }

    public JiraIssue createIssue(final Map<String, Object> fields, final HttpProxy proxy) throws IOException {
        HttpRequest request = HttpRequest.builder(url.getHost(), url.getPort())
                .scheme(Scheme.parse(url.getScheme()))
                .method(HttpMethod.POST)
                .path(DEFAULT_PATH)
                .jsonBody((builder, params) -> builder.field("fields", fields))
                .auth(new BasicAuth(user, password.toCharArray()))
                .proxy(proxy)
                .build();

        HttpResponse response = httpClient.execute(request);
        return JiraIssue.responded(name, fields, request, response);
    }

    private static SettingsException requiredSettingException(String account, String setting) {
        return new SettingsException("invalid jira [" + account + "] account settings. missing required [" + setting + "] setting");
    }
}
