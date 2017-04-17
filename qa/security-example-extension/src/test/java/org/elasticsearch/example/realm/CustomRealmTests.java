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

package org.elasticsearch.example.realm;

import org.elasticsearch.common.settings.SecureString;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.env.Environment;
import org.elasticsearch.xpack.security.user.User;
import org.elasticsearch.xpack.security.authc.RealmConfig;
import org.elasticsearch.xpack.security.authc.support.UsernamePasswordToken;
import org.elasticsearch.test.ESTestCase;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class CustomRealmTests extends ESTestCase {
    public void testAuthenticate() {
        Settings globalSettings = Settings.builder().put("path.home", createTempDir()).build();
        CustomRealm realm = new CustomRealm(new RealmConfig("test", Settings.EMPTY, globalSettings, new Environment(globalSettings), new ThreadContext(globalSettings)));
        SecureString password = CustomRealm.KNOWN_PW.clone();
        UsernamePasswordToken token = new UsernamePasswordToken(CustomRealm.KNOWN_USER, password);
        User user = realm.authenticate(token);
        assertThat(user, notNullValue());
        assertThat(user.roles(), equalTo(CustomRealm.ROLES));
        assertThat(user.principal(), equalTo(CustomRealm.KNOWN_USER));
    }

    public void testAuthenticateBadUser() {
        Settings globalSettings = Settings.builder().put("path.home", createTempDir()).build();
        CustomRealm realm = new CustomRealm(new RealmConfig("test", Settings.EMPTY, globalSettings, new Environment(globalSettings), new ThreadContext(globalSettings)));
        SecureString password = CustomRealm.KNOWN_PW.clone();
        UsernamePasswordToken token = new UsernamePasswordToken(CustomRealm.KNOWN_USER + "1", password);
        User user = realm.authenticate(token);
        assertThat(user, nullValue());
    }
}
