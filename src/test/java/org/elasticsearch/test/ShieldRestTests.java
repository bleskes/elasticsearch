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

package org.elasticsearch.test;

import com.carrotsearch.randomizedtesting.annotations.Name;
import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;
import org.apache.lucene.util.AbstractRandomizedTest;
import org.elasticsearch.client.support.Headers;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.PluginsService;
import org.elasticsearch.shield.authc.support.SecuredStringTests;
import org.elasticsearch.shield.signature.InternalSignatureService;
import org.elasticsearch.shield.ShieldPlugin;
import org.elasticsearch.shield.transport.netty.NettySecuredTransport;
import org.elasticsearch.shield.authc.support.SecuredString;
import org.elasticsearch.shield.authc.support.UsernamePasswordToken;
import org.elasticsearch.test.rest.ElasticsearchRestTests;
import org.elasticsearch.test.rest.RestTestCandidate;
import org.elasticsearch.test.rest.client.RestException;
import org.elasticsearch.test.rest.parser.RestTestParseException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.elasticsearch.shield.authc.support.UsernamePasswordToken.basicAuthHeaderValue;

/**
 * Allows to run Elasticsearch REST tests against a cluster with shield installed.
 * Subclasses {@link org.elasticsearch.test.ShieldIntegrationTest} that contains all the needed code to override the global
 * cluster settings and make sure shield is properly installed and configured.
 * Delegates all of the tests to {@link org.elasticsearch.test.rest.ElasticsearchRestTests}.
 */
@AbstractRandomizedTest.Rest
@ElasticsearchIntegrationTest.ClusterScope(randomDynamicTemplates = false)
public class ShieldRestTests extends ShieldIntegrationTest {

    private final ElasticsearchRestTests delegate;

    public ShieldRestTests(@Name("yaml") RestTestCandidate testCandidate) {
        delegate = new ElasticsearchRestTests(testCandidate) {
            @Override
            protected Settings restClientSettings() {
                return ImmutableSettings.builder()
                        .put(Headers.PREFIX + "." + UsernamePasswordToken.BASIC_AUTH_HEADER, basicAuthHeaderValue(ShieldSettingsSource.DEFAULT_USER_NAME,
                                new SecuredString(ShieldSettingsSource.DEFAULT_PASSWORD.toCharArray()))).build();
            }
        };
    }

    @ParametersFactory
    public static Iterable<Object[]> parameters() throws IOException, RestTestParseException {
        return ElasticsearchRestTests.parameters();
    }

    @BeforeClass
    public static void initExecutionContext() throws IOException, RestException {
        ElasticsearchRestTests.initExecutionContext();
    }

    @AfterClass
    public static void close() {
        ElasticsearchRestTests.close();
    }

    @Test
    public void test() throws IOException {
        delegate.test();
    }

    @Before
    public void reset() throws IOException, RestException {
        delegate.reset();
    }
}
