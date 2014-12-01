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

package org.elasticsearch.shield.ssl;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Provider;
import org.elasticsearch.common.settings.Settings;

/**
 *
 */
public class SSLServiceProvider implements Provider<SSLService> {

    private final Settings settings;

    private SSLService service;

    @Inject
    public SSLServiceProvider(Settings settings) {
        this.settings = settings;
    }

    @Override
    public synchronized SSLService get() {
        if (service == null) {
            service = new SSLService(settings);
        }
        return service;
    }
}
