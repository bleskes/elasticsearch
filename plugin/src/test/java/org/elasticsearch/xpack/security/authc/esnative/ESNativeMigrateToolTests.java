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

package org.elasticsearch.xpack.security.authc.esnative;

import com.google.common.base.Charsets;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.elasticsearch.cli.MockTerminal;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.test.NativeRealmIntegTestCase;
import org.elasticsearch.test.SecuritySettingsSource;
import org.elasticsearch.xpack.security.SecurityTemplateService;
import org.elasticsearch.xpack.security.client.SecurityClient;
import org.junit.BeforeClass;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.is;

/**
 * Integration tests for the {@code ESNativeMigrateTool}
 */
public class ESNativeMigrateToolTests extends NativeRealmIntegTestCase {

    // Randomly use SSL (or not)
    private static boolean useSSL;

    @BeforeClass
    public static void setSSL() {
        useSSL = randomBoolean();
    }

    @Override
    public Settings nodeSettings(int nodeOrdinal) {
        logger.info("--> use SSL? {}", useSSL);
        Settings s = Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(NetworkModule.HTTP_ENABLED.getKey(), true)
                .put("xpack.security.http.ssl.enabled", useSSL)
                .build();
        return s;
    }

    @Override
    protected boolean sslTransportEnabled() {
        return useSSL;
    }

    private Environment nodeEnvironment() throws Exception {
        return internalCluster().getInstances(Environment.class).iterator().next();
    }

    public void testRetrieveUsers() throws Exception {
        final Environment nodeEnvironment = nodeEnvironment();
        String home = Environment.PATH_HOME_SETTING.get(nodeEnvironment.settings());
        String conf = Environment.PATH_CONF_SETTING.get(nodeEnvironment.settings());
        SecurityClient c = new SecurityClient(client());
        logger.error("--> creating users");
        int numToAdd = randomIntBetween(1,10);
        Set<String> addedUsers = new HashSet(numToAdd);
        for (int i = 0; i < numToAdd; i++) {
            String uname = randomAlphaOfLength(5);
            c.preparePutUser(uname, "s3kirt".toCharArray(), "role1", "user").get();
            addedUsers.add(uname);
        }
        logger.error("--> waiting for .security index");
        ensureGreen(SecurityTemplateService.SECURITY_INDEX_NAME);

        MockTerminal t = new MockTerminal();
        String username = nodeClientUsername();
        String password = new String(nodeClientPassword().utf8Bytes(), Charsets.UTF_8);
        String url = getHttpURL();
        ESNativeRealmMigrateTool.MigrateUserOrRoles muor = new ESNativeRealmMigrateTool.MigrateUserOrRoles();
        Settings sslSettings =
                SecuritySettingsSource.getSSLSettingsForStore("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/testnode.jks",
                        "testnode");
        Settings settings = Settings.builder().put(sslSettings)
                .put("path.home", home)
                .put("path.conf", conf)
                .build();
        logger.error("--> retrieving users using URL: {}, home: {}", url, home);

        OptionParser parser = muor.getParser();
        OptionSet options = parser.parse("-u", username, "-p", password, "-U", url, "-Epath.conf=" + conf);
        logger.info("--> options: {}", options.asMap());
        Set<String> users = muor.getUsersThatExist(t, settings, new Environment(settings), options);
        logger.info("--> output: \n{}", t.getOutput());;
        for (String u : addedUsers) {
            assertThat("expected list to contain: " + u, users.contains(u), is(true));
        }
    }

    public void testRetrieveRoles() throws Exception {
        final Environment nodeEnvironment = nodeEnvironment();
        String home = Environment.PATH_HOME_SETTING.get(nodeEnvironment.settings());
        String conf = Environment.PATH_CONF_SETTING.get(nodeEnvironment.settings());
        SecurityClient c = new SecurityClient(client());
        logger.error("--> creating roles");
        int numToAdd = randomIntBetween(1,10);
        Set<String> addedRoles = new HashSet<>(numToAdd);
        for (int i = 0; i < numToAdd; i++) {
            String rname = randomAlphaOfLength(5);
            c.preparePutRole(rname)
                    .cluster("all", "none")
                    .runAs("root", "nobody")
                    .addIndices(new String[]{"index"}, new String[]{"read"},
                            new String[]{"body", "title"}, null, new BytesArray("{\"query\": {\"match_all\": {}}}"))
                    .get();
            addedRoles.add(rname);
        }
        logger.error("--> waiting for .security index");
        ensureGreen(SecurityTemplateService.SECURITY_INDEX_NAME);

        MockTerminal t = new MockTerminal();
        String username = nodeClientUsername();
        String password = new String(nodeClientPassword().utf8Bytes(), Charsets.UTF_8);
        String url = getHttpURL();
        ESNativeRealmMigrateTool.MigrateUserOrRoles muor = new ESNativeRealmMigrateTool.MigrateUserOrRoles();
        Settings sslSettings =
                SecuritySettingsSource.getSSLSettingsForStore("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/testclient.jks",
                        "testclient");
        Settings settings = Settings.builder().put(sslSettings)
                .put("path.home", home)
                .put("path.conf", conf)
                .build();
        logger.error("--> retrieving roles using URL: {}, home: {}", url, home);

        OptionParser parser = muor.getParser();
        OptionSet options = parser.parse("-u", username, "-p", password, "-U", url, "-Epath.conf", conf);
        Set<String> roles = muor.getRolesThatExist(t, settings, new Environment(settings), options);
        logger.info("--> output: \n{}", t.getOutput());;
        for (String r : addedRoles) {
            assertThat("expected list to contain: " + r, roles.contains(r), is(true));
        }
    }
}
