/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.license.plugin;

import org.elasticsearch.action.ActionModule;
import org.elasticsearch.action.admin.cluster.ClusterAction;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.common.collect.ImmutableSet;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.license.plugin.action.get.GetLicenseAction;
import org.elasticsearch.license.plugin.action.get.TransportGetLicenseAction;
import org.elasticsearch.license.plugin.action.put.PutLicenseAction;
import org.elasticsearch.license.plugin.action.put.TransportPutLicenseAction;
import org.elasticsearch.license.plugin.core.LicensesMetaData;
import org.elasticsearch.license.plugin.rest.RestGetLicenseAction;
import org.elasticsearch.license.plugin.rest.RestPutLicenseAction;
import org.elasticsearch.license.plugin.core.LicensesService;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;

import java.util.Collection;

//TODO: plugin hooks
public class LicensePlugin extends AbstractPlugin {

    static {
        MetaData.registerFactory(LicensesMetaData.TYPE, LicensesMetaData.FACTORY);
    }

    @Override
    public String name() {
        return "license";
    }

    @Override
    public String description() {
        return "Internal Elasticsearch Licensing Plugin";
    }

    public void onModule(RestModule module) {
        // Register REST endpoint
        module.addRestAction(RestPutLicenseAction.class);
        module.addRestAction(RestGetLicenseAction.class);
    }

    public void onModule(ActionModule module) {
        module.registerAction(PutLicenseAction.INSTANCE, TransportPutLicenseAction.class);
        module.registerAction(GetLicenseAction.INSTANCE, TransportGetLicenseAction.class);
    }

    @Override
    public Collection<Class<? extends LifecycleComponent>> services() {
        return ImmutableSet.<Class<? extends LifecycleComponent>>of(LicensesService.class);
    }


    @Override
    public Collection<Class<? extends Module>> modules() {
        return ImmutableSet.<Class<? extends Module>>of(LicenseModule.class);
    }
    //TODO: module binding? (LicenseModule)
}
