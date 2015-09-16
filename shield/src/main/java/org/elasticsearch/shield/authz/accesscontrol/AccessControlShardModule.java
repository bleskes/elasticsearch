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

package org.elasticsearch.shield.authz.accesscontrol;

import org.elasticsearch.common.inject.multibindings.Multibinder;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.engine.IndexSearcherWrapper;
import org.elasticsearch.shield.support.AbstractShieldModule;

public class AccessControlShardModule extends AbstractShieldModule.Node {

    public AccessControlShardModule(Settings settings) {
        super(settings);
    }

    @Override
    protected void configureNode() {
        Multibinder<IndexSearcherWrapper> multibinder
                = Multibinder.newSetBinder(binder(), IndexSearcherWrapper.class);
        multibinder.addBinding().to(ShieldIndexSearcherWrapper.class);
    }
}
