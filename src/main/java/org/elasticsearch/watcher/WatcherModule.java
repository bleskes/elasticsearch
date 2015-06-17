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

package org.elasticsearch.watcher;


import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.inject.SpawnModules;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.watcher.actions.ActionModule;
import org.elasticsearch.watcher.client.WatcherClientModule;
import org.elasticsearch.watcher.condition.ConditionModule;
import org.elasticsearch.watcher.execution.ExecutionModule;
import org.elasticsearch.watcher.history.HistoryModule;
import org.elasticsearch.watcher.input.InputModule;
import org.elasticsearch.watcher.license.LicenseModule;
import org.elasticsearch.watcher.rest.WatcherRestModule;
import org.elasticsearch.watcher.shield.WatcherShieldModule;
import org.elasticsearch.watcher.support.TemplateUtils;
import org.elasticsearch.watcher.support.clock.ClockModule;
import org.elasticsearch.watcher.support.http.HttpClientModule;
import org.elasticsearch.watcher.support.init.InitializingModule;
import org.elasticsearch.watcher.support.secret.SecretModule;
import org.elasticsearch.watcher.support.template.TemplateModule;
import org.elasticsearch.watcher.support.validation.WatcherSettingsValidation;
import org.elasticsearch.watcher.transform.TransformModule;
import org.elasticsearch.watcher.transport.WatcherTransportModule;
import org.elasticsearch.watcher.trigger.TriggerModule;
import org.elasticsearch.watcher.watch.WatchModule;

import java.util.Arrays;


public class WatcherModule extends AbstractModule implements SpawnModules {

    protected final Settings settings;

    public WatcherModule(Settings settings) {
        this.settings = settings;
    }

    @Override
    public Iterable<? extends Module> spawnModules() {
        return Arrays.asList(
                new InitializingModule(),
                new LicenseModule(),
                new WatchModule(),
                new TemplateModule(),
                new HttpClientModule(),
                new ClockModule(),
                new WatcherClientModule(),
                new TransformModule(),
                new WatcherRestModule(),
                new TriggerModule(settings),
                new WatcherTransportModule(),
                new ConditionModule(),
                new InputModule(),
                new ActionModule(),
                new HistoryModule(),
                new ExecutionModule(),
                new WatcherShieldModule(settings),
                new SecretModule(settings));
    }

    @Override
    protected void configure() {
        bind(WatcherLifeCycleService.class).asEagerSingleton();
        bind(TemplateUtils.class).asEagerSingleton();
        bind(WatcherSettingsValidation.class).asEagerSingleton();
    }

}
