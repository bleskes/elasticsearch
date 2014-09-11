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

package org.elasticsearch.shield.support;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.inject.SpawnModules;
import org.elasticsearch.common.settings.Settings;

/**
 *
 */
public abstract class AbstractShieldModule extends AbstractModule {

    protected final Settings settings;
    protected final boolean clientMode;

    public AbstractShieldModule(Settings settings) {
        this.settings = settings;
        this.clientMode = !"node".equals(settings.get(Client.CLIENT_TYPE_SETTING));
    }

    @Override
    protected final void configure() {
        configure(clientMode);
    }

    protected abstract void configure(boolean clientMode);

    public static abstract class Spawn extends AbstractShieldModule implements SpawnModules {

        protected Spawn(Settings settings) {
            super(settings);
        }

        @Override
        public final Iterable<? extends Module> spawnModules() {
            return spawnModules(clientMode);
        }

        public abstract Iterable<? extends Module> spawnModules(boolean clientMode);
    }

    public static abstract class Node extends AbstractShieldModule {

        protected Node(Settings settings) {
            super(settings);
        }

        @Override
        protected final void configure(boolean clientMode) {
            assert !clientMode : "[" + getClass().getSimpleName() + "] is a node only module";
            configureNode();
        }

        protected abstract void configureNode();

        public static abstract class Spawn extends Node implements SpawnModules {

            protected Spawn(Settings settings) {
                super(settings);
            }

            public abstract Iterable<? extends AbstractShieldModule.Node> spawnModules();
        }

    }
}
