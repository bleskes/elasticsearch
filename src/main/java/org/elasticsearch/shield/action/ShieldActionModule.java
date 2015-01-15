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

package org.elasticsearch.shield.action;

import org.elasticsearch.action.ActionModule;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.inject.PreProcessModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.action.authc.cache.ClearRealmCacheAction;
import org.elasticsearch.shield.action.authc.cache.TransportClearRealmCacheAction;
import org.elasticsearch.shield.support.AbstractShieldModule;

/**
 *
 */
public class ShieldActionModule extends AbstractShieldModule implements PreProcessModule {

    public ShieldActionModule(Settings settings) {
        super(settings);
    }

    @Override
    public void processModule(Module module) {
        if (module instanceof ActionModule) {

            // registering the security filter only for nodes
            if (!clientMode) {
                ((ActionModule) module).registerFilter(ShieldActionFilter.class);
            }

            // registering all shield actions
            ((ActionModule) module).registerAction(ClearRealmCacheAction.INSTANCE, TransportClearRealmCacheAction.class);
        }
    }

    @Override
    protected void configure(boolean clientMode) {
        if (!clientMode) {
            bind(ShieldActionMapper.class).asEagerSingleton();
            // we need to ensure that there's only a single instance of this filter.
            bind(ShieldActionFilter.class).asEagerSingleton();
        }
    }
}
