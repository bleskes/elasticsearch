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

package org.elasticsearch.shield.authz.store;

import org.elasticsearch.common.base.Charsets;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.shield.audit.logfile.CapturingLogger;
import org.elasticsearch.shield.authc.support.RefreshListener;
import org.elasticsearch.shield.authz.Permission;
import org.elasticsearch.shield.authz.Privilege;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;

/**
 *
 */
public class FileRolesStoreTests extends ElasticsearchTestCase {

    @Test
    public void testParseFile() throws Exception {
        Path path = Paths.get(getClass().getResource("roles.yml").toURI());
        Map<String, Permission.Global.Role> roles = FileRolesStore.parseFile(path, logger);
        assertThat(roles, notNullValue());
        assertThat(roles.size(), is(4));

        Permission.Global.Role role = roles.get("role1");
        assertThat(role, notNullValue());
        assertThat(role.name(), equalTo("role1"));
        assertThat(role.cluster(), notNullValue());
        assertThat(role.cluster().privilege(), is(Privilege.Cluster.ALL));
        assertThat(role.indices(), notNullValue());
        assertThat(role.indices().groups(), notNullValue());
        assertThat(role.indices().groups().length, is(2));

        Permission.Global.Indices.Group group = role.indices().groups()[0];
        assertThat(group.indices(), notNullValue());
        assertThat(group.indices().length, is(2));
        assertThat(group.indices()[0], equalTo("idx1"));
        assertThat(group.indices()[1], equalTo("idx2"));
        assertThat(group.privilege(), notNullValue());
        assertThat(group.privilege(), is(Privilege.Index.READ));

        group = role.indices().groups()[1];
        assertThat(group.indices(), notNullValue());
        assertThat(group.indices().length, is(1));
        assertThat(group.indices()[0], equalTo("idx3"));
        assertThat(group.privilege(), notNullValue());
        assertThat(group.privilege(), is(Privilege.Index.CRUD));

        role = roles.get("role2");
        assertThat(role, notNullValue());
        assertThat(role.name(), equalTo("role2"));
        assertThat(role.cluster(), notNullValue());
        assertThat(role.cluster().privilege(), is(Privilege.Cluster.ALL)); // MONITOR is collapsed into ALL
        assertThat(role.indices(), notNullValue());
        assertThat(role.indices(), is(Permission.Indices.Core.NONE));

        role = roles.get("role3");
        assertThat(role, notNullValue());
        assertThat(role.name(), equalTo("role3"));
        assertThat(role.cluster(), notNullValue());
        assertThat(role.cluster(), is(Permission.Cluster.Core.NONE));
        assertThat(role.indices(), notNullValue());
        assertThat(role.indices().groups(), notNullValue());
        assertThat(role.indices().groups().length, is(1));

        group = role.indices().groups()[0];
        assertThat(group.indices(), notNullValue());
        assertThat(group.indices().length, is(1));
        assertThat(group.indices()[0], equalTo("/.*_.*/"));
        assertThat(group.privilege(), notNullValue());
        assertThat(group.privilege().isAlias(Privilege.Index.union(Privilege.Index.READ, Privilege.Index.WRITE)), is(true));

        role = roles.get("role4");
        assertThat(role, notNullValue());
        assertThat(role.name(), equalTo("role4"));
        assertThat(role.cluster(), notNullValue());
        assertThat(role.cluster(), is(Permission.Cluster.Core.NONE));
        assertThat(role.indices(), is(Permission.Indices.Core.NONE));
    }

    /**
     * This test is mainly to make sure we can read the default roles.yml config
     */
    @Test
    public void testDefaultRolesFile() throws Exception {
        Path path = Paths.get(getClass().getResource("default_roles.yml").toURI());
        Map<String, Permission.Global.Role> roles = FileRolesStore.parseFile(path, logger);
        assertThat(roles, notNullValue());
        assertThat(roles.size(), is(8));

        assertThat(roles, hasKey("admin"));
        assertThat(roles, hasKey("power_user"));
        assertThat(roles, hasKey("user"));
        assertThat(roles, hasKey("kibana3"));
        assertThat(roles, hasKey("kibana4"));
        assertThat(roles, hasKey("logstash"));
        assertThat(roles, hasKey("marvel_user"));
        assertThat(roles, hasKey("marvel_agent"));
    }

