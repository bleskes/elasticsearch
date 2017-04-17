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
import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;

import org.elasticsearch.common.settings.SecureString;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.test.rest.yaml.ClientYamlTestCandidate;
import org.elasticsearch.test.rest.yaml.ESClientYamlSuiteTestCase;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.elasticsearch.xpack.security.authc.support.UsernamePasswordToken.basicAuthHeaderValue;


public class SmokeTestWatcherWithSecurityClientYamlTestSuiteIT extends ESClientYamlSuiteTestCase {

    private static final String TEST_ADMIN_USERNAME = "test_admin";
    private static final String TEST_ADMIN_PASSWORD = "changeme";

    public SmokeTestWatcherWithSecurityClientYamlTestSuiteIT(@Name("yaml") ClientYamlTestCandidate testCandidate) {
        super(testCandidate);
    }

    @ParametersFactory
    public static Iterable<Object[]> parameters() throws IOException {
        return ESClientYamlSuiteTestCase.createParameters();
    }

    @Before
    public void startWatcher() throws Exception {
        getAdminExecutionContext().callApi("xpack.watcher.start", emptyMap(), emptyList(), emptyMap());
    }

    @After
    public void stopWatcher() throws Exception {
        getAdminExecutionContext().callApi("xpack.watcher.stop", emptyMap(), emptyList(), emptyMap());
    }

    @Override
    protected Settings restClientSettings() {
        String token = basicAuthHeaderValue("watcher_manager", new SecureString("changeme".toCharArray()));
        return Settings.builder()
                .put(ThreadContext.PREFIX + ".Authorization", token)
                .build();
    }

    @Override
    protected Settings restAdminSettings() {
        String token = basicAuthHeaderValue(TEST_ADMIN_USERNAME, new SecureString(TEST_ADMIN_PASSWORD.toCharArray()));
        return Settings.builder()
            .put(ThreadContext.PREFIX + ".Authorization", token)
            .build();
    }
}

