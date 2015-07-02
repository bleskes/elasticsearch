/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.license.plugin.core;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.collect.Tuple;

public class LicenseUtils {

    public final static String EXPIRED_FEATURE_HEADER = "es.license.expired.feature";

    /**
     * Exception to be thrown when a feature action requires a valid license, but license
     * has expired
     *
     * <code>feature</code> accessible through {@link #EXPIRED_FEATURE_HEADER} in the
     * exception's rest header
     */
    public static ElasticsearchException newExpirationException(String feature) {
        // TODO: after https://github.com/elastic/elasticsearch/pull/12006 use ElasicsearchException with addHeader(EXPIRED_FEATURE_HEADER, feature)
        return new ElasticsearchException.WithRestHeadersException("license expired for feature [" + feature + "]",
                Tuple.tuple(EXPIRED_FEATURE_HEADER, new String[] {feature}));
    }
}
