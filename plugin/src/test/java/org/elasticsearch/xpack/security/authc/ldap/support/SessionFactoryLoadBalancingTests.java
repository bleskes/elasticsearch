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

package org.elasticsearch.xpack.security.authc.ldap.support;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.sdk.LDAPConnection;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.common.settings.SecureString;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.env.Environment;
import org.elasticsearch.test.junit.annotations.TestLogging;
import org.elasticsearch.xpack.security.authc.RealmConfig;
import org.elasticsearch.xpack.ssl.SSLService;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Tests that the server sets properly load balance connections without throwing exceptions
 */
@TestLogging("org.elasticsearch.xpack.security.authc.ldap.support:DEBUG")
public class SessionFactoryLoadBalancingTests extends LdapTestCase {

    public void testRoundRobin() throws Exception {
        TestSessionFactory testSessionFactory = createSessionFactory(LdapLoadBalancing.ROUND_ROBIN);

        final int numberOfIterations = randomIntBetween(1, 5);
        for (int iteration = 0; iteration < numberOfIterations; iteration++) {
            for (int i = 0; i < numberOfLdapServers; i++) {
                LDAPConnection connection = null;
                try {
                    connection = testSessionFactory.getServerSet().getConnection();
                    assertThat(connection.getConnectedPort(), is(ldapServers[i].getListenPort()));
                } finally {
                    if (connection != null) {
                        connection.close();
                    }
                }
            }
        }
    }

    public void testRoundRobinWithFailures() throws Exception {
        assumeTrue("at least one ldap server should be present for this test", ldapServers.length > 1);
        logger.debug("using [{}] ldap servers, urls {}", ldapServers.length, ldapUrls());
        TestSessionFactory testSessionFactory = createSessionFactory(LdapLoadBalancing.ROUND_ROBIN);

        // create a list of ports
        List<Integer> ports = new ArrayList<>(numberOfLdapServers);
        for (int i = 0; i < ldapServers.length; i++) {
            ports.add(ldapServers[i].getListenPort());
        }
        logger.debug("list of all ports {}", ports);

        final int numberToKill = randomIntBetween(1, numberOfLdapServers - 1);
        logger.debug("killing [{}] servers", numberToKill);

        // get a subset to kill
        final List<InMemoryDirectoryServer> ldapServersToKill = randomSubsetOf(numberToKill, ldapServers);
        final List<InMemoryDirectoryServer> ldapServersList = Arrays.asList(ldapServers);
        final List<ServerSocket> boundSockets = new ArrayList<>();
        final List<Thread> listenThreads = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(ldapServersToKill.size());
        try {
            for (InMemoryDirectoryServer ldapServerToKill : ldapServersToKill) {
                final int index = ldapServersList.indexOf(ldapServerToKill);
                assertThat(index, greaterThanOrEqualTo(0));
                final Integer port = Integer.valueOf(ldapServers[index].getListenPort());
                logger.debug("shutting down server index [{}] listening on [{}]", index, port);
                assertTrue(ports.remove(port));
                ldapServers[index].shutDown(true);

                // when running multiple test jvms, there is a chance that something else could
                // start listening on this port so we try to avoid this by binding again to this
                // port with a server socket. The server socket only has a backlog of size one and
                // we start a thread that connects to this socket but the connection is never
                // accepted so other connections will be rejected. The socket attempts a blocking
                // read, which will hold up the thread until the server socket is closed at the end
                // of the test.
                // NOTE: this is not perfect as there is a small amount of time between the shutdown
                // of the ldap server and the opening of the socket
                logger.debug("opening mock server socket listening on [{}]", port);
                ServerSocket mockServerSocket = new ServerSocket(port, 1, InetAddress.getByName("localhost"));
                Runnable runnable = () -> {
                    try (Socket socket = new Socket(InetAddress.getByName("localhost"), port)) {
                        logger.debug("opened socket [{}] and blocking other connections", socket);
                        latch.countDown();
                        socket.getInputStream().read();
                    } catch (IOException e) {
                        logger.trace("caught io exception", e);
                    }
                };
                Thread thread = new Thread(runnable);
                thread.start();
                boundSockets.add(mockServerSocket);
                listenThreads.add(thread);

                assertThat(ldapServers[index].getListenPort(), is(-1));
            }

            latch.await();
            final int numberOfIterations = randomIntBetween(1, 5);
            for (int iteration = 0; iteration < numberOfIterations; iteration++) {
                logger.debug("iteration [{}]", iteration);
                for (Integer port : ports) {
                    logger.debug("attempting connection with expected port [{}]", port);
                    try (LDAPConnection connection = testSessionFactory.getServerSet().getConnection()) {
                        assertThat(connection.getConnectedPort(), is(port));
                    }
                }
            }
        } finally {
            for (ServerSocket socket : boundSockets) {
                socket.close();
            }
            for (Thread t : listenThreads) {
                t.join();
            }
        }
    }

