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

package org.elasticsearch.xpack.graph;

import org.elasticsearch.action.ActionModule;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsModule;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.xpack.XPackPlugin;
import org.elasticsearch.xpack.graph.action.GraphExploreAction;
import org.elasticsearch.xpack.graph.action.TransportGraphExploreAction;
import org.elasticsearch.xpack.graph.rest.action.RestGraphAction;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Graph extends Plugin {

    public static final String NAME = "graph";
    private final boolean transportClientMode;
    protected final boolean enabled;
    
    
    public Graph(Settings settings) {
        this.transportClientMode = XPackPlugin.transportClientMode(settings);
        enabled = enabled(settings);
    }    
    
    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Elasticsearch Graph Plugin";
    }
    
    public static boolean enabled(Settings settings) {
        return XPackPlugin.featureEnabled(settings, NAME, true);
    }

    public Collection<Module> nodeModules() {
        return Collections.singletonList(new GraphModule(enabled, transportClientMode));
    }

    @Override
    public Collection<Class<? extends LifecycleComponent>> nodeServices() {
        if (enabled == false|| transportClientMode) {
            return Collections.emptyList();
        }
        return Collections.singletonList(GraphLicensee.class);
    }

    public void onModule(ActionModule actionModule) {
        if (enabled) {
            actionModule.registerAction(GraphExploreAction.INSTANCE, TransportGraphExploreAction.class);
        }
    }

    public void onModule(NetworkModule module) {
        if (enabled && transportClientMode == false) {
            module.registerRestHandler(RestGraphAction.class);        
        }
    }


    @Override
    public List<Setting<?>> getSettings() {
        return Collections.singletonList(Setting.boolSetting(XPackPlugin.featureEnabledSetting(NAME), true, Setting.Property.NodeScope));
    }

}
