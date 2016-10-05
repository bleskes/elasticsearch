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

package org.elasticsearch.xpack.security.action;

import org.elasticsearch.common.inject.multibindings.Multibinder;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.XPackSettings;
import org.elasticsearch.xpack.security.action.filter.SecurityActionFilter;
import org.elasticsearch.xpack.security.action.interceptor.BulkShardRequestInterceptor;
import org.elasticsearch.xpack.security.action.interceptor.FieldStatsRequestInterceptor;
import org.elasticsearch.xpack.security.action.interceptor.RequestInterceptor;
import org.elasticsearch.xpack.security.action.interceptor.SearchRequestInterceptor;
import org.elasticsearch.xpack.security.action.interceptor.UpdateRequestInterceptor;
import org.elasticsearch.xpack.security.support.AbstractSecurityModule;

public class SecurityActionModule extends AbstractSecurityModule.Node {

    public SecurityActionModule(Settings settings) {
        super(settings);
    }

    @Override
    protected void configureNode() {
        // we need to ensure that there's only a single instance of the action filters
        bind(SecurityActionFilter.class).asEagerSingleton();

        Multibinder<RequestInterceptor> multibinder
                = Multibinder.newSetBinder(binder(), RequestInterceptor.class);
        if (XPackSettings.DLS_FLS_ENABLED.get(settings)) {
            multibinder.addBinding().to(SearchRequestInterceptor.class);
            multibinder.addBinding().to(UpdateRequestInterceptor.class);
            multibinder.addBinding().to(BulkShardRequestInterceptor.class);
            multibinder.addBinding().to(FieldStatsRequestInterceptor.class);
        }
    }
}
