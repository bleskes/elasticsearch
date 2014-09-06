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

package org.elasticsearch.shield.authc.ldap;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

import static org.hamcrest.Matchers.*;

/**
 *
 */
public class LdapModuleTests extends ElasticsearchTestCase {

    @Test
    public void testEnabled() throws Exception {
        assertThat(LdapModule.enabled(ImmutableSettings.EMPTY), is(false));
        Settings settings = ImmutableSettings.builder()
                .put("shield.authc", false)
                .build();
        assertThat(LdapModule.enabled(settings), is(false));
        settings = ImmutableSettings.builder()
                .put("shield.authc.ldap.enabled", false)
                .build();
        assertThat(LdapModule.enabled(settings), is(false));
        settings = ImmutableSettings.builder()
                .put("shield.authc.ldap.enabled", true)
                .build();
        assertThat(LdapModule.enabled(settings), is(true));
    }
}
