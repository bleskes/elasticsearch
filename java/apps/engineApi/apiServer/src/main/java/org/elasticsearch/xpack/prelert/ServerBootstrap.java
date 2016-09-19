package org.elasticsearch.xpack.prelert;


import com.prelert.settings.PrelertSettings;
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

        int port = PrelertSettings.getSettingOrDefault(JETTY_PORT_PROPERTY, JETTY_PORT);
        String home = PrelertSettings.getSettingOrDefault(JETTY_HOME_PROPERTY, DEFAULT_JETTY_HOME);

        Settings.Builder settings = Settings.builder();
        settings.put("path.home", home);
        settings.put("http.port", port);
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
