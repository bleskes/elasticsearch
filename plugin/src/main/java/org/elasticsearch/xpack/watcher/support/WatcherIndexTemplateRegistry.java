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

package org.elasticsearch.xpack.watcher.support;

import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.util.Supplier;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateResponse;
import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateListener;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.gateway.GatewayService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.security.InternalClient;
import org.elasticsearch.xpack.template.TemplateUtils;
import org.elasticsearch.xpack.watcher.support.init.proxy.WatcherClientProxy;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static java.util.Collections.unmodifiableMap;

/**
 */
public class WatcherIndexTemplateRegistry extends AbstractComponent implements ClusterStateListener {

    private static final String FORBIDDEN_INDEX_SETTING = "index.mapper.dynamic";
    public static final String INDEX_TEMPLATE_VERSION = "3";

    public static final String HISTORY_TEMPLATE_NAME = "watch_history_" + INDEX_TEMPLATE_VERSION;
    public static final String TRIGGERED_TEMPLATE_NAME = "triggered_watches";
    public static final String WATCHES_TEMPLATE_NAME = "watches";

    public static final Setting<Settings> HISTORY_TEMPLATE_SETTING = Setting.groupSetting("xpack.watcher.history.index.",
            Setting.Property.Dynamic, Setting.Property.NodeScope);
    public static final Setting<Settings> TRIGGERED_TEMPLATE_SETTING = Setting.groupSetting("xpack.watcher.triggered_watches.index.",
            Setting.Property.Dynamic, Setting.Property.NodeScope);
    public static final Setting<Settings> WATCHES_TEMPLATE_SETTING = Setting.groupSetting("xpack.watcher.watches.index.",
            Setting.Property.Dynamic, Setting.Property.NodeScope);
    public static final TemplateConfig[] TEMPLATE_CONFIGS = new TemplateConfig[]{
            new TemplateConfig(TRIGGERED_TEMPLATE_NAME, TRIGGERED_TEMPLATE_SETTING),
            new TemplateConfig(HISTORY_TEMPLATE_NAME, "watch_history", HISTORY_TEMPLATE_SETTING),
            new TemplateConfig(WATCHES_TEMPLATE_NAME, WATCHES_TEMPLATE_SETTING)
    };

    private final WatcherClientProxy client;
    private final ThreadPool threadPool;
    private final ClusterService clusterService;
    private final TemplateConfig[] indexTemplates;

    private final ConcurrentMap<String, AtomicBoolean> templateCreationsInProgress = new ConcurrentHashMap<>();

    private volatile Map<String, Settings> customIndexSettings;

    @Inject
    public WatcherIndexTemplateRegistry(Settings settings, ClusterSettings clusterSettings, ClusterService clusterService,
                                        ThreadPool threadPool, InternalClient client) {
        super(settings);
        this.client = new WatcherClientProxy(settings, client);
        this.threadPool = threadPool;
        this.clusterService = clusterService;
        this.indexTemplates = TEMPLATE_CONFIGS;
        clusterService.addListener(this);

        Map<String, Settings> customIndexSettings = new HashMap<>();
        for (TemplateConfig indexTemplate : indexTemplates) {
            clusterSettings.addSettingsUpdateConsumer(indexTemplate.getSetting(), (s) -> updateConfig(indexTemplate, s));
            Settings customSettings = this.settings.getAsSettings(indexTemplate.getSetting().getKey());
            customIndexSettings.put(indexTemplate.getSetting().getKey(), customSettings);
        }
        this.customIndexSettings = unmodifiableMap(customIndexSettings);
    }

    @Override
    public void clusterChanged(ClusterChangedEvent event) {
        ClusterState state = event.state();
        if (state.blocks().hasGlobalBlock(GatewayService.STATE_NOT_RECOVERED_BLOCK)) {
            // wait until the gateway has recovered from disk, otherwise we think may not have the index templates,
            // while they actually do exist
            return;
        }

        if (event.localNodeMaster() == false) {
            // Only the node that runs or will run Watcher should update the templates. Otherwise unnecessary put template
            // calls would happen
            return;
        }

        addTemplatesIfMissing(state, false);
    }

    /**
     * Adds the registered index templates if missing to the cluster.
     */
    public void addTemplatesIfMissing() {
        // to be sure that the templates exist after this method call, we should wait until the put index templates calls
        // have returned if the templates were missing
        addTemplatesIfMissing(clusterService.state(), true);
    }

