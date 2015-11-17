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

package org.elasticsearch.integration;

import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.ShieldPlugin;
import org.elasticsearch.shield.authc.support.Hasher;
import org.elasticsearch.shield.authc.support.SecuredString;
import org.elasticsearch.test.ShieldIntegTestCase;

import static org.elasticsearch.shield.authc.support.UsernamePasswordToken.BASIC_AUTH_HEADER;
import static org.elasticsearch.shield.authc.support.UsernamePasswordToken.basicAuthHeaderValue;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.hamcrest.Matchers.equalTo;

/**
 */
public class IndicesPermissionsWithAliasesWildcardsAndRegexsTests extends ShieldIntegTestCase {

    protected static final SecuredString USERS_PASSWD = new SecuredString("change_me".toCharArray());
    protected static final String USERS_PASSWD_HASHED = new String(Hasher.BCRYPT.hash(new SecuredString("change_me".toCharArray())));

    @Override
    protected String configUsers() {
        return super.configUsers() +
                "user1:" + USERS_PASSWD_HASHED + "\n";
    }

    @Override
    protected String configUsersRoles() {
        return super.configUsersRoles() +
                "role1:user1\n";
    }

    @Override
    protected String configRoles() {
        return super.configRoles() +
                "\nrole1:\n" +
                "  cluster: all\n" +
                "  indices:\n" +
                "     't*':\n" +
                "        privileges: ALL\n" +
                "        fields: field1\n" +
                "     'my_alias':\n" +
                "        privileges: ALL\n" +
                "        fields: field2\n" +
                "     '/an_.*/':\n" +
                "        privileges: ALL\n" +
                "        fields: field3\n";
    }

    @Override
    public Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(ShieldPlugin.DLS_FLS_ENABLED_SETTING, true)
                .build();
    }

    public void testResolveWildcardsRegexs() throws Exception {
        assertAcked(client().admin().indices().prepareCreate("test")
                        .addMapping("type1", "field1", "type=string", "field2", "type=string")
                        .addAlias(new Alias("my_alias"))
                        .addAlias(new Alias("an_alias"))
        );
        client().prepareIndex("test", "type1", "1").setSource("field1", "value1", "field2", "value2",  "field3", "value3")
                .setRefresh(true)
                .get();

        GetResponse getResponse = client().prepareGet("test", "type1", "1")
                .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("user1", USERS_PASSWD))
                .get();
        assertThat(getResponse.getSource().size(), equalTo(1));
        assertThat((String) getResponse.getSource().get("field1"), equalTo("value1"));

        getResponse = client().prepareGet("my_alias", "type1", "1")
                .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("user1", USERS_PASSWD))
                .get();
        assertThat(getResponse.getSource().size(), equalTo(1));
        assertThat((String) getResponse.getSource().get("field2"), equalTo("value2"));

        getResponse = client().prepareGet("an_alias", "type1", "1")
                .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("user1", USERS_PASSWD))
                .get();
        assertThat(getResponse.getSource().size(), equalTo(1));
        assertThat((String) getResponse.getSource().get("field3"), equalTo("value3"));
    }

}
