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

package org.elasticsearch.xpack.prelert;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.bootstrap.JarHell;
import org.elasticsearch.cli.Terminal;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.internal.InternalSettingsPreparer;
import org.elasticsearch.transport.Netty4Plugin;
import org.elasticsearch.xpack.prelert.job.persistence.ElasticsearchJobProvider;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

public class ServerBootstrap {

    private static final String JETTY_PORT_PROPERTY = "jetty.port";
    private static final String JETTY_HOME_PROPERTY = "jetty.home";
    private static final String DEFAULT_JETTY_HOME = "cots/jetty";

    public static final int JETTY_PORT = 8080;

    private static final Logger LOGGER = Loggers.getLogger(ServerBootstrap.class);

    public static void main(String[] args) throws Exception {

        boolean useNativeProcess = false;
        if (args.length > 0 && PrelertPlugin.USE_NATIVE_PROCESS_OPTION.toLowerCase().equals(args[0].toLowerCase())) {
            LOGGER.info("Using the native autodetect process");
            useNativeProcess = true;
        }

        JarHell.checkJarHell();
        Settings.Builder settings = Settings.builder();
        settings.put(PrelertPlugin.USE_NATIVE_PROCESS_OPTION, useNativeProcess);
        settings.put("path.home", System.getProperty(JETTY_HOME_PROPERTY, DEFAULT_JETTY_HOME));
        settings.put("http.port", System.getProperty(JETTY_PORT_PROPERTY, Integer.toString(JETTY_PORT)));
        settings.put("cluster.name", "prelert");
        if (System.getProperty("network-host") != null) {
            settings.put("network.host", System.getProperty("network-host"));
        }

        CountDownLatch latch = new CountDownLatch(1);
        Node node = new PrelertNode(settings.build());
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                try {
                    node.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            }
        });
        node.start();
        // Verifies that we're good to go (at least yellow state and prelert-usage exists.
        // NORELEASE: when moving to be a plugin verifying prelert-usage index exists (and create one if it isn;t there)
        // should be triggered via a cluster state listener.
        node.injector().getInstance(ElasticsearchJobProvider.class).initialize();
        latch.await();
    }

    static class PrelertNode extends Node {

        public PrelertNode(Settings settings) {
            super(InternalSettingsPreparer.prepareEnvironment(settings, Terminal.DEFAULT),
                    Arrays.asList(PrelertPlugin.class, Netty4Plugin.class));
        }
    }
}
