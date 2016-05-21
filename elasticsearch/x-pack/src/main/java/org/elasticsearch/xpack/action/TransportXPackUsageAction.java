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
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.XPackFeatureSet;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 */
public class TransportXPackUsageAction extends HandledTransportAction<XPackUsageRequest, XPackUsageResponse> {

    private final Set<XPackFeatureSet> featureSets;

    @Inject
    public TransportXPackUsageAction(Settings settings, ThreadPool threadPool, TransportService transportService,
                                     ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver,
                                     Set<XPackFeatureSet> featureSets) {
        super(settings, XPackInfoAction.NAME, threadPool, transportService, actionFilters, indexNameExpressionResolver,
                XPackUsageRequest::new);
        this.featureSets = featureSets;
    }

    @Override
    protected void doExecute(XPackUsageRequest request, ActionListener<XPackUsageResponse> listener) {
        List<XPackFeatureSet.Usage> usages = featureSets.stream().map(XPackFeatureSet::usage).collect(Collectors.toList());
        listener.onResponse(new XPackUsageResponse(usages));
    }
}
