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

package org.elasticsearch.watcher.transport.actions;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.MasterNodeOperationRequest;
import org.elasticsearch.action.support.master.TransportMasterNodeOperationAction;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.plugin.core.LicenseExpiredException;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.watcher.license.LicenseService;

/**
 *
 */
public abstract class WatcherTransportAction<Request extends MasterNodeOperationRequest<Request>, Response extends ActionResponse> extends TransportMasterNodeOperationAction<Request, Response> {

    private final LicenseService licenseService;

    public WatcherTransportAction(Settings settings, String actionName, TransportService transportService, ClusterService clusterService, ThreadPool threadPool, ActionFilters actionFilters, LicenseService licenseService) {
        super(settings, actionName, transportService, clusterService, threadPool, actionFilters);
        this.licenseService = licenseService;
    }

    @Override
    protected void doExecute(Request request, ActionListener<Response> listener) {
        if (licenseService.enabled()) {
            super.doExecute(request, listener);
        } else {
            listener.onFailure(new LicenseExpiredException(LicenseService.FEATURE_NAME));
        }
    }
}
