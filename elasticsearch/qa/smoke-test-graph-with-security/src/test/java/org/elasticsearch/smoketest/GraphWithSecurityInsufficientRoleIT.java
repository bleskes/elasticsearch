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

package org.elasticsearch.smoketest;

import com.carrotsearch.randomizedtesting.annotations.Name;
import org.elasticsearch.test.rest.RestTestCandidate;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;

public class GraphWithSecurityInsufficientRoleIT extends GraphWithSecurityIT {

    public GraphWithSecurityInsufficientRoleIT(@Name("yaml") RestTestCandidate testCandidate) {
        super(testCandidate);
    }

    public void test() throws IOException {
        try {
            super.test();
            fail();
        } catch(AssertionError ae) {
            assertThat(ae.getMessage(), containsString("action [indices:data/read/xpack/graph/explore"));
            assertThat(ae.getMessage(), containsString("returned [403 Forbidden]"));
            assertThat(ae.getMessage(), containsString("is unauthorized for user [no_graph_explorer]"));
        }
    }

    @Override
    protected String[] getCredentials() {
        return new String[]{"no_graph_explorer", "changeme"};
    }
}

