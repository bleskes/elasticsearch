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

package org.elasticsearch.shield.test;

import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.shield.authc.AuthenticationException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ShieldAssertions {

    public static void assertContainsWWWAuthenticateHeader(AuthenticationException e) {
        assertThat(e.status(), is(RestStatus.UNAUTHORIZED));
        assertThat(e.getHeaders(), hasKey("WWW-Authenticate"));
        assertThat(e.getHeaders().get("WWW-Authenticate"), hasSize(1));
        assertThat(e.getHeaders().get("WWW-Authenticate").get(0), is(AuthenticationException.HEADERS.entrySet().iterator().next().getValue().get(0)));
    }
}
