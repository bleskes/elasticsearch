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

package org.elasticsearch.watcher.support.init;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.settings.Settings;

import java.util.Set;

/**
 * A service to lazy initialize {@link InitializingService.Initializable} constructs.
 */
public class InitializingService extends AbstractLifecycleComponent {

    private final Injector injector;
    private final Set<Initializable> initializables;

    @Inject
    public InitializingService(Settings settings, Injector injector, Set<Initializable> initializables) {
        super(settings);
        this.injector = injector;
        this.initializables = initializables;
    }

    @Override
    protected void doStart() throws ElasticsearchException {
        for (Initializable initializable : initializables) {
            initializable.init(injector);
        }
    }

    @Override
    protected void doStop() throws ElasticsearchException {
    }

    @Override
    protected void doClose() throws ElasticsearchException {
    }

    public interface Initializable {

        void init(Injector injector);
    }
}
