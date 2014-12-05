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

package org.elasticsearch.alerts;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.indices.IndexMissingException;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simple service to get settings that are persisted in the a special document in the .alerts index.
 * Also notifies known components about setting changes.
 *
 * The service requires on the fact that the alert service has been started.
 */
public class ConfigurationManager extends AbstractComponent {

    public static final String CONFIG_TYPE = "config";
    public static final String GLOBAL_CONFIG_NAME = "global";

    private final Client client;
    private final CopyOnWriteArrayList<ConfigurableComponentListener> registeredComponents;

    @Inject
    public ConfigurationManager(Settings settings, Client client) {
        super(settings);
        this.client = client;
        registeredComponents = new CopyOnWriteArrayList<>();
    }

    /**
     * This method gets the config
     * @return The immutable settings loaded from the index
     */
    public Settings getConfig() {
        try {
            client.admin().indices().prepareRefresh(AlertsStore.ALERT_INDEX).get();
        } catch (IndexMissingException ime) {
            logger.error("No index [" + AlertsStore.ALERT_INDEX + "] found");
            return null;
        }
        GetResponse response = client.prepareGet(AlertsStore.ALERT_INDEX, CONFIG_TYPE, GLOBAL_CONFIG_NAME).get();
        if (response.isExists()) {
            return ImmutableSettings.settingsBuilder().loadFromSource(response.getSourceAsString()).build();
        } else {
            return null;
        }
    }

    /**
     * Notify the listeners of a new config
     *
     * @param settingsSource
     */
    public void updateConfig(BytesReference settingsSource) throws IOException {
        Settings settings = ImmutableSettings.settingsBuilder().loadFromSource(settingsSource.toUtf8()).build();
        for (ConfigurableComponentListener componentListener : registeredComponents) {
            componentListener.receiveConfigurationUpdate(settings);
        }
    }

    /**
     * Registers an component to receive config updates
     */
    public void registerListener(ConfigurableComponentListener configListener) {
        if (!registeredComponents.contains(configListener)) {
            registeredComponents.add(configListener);
        }
    }
}
