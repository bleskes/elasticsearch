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

import org.elasticsearch.Version;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;

/**
 * This class is used to keep track of changes that we might have to make once we upgrade versions of dependencies, especially elasticsearch core.
 * Every change is listed as a specific assert that trips with a future version of es core, with a meaningful description that explains what needs to be done.
 *
 * For each assertion we should have one or more corresponding TODOs in the code points that require changes, and also a link to the issue that applies the
 * required fixes upstream.
 *
 * NOTE: changes suggested by asserts descriptions may break backwards compatibility. The same shield jar is supposed to work against multiple es core versions,
 * thus if we make a change in shield that requires e.g. es core 1.4.1 it means that the next shield release won't support es core 1.4.0 anymore.
 * In many cases we will just have to bump the version of the assert then, unless we want to break backwards compatibility, but the idea is that this class
 * helps keeping track of this and eventually making changes when needed.
 */
public class VersionCompatibilityTests extends ElasticsearchTestCase {

    @Test
    public void testCompatibility() {
        /**
         * see https://github.com/elasticsearch/elasticsearch/issues/9372 {@link org.elasticsearch.shield.license.LicenseService}
         * Once es core supports merging cluster level custom metadata (licenses in our case), the tribe node will see some license coming from the tribe and everything will be ok.
         *
         */
        assertThat("Remove workaround in LicenseService class when es core supports merging cluster level custom metadata", Version.CURRENT.onOrBefore(Version.V_1_5_0), is(true));

        /**
         * see https://github.com/elastic/elasticsearch/pull/10319 {@link org.elasticsearch.transport.netty.ShieldMessageChannelHandler}
         * Once ES core supports exposing the channel in {@link org.elasticsearch.transport.netty.NettyTransportChannel}
         * we should implement the certificate extraction logic as a {@link org.elasticsearch.shield.transport.ServerTransportFilter}
         */
        assertThat("Remove ShieldMessageChannelHandler and implement PKI cert extraction as a ServerTransportFilter", Version.CURRENT.onOrBefore(Version.V_1_5_0), is(true));

        /**
         * see https://github.com/elastic/elasticsearch/pull/10323 {@link org.elasticsearch.rest.FakeRestRequest}
         * ES core has FakeRestRequest but it is not included in the test jar. Once it is included in the test jar, Shield
         * should be updated to remove the copied version of the class {@link org.elasticsearch.rest.FakeRestRequest}
         */
        assertThat("Remove FakeRestRequest and use version in core", Version.CURRENT.onOrBefore(Version.V_1_5_0), is(true));
    }
}
