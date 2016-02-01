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

package org.elasticsearch.shield;

import org.elasticsearch.test.ESTestCase;

import static org.hamcrest.Matchers.is;

/**
 *
 */
public class SystemInternalUserTests extends ESTestCase {

    public void testIsAuthorized() throws Exception {
        assertThat(InternalSystemUser.isAuthorized("indices:monitor/whatever"), is(true));
        assertThat(InternalSystemUser.isAuthorized("cluster:monitor/whatever"), is(true));
        assertThat(InternalSystemUser.isAuthorized("internal:whatever"), is(true));
        assertThat(InternalSystemUser.isAuthorized("cluster:admin/reroute"), is(true));
        assertThat(InternalSystemUser.isAuthorized("cluster:admin/whatever"), is(false));
        assertThat(InternalSystemUser.isAuthorized("indices:whatever"), is(false));
        assertThat(InternalSystemUser.isAuthorized("cluster:whatever"), is(false));
        assertThat(InternalSystemUser.isAuthorized("whatever"), is(false));
    }
}
