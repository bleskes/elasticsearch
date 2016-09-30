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

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.cli.Terminal;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.internal.InternalSettingsPreparer;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;

import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.transport.Netty4Plugin;
import org.elasticsearch.xpack.prelert.action.job.GetJobsAction;
import org.elasticsearch.xpack.prelert.action.job.PutJobAction;
import org.elasticsearch.xpack.prelert.action.job.TransportGetJobsAction;
import org.elasticsearch.xpack.prelert.action.job.TransportPutJobAction;
import org.elasticsearch.xpack.prelert.rest.job.RestGetJobsAction;
import org.elasticsearch.xpack.prelert.rest.job.RestPutJobsAction;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ServerBootstrap {

    private static final String JETTY_PORT_PROPERTY = "jetty.port";
    private static final String JETTY_HOME_PROPERTY = "jetty.home";
    private static final String DEFAULT_JETTY_HOME = "cots/jetty";

    public static final int JETTY_PORT = 8080;

    public static void main(String[] args) throws Exception {
        System.setProperty("es.logger.prefix", "");

        Settings.Builder settings = Settings.builder();
        settings.put("path.home", System.getProperty(JETTY_HOME_PROPERTY, DEFAULT_JETTY_HOME));
        settings.put("http.port", JETTY_PORT);
        settings.put("cluster.name", "prelert");

        CountDownLatch latch = new CountDownLatch(1);
        try {
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
            latch.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class PrelertNode extends Node {

        public PrelertNode(Settings settings) {
            super(
                    InternalSettingsPreparer.prepareEnvironment(settings, Terminal.DEFAULT),
                    Arrays.asList(PrelertPlugin.class, Netty4Plugin.class)
            );
        }
    }

    public static class PrelertPlugin extends Plugin implements ActionPlugin {

        @Override
        public Collection<Module> createGuiceModules() {
            return Collections.singleton(new PrelertModule());
        }

        @Override
        public List<Class<? extends RestHandler>> getRestHandlers() {
            return Arrays.asList(
                    RestGetJobsAction.class,
                    RestPutJobsAction.class);
        }

        @Override
        public List<ActionHandler<? extends ActionRequest<?>, ? extends ActionResponse>> getActions() {
            return Arrays.asList(
                    new ActionHandler<>(GetJobsAction.INSTANCE, TransportGetJobsAction.class),
                    new ActionHandler<>(PutJobAction.INSTANCE, TransportPutJobAction.class));
        }
    }

}