    @Test
    public void testAutoReload() throws Exception {
        ThreadPool threadPool = null;
        ResourceWatcherService watcherService = null;
        try {
            Path users = Paths.get(getClass().getResource("roles.yml").toURI());
            Path tmp = newTempFile().toPath();
            Files.copy(users, Files.newOutputStream(tmp));

            Settings settings = ImmutableSettings.builder()
                    .put("watcher.interval.high", "500ms")
                    .put("shield.authz.store.files.roles", tmp.toAbsolutePath())
                    .build();

            Environment env = new Environment(settings);
            threadPool = new ThreadPool("test");
            watcherService = new ResourceWatcherService(settings, threadPool);
            final CountDownLatch latch = new CountDownLatch(1);
            FileRolesStore store = new FileRolesStore(settings, env, watcherService, new RefreshListener() {
                @Override
                public void onRefresh() {
                    latch.countDown();
                }
            });
            store.start();

            Permission.Global.Role role = store.role("role1");
            assertThat(role, notNullValue());
            role = store.role("role5");
            assertThat(role, nullValue());

            watcherService.start();

            try (BufferedWriter writer = Files.newBufferedWriter(tmp, Charsets.UTF_8, StandardOpenOption.APPEND)) {
                writer.newLine();
                writer.newLine();
                writer.newLine();
                writer.append("role5:").append(System.lineSeparator());
                writer.append("  cluster: 'MONITOR'");
            }

            if (!latch.await(5, TimeUnit.SECONDS)) {
                fail("Waited too long for the updated file to be picked up");
            }

            role = store.role("role5");
            assertThat(role, notNullValue());
            assertThat(role.name(), equalTo("role5"));
            assertThat(role.cluster().check("cluster:monitor/foo/bar"), is(true));
            assertThat(role.cluster().check("cluster:admin/foo/bar"), is(false));

        } finally {
            if (watcherService != null) {
                watcherService.stop();
            }
            terminate(threadPool);
        }
    }

    @Test
    public void testThatEmptyFileDoesNotResultInLoop() throws Exception {
        File file = newTempFile();
        com.google.common.io.Files.write("#".getBytes(Charsets.UTF_8), file);
        Map<String, Permission.Global.Role> roles = FileRolesStore.parseFile(file.toPath(), logger);
        assertThat(roles.keySet(), is(empty()));
    }

    @Test
    public void testThatInvalidRoleDefinitions() throws Exception {
        Path path = Paths.get(getClass().getResource("invalid_roles.yml").toURI());
        CapturingLogger logger = new CapturingLogger(CapturingLogger.Level.ERROR);
        Map<String, Permission.Global.Role> roles = FileRolesStore.parseFile(path, logger);
        assertThat(roles.size(), is(1));
        assertThat(roles, hasKey("valid_role"));
        Permission.Global.Role role = roles.get("valid_role");
        assertThat(role, notNullValue());
        assertThat(role.name(), equalTo("valid_role"));

        List<CapturingLogger.Msg> entries = logger.output(CapturingLogger.Level.ERROR);
        assertThat(entries, hasSize(5));
        assertThat(entries.get(0).text, startsWith("invalid role definition [$dlk39] in roles file [" + path.toAbsolutePath() + "]. invalid role name"));
        assertThat(entries.get(1).text, startsWith("invalid role definition [role1] in roles file [" + path.toAbsolutePath() + "]"));
        assertThat(entries.get(2).text, startsWith("invalid role definition [role2] in roles file [" + path.toAbsolutePath() + "]. could not resolve cluster privileges [blkjdlkd]"));
        assertThat(entries.get(3).text, startsWith("invalid role definition [role3] in roles file [" + path.toAbsolutePath() + "]. [indices] field value must be an array"));
        assertThat(entries.get(4).text, startsWith("invalid role definition [role4] in roles file [" + path.toAbsolutePath() + "]. could not resolve indices privileges [al;kjdlkj;lkj]"));
    }
}
