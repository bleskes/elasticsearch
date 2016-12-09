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

package org.elasticsearch.xpack.ssl;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;

import javax.net.ssl.SSLContext;

/**
 * Extending SSLService to make helper methods public to access in tests
 */
public class TestsSSLService extends SSLService {

    public TestsSSLService(Settings settings, Environment environment) {
        super(settings, environment);
    }

    @Override
    public SSLContext sslContext() {
        return super.sslContext();
    }

    /**
     * Allows to get alternative ssl context, like for the http client
     */
    public SSLContext sslContext(Settings settings) {
        return sslContextHolder(super.sslConfiguration(settings)).sslContext();
    }
}
