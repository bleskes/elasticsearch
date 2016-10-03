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

package org.elasticsearch.xpack.watcher.support.xcontent;

import org.elasticsearch.common.xcontent.ToXContent;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

/**
 *
 */
public class WatcherParams extends ToXContent.DelegatingMapParams {

    public static final WatcherParams HIDE_SECRETS = WatcherParams.builder().hideSecrets(true).build();

    static final String HIDE_SECRETS_KEY = "hide_secrets";
    static final String COLLAPSE_ARRAYS_KEY = "collapse_arrays";
    static final String DEBUG_KEY = "debug";

    public static boolean hideSecrets(ToXContent.Params params) {
        return wrap(params).hideSecrets();
    }

    public static boolean collapseArrays(ToXContent.Params params) {
        return wrap(params).collapseArrays();
    }

    public static boolean debug(ToXContent.Params params) {
        return wrap(params).debug();
    }

    private WatcherParams(Map<String, String> params, ToXContent.Params delegate) {
        super(params, delegate);
    }

    public boolean hideSecrets() {
        return paramAsBoolean(HIDE_SECRETS_KEY, false);
    }

    public boolean collapseArrays() {
        return paramAsBoolean(COLLAPSE_ARRAYS_KEY, false);
    }

    public boolean debug() {
        return paramAsBoolean(DEBUG_KEY, false);
    }

    public static WatcherParams wrap(ToXContent.Params params) {
        return params instanceof WatcherParams ?
                (WatcherParams) params :
                new WatcherParams(emptyMap(), params);
    }

    public static Builder builder() {
        return builder(ToXContent.EMPTY_PARAMS);
    }

    public static Builder builder(ToXContent.Params delegate) {
        return new Builder(delegate);
    }

    public static class Builder {

        private final ToXContent.Params delegate;
        private final Map<String, String> params = new HashMap<>();

        private Builder(ToXContent.Params delegate) {
            this.delegate = delegate;
        }

        public Builder hideSecrets(boolean hideSecrets) {
            params.put(HIDE_SECRETS_KEY, String.valueOf(hideSecrets));
            return this;
        }

        public Builder collapseArrays(boolean collapseArrays) {
            params.put(COLLAPSE_ARRAYS_KEY, String.valueOf(collapseArrays));
            return this;
        }

        public Builder debug(boolean debug) {
            params.put(DEBUG_KEY, String.valueOf(debug));
            return this;
        }

        public Builder put(String key, Object value) {
            params.put(key, String.valueOf(value));
            return this;
        }

        public WatcherParams build() {
            return new WatcherParams(unmodifiableMap(new HashMap<>(params)), delegate);
        }
    }
}
