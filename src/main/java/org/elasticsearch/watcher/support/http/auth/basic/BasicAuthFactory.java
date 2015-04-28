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

package org.elasticsearch.watcher.support.http.auth.basic;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.support.http.auth.HttpAuthFactory;
import org.elasticsearch.watcher.support.secret.SecretService;

import java.io.IOException;

/**
 *
 */
public class BasicAuthFactory extends HttpAuthFactory<BasicAuth, ApplicableBasicAuth> {

    private final SecretService secretService;

    @Inject
    public BasicAuthFactory(SecretService secretService) {
        this.secretService = secretService;
    }

    public String type() {
        return BasicAuth.TYPE;
    }

    public BasicAuth parse(XContentParser parser) throws IOException {
        return BasicAuth.parse(parser);
    }

    @Override
    public ApplicableBasicAuth createApplicable(BasicAuth auth) {
        return new ApplicableBasicAuth(auth, secretService);
    }
}
