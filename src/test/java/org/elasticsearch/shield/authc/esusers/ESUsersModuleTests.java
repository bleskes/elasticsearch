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

package org.elasticsearch.shield.authc.esusers;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Guice;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;

/**
 *
 */
public class ESUsersModuleTests extends ElasticsearchTestCase {

    private Path users;
    private Path usersRoles;

    @Before
    public void init() throws Exception {
        users = Paths.get(getClass().getResource("users").toURI());
        usersRoles = Paths.get(getClass().getResource("users_roles").toURI());
    }

    @Test
    public void test() throws Exception {
        Settings settings = ImmutableSettings.builder().put("client.type", "node").build();
        Injector injector = Guice.createInjector(new TestModule(users, usersRoles), new ESUsersModule(settings));
        ESUsersRealm realm = injector.getInstance(ESUsersRealm.class);
        assertThat(realm, notNullValue());
        assertThat(realm.userPasswdStore, notNullValue());
        assertThat(realm.userPasswdStore, instanceOf(FileUserPasswdStore.class));
        assertThat(realm.userRolesStore, notNullValue());
        assertThat(realm.userRolesStore, instanceOf(FileUserRolesStore.class));
    }

    @Test
    public void testEnabled() throws Exception {
        assertThat(ESUsersModule.enabled(ImmutableSettings.EMPTY), is(true));
        Settings settings = ImmutableSettings.builder()
                .put("shield.authc.esusers.enabled", false)
                .build();
        assertThat(ESUsersModule.enabled(settings), is(false));
        settings = ImmutableSettings.builder()
                .put("shield.authc.esusers.enabled", true)
                .build();
        assertThat(ESUsersModule.enabled(settings), is(true));
    }

    public static class TestModule extends AbstractModule {

        final Path users;
        final Path usersRoles;

        public TestModule(Path users, Path usersRoles) {
            this.users = users;
            this.usersRoles = usersRoles;
        }

        @Override
        protected void configure() {
            Settings settings = ImmutableSettings.builder()
                    .put("shield.authc.esusers.file.users", users.toAbsolutePath())
                    .put("shield.authc.esusers.file.users_roles", usersRoles.toAbsolutePath())
                    .build();
            Environment env = new Environment(settings);
            bind(Settings.class).toInstance(settings);
            bind(Environment.class).toInstance(env);
            bind(ThreadPool.class).toInstance(new ThreadPool("test"));
            bind(ResourceWatcherService.class).asEagerSingleton();
            bind(RestController.class).toInstance(mock(RestController.class));
        }
    }

}
