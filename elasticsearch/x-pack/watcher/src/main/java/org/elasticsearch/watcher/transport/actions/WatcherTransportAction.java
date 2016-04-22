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
import org.elasticsearch.action.support.master.MasterNodeRequest;
import org.elasticsearch.action.support.master.TransportMasterNodeAction;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.plugin.core.LicenseUtils;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.watcher.WatcherLicensee;

import java.util.function.Supplier;

/**
 *
 */
public abstract class WatcherTransportAction<Request extends MasterNodeRequest<Request>, Response extends ActionResponse>
        extends TransportMasterNodeAction<Request, Response> {

    protected final WatcherLicensee watcherLicensee;

    public WatcherTransportAction(Settings settings, String actionName, TransportService transportService,
                                  ClusterService clusterService, ThreadPool threadPool, ActionFilters actionFilters,
                                  IndexNameExpressionResolver indexNameExpressionResolver, WatcherLicensee watcherLicensee,
                                  Supplier<Request> request) {
        super(settings, actionName, transportService, clusterService, threadPool, actionFilters, indexNameExpressionResolver, request);
        this.watcherLicensee = watcherLicensee;
    }

    @Override
    protected void doExecute(Task task, Request request, ActionListener<Response> listener) {
        if (watcherLicensee.isWatcherTransportActionAllowed()) {
            super.doExecute(task, request, listener);
        } else {
            listener.onFailure(LicenseUtils.newComplianceException(WatcherLicensee.ID));
        }
    }
}
