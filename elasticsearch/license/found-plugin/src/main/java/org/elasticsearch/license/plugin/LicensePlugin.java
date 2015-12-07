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
package org.elasticsearch.license.plugin;

import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.plugin.core.FoundLicensesService;
import org.elasticsearch.plugins.Plugin;

import java.util.ArrayList;
import java.util.Collection;

public class LicensePlugin extends Plugin {

    public static final String NAME = "license";

    @Inject
    public LicensePlugin(Settings settings) {
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Internal Elasticsearch Licensing Plugin";
    }

    @Override
    public Collection<Class<? extends LifecycleComponent>> nodeServices() {
        Collection<Class<? extends LifecycleComponent>> services = new ArrayList<>();
        services.add(FoundLicensesService.class);
        return services;
    }

    @Override
    public Collection<Module> nodeModules() {
        Collection<Module> modules = new ArrayList<Module>();
        modules.add(new LicenseModule());
        return modules;
    }
}
