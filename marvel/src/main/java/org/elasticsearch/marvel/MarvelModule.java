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

package org.elasticsearch.marvel;

import com.google.common.collect.ImmutableList;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.inject.SpawnModules;
import org.elasticsearch.marvel.agent.AgentService;
import org.elasticsearch.marvel.agent.collector.CollectorModule;
import org.elasticsearch.marvel.agent.exporter.ExporterModule;
import org.elasticsearch.marvel.agent.settings.MarvelSettingsModule;
import org.elasticsearch.marvel.license.LicenseModule;

public class MarvelModule extends AbstractModule implements SpawnModules {

    @Override
    protected void configure() {
        bind(AgentService.class).asEagerSingleton();
    }

    @Override
    public Iterable<? extends Module> spawnModules() {
        return ImmutableList.of(new MarvelSettingsModule(), new LicenseModule(), new CollectorModule(), new ExporterModule());
    }
}
