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

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.test.rest.ESRestTestCase;
import org.elasticsearch.test.rest.yaml.ClientYamlTestCandidate;
import org.elasticsearch.test.rest.yaml.ESClientYamlSuiteTestCase;
import org.elasticsearch.xpack.security.authc.support.SecuredString;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.elasticsearch.xpack.security.authc.support.UsernamePasswordToken.basicAuthHeaderValue;

public class SmokeTestPluginsSslClientYamlTestSuiteIT extends ESClientYamlSuiteTestCase {

    private static final String USER = "test_user";
    private static final String PASS = "changeme";
    private static final String KEYSTORE_PASS = "keypass";

    public SmokeTestPluginsSslClientYamlTestSuiteIT(@Name("yaml") ClientYamlTestCandidate testCandidate) {
        super(testCandidate);
    }

    @ParametersFactory
    public static Iterable<Object[]> parameters() throws IOException {
        return ESClientYamlSuiteTestCase.createParameters();
    }

    static Path keyStore;

    @BeforeClass
    public static void getKeyStore() {
      try {
          keyStore = PathUtils.get(SmokeTestPluginsSslClientYamlTestSuiteIT.class.getResource("/test-node.jks").toURI());
      } catch (URISyntaxException e) {
          throw new ElasticsearchException("exception while reading the store", e);
      }
      if (!Files.exists(keyStore)) {
          throw new IllegalStateException("Keystore file [" + keyStore + "] does not exist.");
      }
    }

    @AfterClass
    public static void clearKeyStore() {
      keyStore = null;
    }

    @Override
    protected Settings restClientSettings() {
        String token = basicAuthHeaderValue(USER, new SecuredString(PASS.toCharArray()));
        return Settings.builder()
                .put(ThreadContext.PREFIX + ".Authorization", token)
                .put(ESRestTestCase.TRUSTSTORE_PATH, keyStore)
                .put(ESRestTestCase.TRUSTSTORE_PASSWORD, KEYSTORE_PASS)
                .build();
    }

    @Override
    protected String getProtocol() {
        return "https";
    }
}
