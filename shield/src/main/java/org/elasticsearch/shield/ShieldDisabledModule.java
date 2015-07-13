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

package org.elasticsearch.shield;

import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.inject.PreProcessModule;
import org.elasticsearch.common.inject.util.Providers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestModule;
import org.elasticsearch.shield.license.LicenseService;
import org.elasticsearch.shield.rest.action.RestShieldInfoAction;
import org.elasticsearch.shield.support.AbstractShieldModule;

public class ShieldDisabledModule extends AbstractShieldModule implements PreProcessModule {

    public ShieldDisabledModule(Settings settings) {
        super(settings);
    }

    @Override
    protected void configure(boolean clientMode) {
        assert !shieldEnabled : "shield disabled module should only get loaded with shield disabled";
        if (!clientMode) {
            // required by the shield info rest action (when shield is disabled)
            bind(LicenseService.class).toProvider(Providers.<LicenseService>of(null));
        }
    }

    @Override
    public void processModule(Module module) {
        if (module instanceof RestModule) {
            //we want to expose the shield rest action even when the plugin is disabled
            ((RestModule) module).addRestAction(RestShieldInfoAction.class);
        }
    }
}