    void addTemplatesIfMissing(ClusterState state, boolean wait) {
        for (TemplateConfig template : indexTemplates) {
            final String templateName = template.getTemplateName();
            final AtomicBoolean creationCheck = templateCreationsInProgress.computeIfAbsent(templateName, key -> new AtomicBoolean(false));
            if (creationCheck.compareAndSet(false, true)) {
                if (!state.metaData().getTemplates().containsKey(templateName)) {

                    logger.debug("adding index template [{}], because it doesn't exist", templateName);
                    putTemplate(template, creationCheck, wait);
                } else {
                    creationCheck.set(false);
                    logger.trace("not adding index template [{}], because it already exists", templateName);
                }
            }
        }
    }

    private void updateConfig(TemplateConfig config, Settings settings) {
        if (clusterService.localNode().isMasterNode() == false) {
            // Only the node that runs or will run Watcher should update the templates. Otherwise unnecessary put template
            // calls would happen
            return;
        }
        if (settings.names().isEmpty()) {
            return;
        }

        Settings existingSettings = customIndexSettings.get(config.getSetting().getKey());
        if (existingSettings == null) {
            existingSettings = Settings.EMPTY;
        }

        boolean changed = false;
        Settings.Builder builder = Settings.builder().put(existingSettings);
        for (Map.Entry<String, String> newSettingsEntry : settings.getAsMap().entrySet()) {
            String name = "index." + newSettingsEntry.getKey();
            if (FORBIDDEN_INDEX_SETTING.equals(name)) {
                logger.warn("overriding the default [{}} setting is forbidden. ignoring...", name);
                continue;
            }

            String newValue = newSettingsEntry.getValue();
            String currentValue = existingSettings.get(name);
            if (!newValue.equals(currentValue)) {
                changed = true;
                builder.put(name, newValue);
                logger.info("changing setting [{}] from [{}] to [{}]", name, currentValue, newValue);
            }
        }

        if (changed) {
            Map<String, Settings> customIndexSettings = new HashMap<String, Settings>(this.customIndexSettings);
            customIndexSettings.put(config.getSetting().getKey(), builder.build());
            this.customIndexSettings = customIndexSettings;
            putTemplate(config, templateCreationsInProgress.computeIfAbsent(config.getTemplateName(), key -> new AtomicBoolean(true)),
                    false);
        }
    }

    private void putTemplate(final TemplateConfig config, final AtomicBoolean creationCheck, boolean wait) {
        final Executor executor;
        if (wait) {
            executor = Runnable::run;
        } else {
            executor = threadPool.generic();
        }
        executor.execute(() -> {
            final String templateName = config.getTemplateName();
            final byte[] template = TemplateUtils.loadTemplate("/" + config.getFileName() + ".json", INDEX_TEMPLATE_VERSION,
                    Pattern.quote("${xpack.watcher.template.version}")).getBytes(StandardCharsets.UTF_8);

            PutIndexTemplateRequest request = new PutIndexTemplateRequest(templateName).source(template, XContentType.JSON);
            request.masterNodeTimeout(TimeValue.timeValueMinutes(1));
            Settings customSettings = customIndexSettings.get(config.getSetting().getKey());
            if (customSettings != null && customSettings.names().size() > 0) {
                Settings updatedSettings = Settings.builder()
                        .put(request.settings())
                        .put(customSettings)
                        .build();
                request.settings(updatedSettings);
            }
            client.putTemplate(request, new ActionListener<PutIndexTemplateResponse>() {
                @Override
                public void onResponse(PutIndexTemplateResponse response) {
                    creationCheck.set(false);
                    if (response.isAcknowledged() == false) {
                        logger.error("Error adding watcher template [{}], request was not acknowledged", templateName);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    creationCheck.set(false);
                    logger.error(new ParameterizedMessage("Error adding watcher template [{}]", templateName), e);
                }
            });
        });
    }

    public static class TemplateConfig {

        private final String templateName;
        private String fileName;
        private final Setting<Settings> setting;

        public TemplateConfig(String templateName, Setting<Settings> setting) {
            this(templateName, templateName, setting);
        }

        public TemplateConfig(String templateName, String fileName, Setting<Settings> setting) {
            this.templateName = templateName;
            this.fileName = fileName;
            this.setting = setting;
        }

        public String getFileName() {
            return fileName;
        }

        public String getTemplateName() {
            return templateName;
        }

        public Setting<Settings> getSetting() {
            return setting;
        }
    }
}
