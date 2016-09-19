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


import org.elasticsearch.Version;
import org.elasticsearch.action.ActionModule;
import org.elasticsearch.common.cli.Terminal;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.internal.InternalSettingsPreparer;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.xpack.prelert.action.PutJobAction;
import org.elasticsearch.xpack.prelert.action.TransportPutJobAction;
import org.elasticsearch.xpack.prelert.rest.RestPutJobsAction;
import org.elasticsearch.rest.RestModule;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

public class ServerBootstrap {

    private static final String JETTY_PORT_PROPERTY = "jetty.port";
    private static final String JETTY_HOME_PROPERTY = "jetty.home";
    private static final String DEFAULT_JETTY_HOME = "cots/jetty";

    public static final int JETTY_PORT = 8080;

    public static void main(String[] args) throws Exception {
        System.setProperty("es.logger.prefix", "");

        Settings.Builder settings = Settings.builder();
        settings.put("path.home", DEFAULT_JETTY_HOME);
        settings.put("http.port", JETTY_PORT);
        settings.put("name", "node");
        settings.put("cluster.name", "prelert");
        settings.put("security.manager.enabled", "false");
        settings.put(InternalSettingsPreparer.IGNORE_SYSTEM_PROPERTIES_SETTING, true);


        CountDownLatch latch = new CountDownLatch(1);
        try {
            Node node = new PrelertNode(settings.build());
            Runtime.getRuntime().addShutdownHook(new Thread() {

                @Override
                public void run() {
                    node.close();
                    latch.countDown();
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
            super(InternalSettingsPreparer.prepareEnvironment(settings, Terminal.DEFAULT), Version.CURRENT,
                    Collections.singleton(PrelertPlugin.class));
        }
    }

    public static class PrelertPlugin extends Plugin {

        @Override
        public String name() {
            return "prelert";
        }

        @Override
        public String description() {
            return "prelert plugin";
        }

        @Override
        public Collection<Module> nodeModules() {
            return Collections.singleton(new PrelertModule());
        }

        public void onModule(RestModule module) {
            module.addRestAction(RestPutJobsAction.class);
        }

        public void onModule(ActionModule module) {
            module.registerAction(PutJobAction.INSTANCE, TransportPutJobAction.class);
        }
    }

}
