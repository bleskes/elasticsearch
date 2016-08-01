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

package org.elasticsearch.xpack.action;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.XPackInfoResponse;
import org.elasticsearch.license.License;
import org.elasticsearch.license.LicenseService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.XPackBuild;
import org.elasticsearch.xpack.XPackFeatureSet;
import org.elasticsearch.license.XPackInfoResponse.FeatureSetsInfo.FeatureSet;
import org.elasticsearch.license.XPackInfoResponse.LicenseInfo;

import java.util.Set;
import java.util.stream.Collectors;

/**
 */
public class TransportXPackInfoAction extends HandledTransportAction<XPackInfoRequest, XPackInfoResponse> {

    private final LicenseService licenseService;
    private final Set<XPackFeatureSet> featureSets;

    @Inject
    public TransportXPackInfoAction(Settings settings, ThreadPool threadPool, TransportService transportService,
                                    ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver,
                                    LicenseService licenseService, Set<XPackFeatureSet> featureSets) {
        super(settings, XPackInfoAction.NAME, threadPool, transportService, actionFilters, indexNameExpressionResolver,
                XPackInfoRequest::new);
        this.licenseService = licenseService;
        this.featureSets = featureSets;
    }

    @Override
    protected void doExecute(XPackInfoRequest request, ActionListener<XPackInfoResponse> listener) {


        XPackInfoResponse.BuildInfo buildInfo = null;
        if (request.getCategories().contains(XPackInfoRequest.Category.BUILD)) {
            buildInfo = new XPackInfoResponse.BuildInfo(XPackBuild.CURRENT);
        }

        LicenseInfo licenseInfo = null;
        if (request.getCategories().contains(XPackInfoRequest.Category.LICENSE)) {
            License license = licenseService.getLicense();
            if (license != null) {
                licenseInfo = new LicenseInfo(license);
            }
        }

        XPackInfoResponse.FeatureSetsInfo featureSetsInfo = null;
        if (request.getCategories().contains(XPackInfoRequest.Category.FEATURES)) {
            Set<FeatureSet> featureSets = this.featureSets.stream().map(fs ->
                    new FeatureSet(fs.name(), request.isVerbose() ? fs.description() : null, fs.available(), fs.enabled()))
                    .collect(Collectors.toSet());
            featureSetsInfo = new XPackInfoResponse.FeatureSetsInfo(featureSets);
        }

        listener.onResponse(new XPackInfoResponse(buildInfo, licenseInfo, featureSetsInfo));
    }
}