    public void testFailover() throws Exception {
        assumeTrue("at least one ldap server should be present for this test", ldapServers.length > 1);
        logger.debug("using [{}] ldap servers, urls {}", ldapServers.length, ldapUrls());
        TestSessionFactory testSessionFactory = createSessionFactory(LdapLoadBalancing.FAILOVER);

        // first test that there is no round robin stuff going on
        final int firstPort = ldapServers[0].getListenPort();
        for (int i = 0; i < numberOfLdapServers; i++) {
            LDAPConnection connection = null;
            try {
                connection = testSessionFactory.getServerSet().getConnection();
                assertThat(connection.getConnectedPort(), is(firstPort));
            } finally {
                if (connection != null) {
                    connection.close();
                }
            }
        }

        logger.debug("shutting down server index [0] listening on [{}]", ldapServers[0].getListenPort());
        // always kill the first one
        ldapServers[0].shutDown(true);
        assertThat(ldapServers[0].getListenPort(), is(-1));

        // now randomly shutdown some others
        if (ldapServers.length > 2) {
            // kill at least one other server, but we need at least one good one. Hence the upper bound is number - 2 since we need at least
            // one server to use!
            final int numberToKill = randomIntBetween(1, numberOfLdapServers - 2);
            InMemoryDirectoryServer[] allButFirstServer = Arrays.copyOfRange(ldapServers, 1, ldapServers.length);
            // get a subset to kil
            final List<InMemoryDirectoryServer> ldapServersToKill = randomSubsetOf(numberToKill, allButFirstServer);
            final List<InMemoryDirectoryServer> ldapServersList = Arrays.asList(ldapServers);
            for (InMemoryDirectoryServer ldapServerToKill : ldapServersToKill) {
                final int index = ldapServersList.indexOf(ldapServerToKill);
                assertThat(index, greaterThanOrEqualTo(1));
                final Integer port = Integer.valueOf(ldapServers[index].getListenPort());
                logger.debug("shutting down server index [{}] listening on [{}]", index, port);
                ldapServers[index].shutDown(true);
                assertThat(ldapServers[index].getListenPort(), is(-1));
            }
        }

        int firstNonStoppedPort = -1;
        // now we find the first that isn't stopped
        for (int i = 0; i < numberOfLdapServers; i++) {
            if (ldapServers[i].getListenPort() != -1) {
                firstNonStoppedPort = ldapServers[i].getListenPort();
                break;
            }
        }
        logger.debug("first non stopped port [{}]", firstNonStoppedPort);

        assertThat(firstNonStoppedPort, not(-1));
        final int numberOfIterations = randomIntBetween(1, 5);
        for (int iteration = 0; iteration < numberOfIterations; iteration++) {
            LDAPConnection connection = null;
            try {
                logger.debug("attempting connection with expected port [{}] iteration [{}]", firstNonStoppedPort, iteration);
                connection = testSessionFactory.getServerSet().getConnection();
                assertThat(connection.getConnectedPort(), is(firstNonStoppedPort));
            } finally {
                if (connection != null) {
                    connection.close();
                }
            }
        }
    }

    private TestSessionFactory createSessionFactory(LdapLoadBalancing loadBalancing) throws Exception {
        String groupSearchBase = "cn=HMS Lydia,ou=crews,ou=groups,o=sevenSeas";
        String userTemplate = "cn={0},ou=people,o=sevenSeas";
        Settings settings = buildLdapSettings(ldapUrls(), new String[] { userTemplate }, groupSearchBase,
                LdapSearchScope.SUB_TREE, loadBalancing);
        RealmConfig config = new RealmConfig("test-session-factory", settings, Settings.builder().put("path.home",
                createTempDir()).build(), new ThreadContext(Settings.EMPTY));
        return new TestSessionFactory(config, new SSLService(Settings.EMPTY, new Environment(config.globalSettings())));
    }

    static class TestSessionFactory extends SessionFactory {

        protected TestSessionFactory(RealmConfig config, SSLService sslService) {
            super(config, sslService);
        }

        @Override
        public void session(String user, SecureString password, ActionListener<LdapSession> listener) {
            listener.onResponse(null);
        }
    }
}
