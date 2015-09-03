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

import com.google.common.base.Joiner;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.http.HttpServerTransport;
import org.elasticsearch.node.Node;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.action.authc.cache.ClearRealmCacheRequest;
import org.elasticsearch.shield.action.authc.cache.ClearRealmCacheResponse;
import org.elasticsearch.shield.authc.Realm;
import org.elasticsearch.shield.authc.Realms;
import org.elasticsearch.shield.authc.support.Hasher;
import org.elasticsearch.shield.authc.support.SecuredString;
import org.elasticsearch.shield.authc.support.SecuredStringTests;
import org.elasticsearch.shield.authc.support.UsernamePasswordToken;
import org.elasticsearch.shield.client.ShieldClient;
import org.elasticsearch.test.ShieldIntegTestCase;
import org.elasticsearch.test.ShieldSettingsSource;
import org.elasticsearch.test.rest.client.http.HttpRequestBuilder;
import org.elasticsearch.test.rest.client.http.HttpResponse;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.*;

/**
 *
 */
public class ClearRealmsCacheTests extends ShieldIntegTestCase {

    private static final String USERS_PASSWD_HASHED = new String(Hasher.BCRYPT.hash(new SecuredString("passwd".toCharArray())));

    private static String[] usernames;

    @BeforeClass
    public static void init() throws Exception {
        usernames = new String[randomIntBetween(5, 10)];
        for (int i = 0; i < usernames.length; i++) {
            usernames[i] = randomAsciiOfLength(6) + "_" + i;
        }
    }

    enum Scenario {

        EVICT_ALL() {

            @Override
            public void assertEviction(User prevUser, User newUser) {
                assertThat(prevUser, not(sameInstance(newUser)));
            }

            @Override
            public void executeRequest() throws Exception {
                executeTransportRequest(new ClearRealmCacheRequest());
            }
        },

        EVICT_SOME() {

            private final String[] evicted_usernames = randomSelection(usernames);
            {
                Arrays.sort(evicted_usernames);
            }

            @Override
            public void assertEviction(User prevUser, User newUser) {
                if (Arrays.binarySearch(evicted_usernames, prevUser.principal()) >= 0) {
                    assertThat(prevUser, not(sameInstance(newUser)));
                } else {
                    assertThat(prevUser, sameInstance(newUser));
                }
            }

            @Override
            public void executeRequest() throws Exception {
                executeTransportRequest(new ClearRealmCacheRequest().usernames(evicted_usernames));
            }
        },

        EVICT_ALL_HTTP() {

            @Override
            public void assertEviction(User prevUser, User newUser) {
                assertThat(prevUser, not(sameInstance(newUser)));
            }

            @Override
            public void executeRequest() throws Exception {
                executeHttpRequest("/_shield/realm/" + (randomBoolean() ? "*" : "_all") + "/_cache/clear", Collections.<String, String>emptyMap());
            }
        },

        EVICT_SOME_HTTP() {

            private final String[] evicted_usernames = randomSelection(usernames);
            {
                Arrays.sort(evicted_usernames);
            }

            @Override
            public void assertEviction(User prevUser, User newUser) {
                if (Arrays.binarySearch(evicted_usernames, prevUser.principal()) >= 0) {
                    assertThat(prevUser, not(sameInstance(newUser)));
                } else {
                    assertThat(prevUser, sameInstance(newUser));
                }
            }

            @Override
            public void executeRequest() throws Exception {
                String path = "/_shield/realm/" + (randomBoolean() ? "*" : "_all") + "/_cache/clear";
                Map<String, String> params = Collections.singletonMap("usernames", Joiner.on(',').join(evicted_usernames));
                executeHttpRequest(path, params);
            }
        };

        public abstract void assertEviction(User prevUser, User newUser);

        public abstract void executeRequest() throws Exception;

        static void executeTransportRequest(ClearRealmCacheRequest request) throws Exception {
            ShieldClient client = new ShieldClient(client());

            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<Throwable> error = new AtomicReference<>();
            client.authc().clearRealmCache(request, new ActionListener<ClearRealmCacheResponse>() {
                @Override
                public void onResponse(ClearRealmCacheResponse response) {
                    assertThat(response.getNodes().length, equalTo(internalCluster().getNodeNames().length));
                    latch.countDown();
                }

                @Override
                public void onFailure(Throwable e) {
                    error.set(e);
                    latch.countDown();
                }
            });

            if (!latch.await(5, TimeUnit.SECONDS)) {
                fail("waiting for clear realms cache request too long");
            }

            if (error.get() != null) {
                fail("failed to clear realm caches" + error.get().getMessage());
            }
        }

