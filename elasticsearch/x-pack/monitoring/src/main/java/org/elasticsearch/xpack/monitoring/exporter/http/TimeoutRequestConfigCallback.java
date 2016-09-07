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
package org.elasticsearch.xpack.monitoring.exporter.http;

import org.apache.http.client.config.RequestConfig.Builder;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.unit.TimeValue;

/**
 * {@code TimeoutRequestConfigCallback} enables the setting of connection-related timeouts for HTTP requests.
 */
class TimeoutRequestConfigCallback implements RestClientBuilder.RequestConfigCallback {

    @Nullable
    private final TimeValue connectTimeout;
    @Nullable
    private final TimeValue socketTimeout;

    /**
     * Create a new {@link TimeoutRequestConfigCallback}.
     *
     * @param connectTimeout The initial connection timeout, if any is supplied
     * @param socketTimeout The socket timeout, if any is supplied
     */
    TimeoutRequestConfigCallback(@Nullable final TimeValue connectTimeout, @Nullable final TimeValue socketTimeout) {
        assert connectTimeout != null || socketTimeout != null : "pointless to use with defaults";

        this.connectTimeout = connectTimeout;
        this.socketTimeout = socketTimeout;
    }

    /**
     * Get the initial connection timeout.
     *
     * @return Can be {@code null} for default (1 second).
     */
    @Nullable
    TimeValue getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Get the socket timeout.
     *
     * @return Can be {@code null} for default (10 seconds).
     */
    @Nullable
    TimeValue getSocketTimeout() {
        return socketTimeout;
    }

    /**
     * Sets the {@linkplain Builder#setConnectTimeout(int) connect timeout} and {@linkplain Builder#setSocketTimeout(int) socket timeout}.
     *
     * @param requestConfigBuilder The request to configure.
     * @return Always {@code requestConfigBuilder}.
     */
    @Override
    public Builder customizeRequestConfig(Builder requestConfigBuilder) {
        if (connectTimeout != null) {
            requestConfigBuilder.setConnectTimeout((int)connectTimeout.millis());
        }
        if (socketTimeout != null) {
            requestConfigBuilder.setSocketTimeout((int)socketTimeout.millis());
        }

        return requestConfigBuilder;
    }

}
