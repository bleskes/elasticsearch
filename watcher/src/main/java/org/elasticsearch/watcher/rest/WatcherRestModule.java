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

package org.elasticsearch.watcher.rest;

import org.elasticsearch.watcher.rest.action.*;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.inject.PreProcessModule;
import org.elasticsearch.rest.RestModule;

/**
 *
 */
public class WatcherRestModule extends AbstractModule implements PreProcessModule {

    @Override
    public void processModule(Module module) {
        if (module instanceof RestModule) {
            RestModule restModule = (RestModule) module;
            restModule.addRestAction(RestPutWatchAction.class);
            restModule.addRestAction(RestDeleteWatchAction.class);
            restModule.addRestAction(RestWatcherStatsAction.class);
            restModule.addRestAction(RestWatcherInfoAction.class);
            restModule.addRestAction(RestGetWatchAction.class);
            restModule.addRestAction(RestWatchServiceAction.class);
            restModule.addRestAction(RestAckWatchAction.class);
            restModule.addRestAction(RestExecuteWatchAction.class);
            restModule.addRestAction(RestHijackOperationAction.class);
        }
    }

    @Override
    protected void configure() {
    }
}