        static void executeHttpRequest(String path, Map<String, String> params) throws Exception {
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpRequestBuilder requestBuilder = new HttpRequestBuilder(client)
                        .httpTransport(internalCluster().getDataNodeInstance(HttpServerTransport.class))
                        .method("POST")
                        .path(path);
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    requestBuilder.addParam(entry.getKey(), entry.getValue());
                }
                requestBuilder.addHeader(UsernamePasswordToken.BASIC_AUTH_HEADER, UsernamePasswordToken.basicAuthHeaderValue(ShieldSettingsSource.DEFAULT_USER_NAME, new SecuredString(ShieldSettingsSource.DEFAULT_PASSWORD.toCharArray())));
                HttpResponse response = requestBuilder.execute();
                assertThat(response.hasBody(), is(true));
                String body = response.getBody();
                assertThat(body.contains("cluster_name"), is(true));
            }
        }
    }

    @Override
    public Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(Node.HTTP_ENABLED, true)
                .build();
    }
    @Override
    public boolean sslTransportEnabled() {
        return false;
    }

    @Override
    protected String configRoles() {
        return ShieldSettingsSource.CONFIG_ROLE_ALLOW_ALL + "\n" +
                "r1:\n" +
                "  cluster: all\n";
    }

    @Override
    protected String configUsers() {
        StringBuilder builder = new StringBuilder(ShieldSettingsSource.CONFIG_STANDARD_USER);
        for (String username : usernames) {
            builder.append(username).append(":").append(USERS_PASSWD_HASHED).append("\n");
        }
        return builder.toString();
    }

    @Override
    protected String configUsersRoles() {
        return ShieldSettingsSource.CONFIG_STANDARD_USER_ROLES +
                "r1:" + Strings.arrayToCommaDelimitedString(usernames);
    }

    @Test
    public void testEvictAll() throws Exception {
        testScenario(Scenario.EVICT_ALL);
    }

    @Test
    public void testEvictSome() throws Exception {
        testScenario(Scenario.EVICT_SOME);
    }

    @Test
    public void testEvictAllHttp() throws Exception {
        testScenario(Scenario.EVICT_ALL_HTTP);
    }

    @Test
    public void testEvictSomeHttp() throws Exception {
        testScenario(Scenario.EVICT_SOME_HTTP);
    }

    private void testScenario(Scenario scenario) throws Exception {

        Map<String, UsernamePasswordToken> tokens = new HashMap<>();
        for (String user : usernames) {
            tokens.put(user, new UsernamePasswordToken(user, SecuredStringTests.build("passwd")));
        }

        List<Realm> realms = new ArrayList<>();
        for (Realms nodeRealms : internalCluster().getInstances(Realms.class)) {
            realms.add(nodeRealms.realm("esusers"));
        }


        // we authenticate each user on each of the realms to make sure they're all cached
        Map<String, Map<Realm, User>> users = new HashMap<>();
        for (Realm realm : realms) {
            for (String username : usernames) {
                User user = realm.authenticate(tokens.get(username));
                assertThat(user, notNullValue());
                Map<Realm, User> realmToUser = users.get(username);
                if (realmToUser == null) {
                    realmToUser = new HashMap<>();
                    users.put(username, realmToUser);
                }
                realmToUser.put(realm, user);
            }
        }

        // all users should be cached now on all realms, lets verify

        for (String username : usernames) {
            for (Realm realm : realms) {
                assertThat(realm.authenticate(tokens.get(username)), sameInstance(users.get(username).get(realm)));
            }
        }

        // now, lets run the scenario
        scenario.executeRequest();

        // now, user_a should have been evicted, but user_b should still be cached
        for (String username : usernames) {
            for (Realm realm : realms) {
                User user = realm.authenticate(tokens.get(username));
                assertThat(user, notNullValue());
                scenario.assertEviction(users.get(username).get(realm), user);
            }
        }
    }

    // selects a random sub-set of the give values
    private static String[] randomSelection(String[] values) {
        List<String> list = new ArrayList<>();
        while (list.isEmpty()) {
            double base = randomDouble();
            for (String value : values) {
                if (randomDouble() < base) {
                    list.add(value);
                }
            }
        }
        return list.toArray(new String[list.size()]);
    }
}
