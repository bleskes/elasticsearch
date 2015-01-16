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

package org.elasticsearch.shield.authc.support.ldap;

import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.shield.ShieldSettingsException;
import org.elasticsearch.shield.authc.RealmConfig;
import org.elasticsearch.shield.authc.support.SecuredString;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.Serializable;

import static org.hamcrest.Matchers.equalTo;

public class ConnectionFactoryTests extends ElasticsearchTestCase {

    @Test
    public void testConfigure_1ldaps() {
        String[] urls = new String[] { "ldaps://example.com:636" };

        ImmutableMap.Builder<String, Serializable> builder = ImmutableMap.builder();
        createConnectionFactoryWithoutHostnameVerification().configureJndiSSL(urls, builder);
        ImmutableMap<String, Serializable> settings = builder.build();
        assertThat((String) settings.get(ConnectionFactory.JAVA_NAMING_LDAP_FACTORY_SOCKET),
                equalTo(LdapSslSocketFactory.class.getName()));
    }

    @Test
    public void testConfigure_2ldaps() {
        String[] urls = new String[] { "ldaps://primary.example.com:636", "LDAPS://secondary.example.com:10636" };

        ImmutableMap.Builder<String, Serializable> builder = ImmutableMap.<String, Serializable>builder();
        createConnectionFactoryWithoutHostnameVerification().configureJndiSSL(urls, builder);
        ImmutableMap<String, Serializable> settings = builder.build();
        assertThat(settings.get(ConnectionFactory.JAVA_NAMING_LDAP_FACTORY_SOCKET), Matchers.<Serializable>equalTo(LdapSslSocketFactory.class.getName()));
    }

    @Test
    public void testConfigure_2ldap() {
        String[] urls = new String[] { "ldap://primary.example.com:392", "LDAP://secondary.example.com:10392" };

        ImmutableMap.Builder<String, Serializable> builder = ImmutableMap.<String, Serializable>builder();
        createConnectionFactoryWithoutHostnameVerification().configureJndiSSL(urls, builder);
        ImmutableMap<String, Serializable> settings = builder.build();
        assertThat(settings.get(ConnectionFactory.JAVA_NAMING_LDAP_FACTORY_SOCKET), equalTo(null));
    }

    @Test
    public void testConfigure_1ldapsWithHostnameVerification() {
        String[] urls = new String[] { "ldaps://example.com:636" };

        ImmutableMap.Builder<String, Serializable> builder = ImmutableMap.builder();
        createConnectionFactoryWithHostnameVerification().configureJndiSSL(urls, builder);
        ImmutableMap<String, Serializable> settings = builder.build();
        assertThat((String) settings.get(ConnectionFactory.JAVA_NAMING_LDAP_FACTORY_SOCKET),
                equalTo(HostnameVerifyingLdapSslSocketFactory.class.getName()));
    }

    @Test
    public void testConfigure_2ldapsWithHostnameVerification() {
        String[] urls = new String[] { "ldaps://primary.example.com:636", "LDAPS://secondary.example.com:10636" };

        ImmutableMap.Builder<String, Serializable> builder = ImmutableMap.<String, Serializable>builder();
        createConnectionFactoryWithHostnameVerification().configureJndiSSL(urls, builder);
        ImmutableMap<String, Serializable> settings = builder.build();
        assertThat(settings.get(ConnectionFactory.JAVA_NAMING_LDAP_FACTORY_SOCKET), Matchers.<Serializable>equalTo(HostnameVerifyingLdapSslSocketFactory.class.getName()));
    }

    @Test
    public void testConfigure_2ldapWithHostnameVerification() {
        String[] urls = new String[] { "ldap://primary.example.com:392", "LDAP://secondary.example.com:10392" };

        ImmutableMap.Builder<String, Serializable> builder = ImmutableMap.<String, Serializable>builder();
        createConnectionFactoryWithHostnameVerification().configureJndiSSL(urls, builder);
        ImmutableMap<String, Serializable> settings = builder.build();
        assertThat(settings.get(ConnectionFactory.JAVA_NAMING_LDAP_FACTORY_SOCKET), equalTo(null));
    }

    @Test(expected = ShieldSettingsException.class)
    public void testConfigure_1ldaps_1ldap() {
        String[] urls = new String[] { "LDAPS://primary.example.com:636", "ldap://secondary.example.com:392" };

        ImmutableMap.Builder<String, Serializable> builder = ImmutableMap.<String, Serializable>builder();
        createConnectionFactoryWithoutHostnameVerification().configureJndiSSL(urls, builder);
    }

    @Test(expected = ShieldSettingsException.class)
    public void testConfigure_1ldap_1ldaps() {
        String[] urls = new String[] { "ldap://primary.example.com:392", "ldaps://secondary.example.com:636" };

        ImmutableMap.Builder<String, Serializable> builder = ImmutableMap.<String, Serializable>builder();
        createConnectionFactoryWithoutHostnameVerification().configureJndiSSL(urls, builder);
    }

    private ConnectionFactory createConnectionFactoryWithoutHostnameVerification() {
        RealmConfig config = new RealmConfig("_name", ImmutableSettings.builder().put("hostname_verification", false).build());
        return new ConnectionFactory<AbstractLdapConnection>(AbstractLdapConnection.class, config) {
            @Override
            public AbstractLdapConnection open(String user, SecuredString password) {
                return null;
            }
        };
    }

    private ConnectionFactory createConnectionFactoryWithHostnameVerification() {
        return new ConnectionFactory<AbstractLdapConnection>(AbstractLdapConnection.class, new RealmConfig("_name")) {
            @Override
            public AbstractLdapConnection open(String user, SecuredString password) {
                return null;
            }
        };
    }
}
