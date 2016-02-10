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

package org.elasticsearch.shield.action;

import org.elasticsearch.common.inject.multibindings.Multibinder;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.action.filter.ShieldActionFilter;
import org.elasticsearch.shield.action.interceptor.BulkRequestInterceptor;
import org.elasticsearch.shield.action.interceptor.RealtimeRequestInterceptor;
import org.elasticsearch.shield.action.interceptor.RequestInterceptor;
import org.elasticsearch.shield.action.interceptor.SearchRequestInterceptor;
import org.elasticsearch.shield.action.interceptor.UpdateRequestInterceptor;
import org.elasticsearch.shield.support.AbstractShieldModule;

public class ShieldActionModule extends AbstractShieldModule.Node {

    public ShieldActionModule(Settings settings) {
        super(settings);
    }

    @Override
    protected void configureNode() {
        bind(ShieldActionMapper.class).asEagerSingleton();
        // we need to ensure that there's only a single instance of the action filters
        bind(ShieldActionFilter.class).asEagerSingleton();

        // TODO: we should move these to action filters and only have 1 chain.
        Multibinder<RequestInterceptor> multibinder
                = Multibinder.newSetBinder(binder(), RequestInterceptor.class);
        multibinder.addBinding().to(RealtimeRequestInterceptor.class);
        multibinder.addBinding().to(SearchRequestInterceptor.class);
        multibinder.addBinding().to(UpdateRequestInterceptor.class);
        multibinder.addBinding().to(BulkRequestInterceptor.class);
    }
}
