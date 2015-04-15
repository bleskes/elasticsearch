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

package org.elasticsearch.shield.authz;

import org.elasticsearch.shield.authc.support.Hasher;
import org.elasticsearch.shield.authc.support.SecuredString;
import org.elasticsearch.test.ShieldIntegrationTest;
import org.junit.Test;

import static org.elasticsearch.shield.authc.support.UsernamePasswordToken.BASIC_AUTH_HEADER;
import static org.elasticsearch.shield.authc.support.UsernamePasswordToken.basicAuthHeaderValue;
import static org.hamcrest.CoreMatchers.containsString;

public class AnalyzeTests extends ShieldIntegrationTest {

    protected static final String USERS_PASSWD_HASHED = new String(Hasher.BCRYPT.hash(new SecuredString("test123".toCharArray())));

    @Override
    protected String configUsers() {
        return super.configUsers() +
                "analyze_indices:" + USERS_PASSWD_HASHED + "\n" +
                "analyze_cluster:" + USERS_PASSWD_HASHED + "\n";
    }

    @Override
    protected String configUsersRoles() {
        return super.configUsersRoles() +
                "analyze_indices:analyze_indices\n" +
                "analyze_cluster:analyze_cluster\n";
    }

    @Override
    protected String configRoles() {
        return super.configRoles()+ "\n" +
                //role that has analyze indices privileges only
                "analyze_indices:\n" +
                "  indices:\n" +
                "    'test_*': indices:admin/analyze\n" +
                "analyze_cluster:\n" +
                "  cluster:\n" +
                "    - cluster:admin/analyze\n";
    }

    @Test
    public void testAnalyzeWithIndices() {
        //this test tries to execute different analyze api variants from a user that has analyze privileges only on a specific index namespace

        createIndex("test_1");
        ensureGreen();

        //ok: user has permissions for analyze on test_*
        client().admin().indices().prepareAnalyze("this is my text").setIndex("test_1").setAnalyzer("standard")
                .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("analyze_indices", new SecuredString("test123".toCharArray()))).get();

        try {
            //fails: user doesn't have permissions for analyze on index non_authorized
            client().admin().indices().prepareAnalyze("this is my text").setIndex("non_authorized").setAnalyzer("standard")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("analyze_indices", new SecuredString("test123".toCharArray()))).get();
        } catch(AuthorizationException e) {
            assertThat(e.getMessage(), containsString("action [indices:admin/analyze] is unauthorized for user [analyze_indices]"));
        }

        try {
            //fails: user doesn't have permissions for cluster level analyze
            client().admin().indices().prepareAnalyze("this is my text").setAnalyzer("standard")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("analyze_indices", new SecuredString("test123".toCharArray()))).get();
        } catch(AuthorizationException e) {
            assertThat(e.getMessage(), containsString("action [cluster:admin/analyze] is unauthorized for user [analyze_indices]"));
        }
    }

    @Test
    public void testAnalyzeWithoutIndices() {
        //this test tries to execute different analyze api variants from a user that has analyze privileges only at cluster level

        try {
            //fails: user doesn't have permissions for analyze on index test_1
            client().admin().indices().prepareAnalyze("this is my text").setIndex("test_1").setAnalyzer("standard")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("analyze_cluster", new SecuredString("test123".toCharArray()))).get();
        } catch(AuthorizationException e) {
            assertThat(e.getMessage(), containsString("action [indices:admin/analyze] is unauthorized for user [analyze_cluster]"));
        }

        client().admin().indices().prepareAnalyze("this is my text").setAnalyzer("standard")
                    .putHeader(BASIC_AUTH_HEADER, basicAuthHeaderValue("analyze_cluster", new SecuredString("test123".toCharArray()))).get();
    }
}
