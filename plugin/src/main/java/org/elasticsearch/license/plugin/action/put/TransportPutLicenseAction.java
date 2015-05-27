/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated
 *  All Rights Reserved.
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
package org.elasticsearch.license.plugin.action.put;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.TransportMasterNodeAction;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.plugin.core.LicensesManagerService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import static org.elasticsearch.license.plugin.core.LicensesService.LicensesUpdateResponse;
import static org.elasticsearch.license.plugin.core.LicensesService.PutLicenseRequestHolder;

public class TransportPutLicenseAction extends TransportMasterNodeAction<PutLicenseRequest, PutLicenseResponse> {

    private final LicensesManagerService licensesManagerService;

    @Inject
    public TransportPutLicenseAction(Settings settings, TransportService transportService, ClusterService clusterService,
                                     LicensesManagerService licensesManagerService, ThreadPool threadPool, ActionFilters actionFilters) {
        super(settings, PutLicenseAction.NAME, transportService, clusterService, threadPool, actionFilters, PutLicenseRequest.class);
        this.licensesManagerService = licensesManagerService;
    }


    @Override
    protected String executor() {
        return ThreadPool.Names.MANAGEMENT;
    }

    @Override
    protected PutLicenseResponse newResponse() {
        return new PutLicenseResponse();
    }

    @Override
    protected ClusterBlockException checkBlock(PutLicenseRequest request, ClusterState state) {
        return state.blocks().indexBlockedException(ClusterBlockLevel.METADATA_WRITE, "");
    }

    @Override
    protected void masterOperation(final PutLicenseRequest request, ClusterState state, final ActionListener<PutLicenseResponse> listener) throws ElasticsearchException {
        licensesManagerService.registerLicenses(new PutLicenseRequestHolder(request, "put licenses []"), new ActionListener<LicensesUpdateResponse>() {
            @Override
            public void onResponse(LicensesUpdateResponse licensesUpdateResponse) {
                listener.onResponse(new PutLicenseResponse(licensesUpdateResponse.isAcknowledged(), licensesUpdateResponse.status()));
            }

            @Override
            public void onFailure(Throwable e) {
                listener.onFailure(e);
            }
        });
    }

}
