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

package org.elasticsearch.xpack.security.authz.permission;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthAction;
import org.elasticsearch.action.admin.cluster.reroute.ClusterRerouteAction;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsAction;
import org.elasticsearch.action.admin.cluster.state.ClusterStateAction;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsAction;
import org.elasticsearch.action.admin.indices.create.CreateIndexAction;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexAction;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsAction;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateAction;
import org.elasticsearch.action.bulk.BulkAction;
import org.elasticsearch.action.delete.DeleteAction;
import org.elasticsearch.action.get.GetAction;
import org.elasticsearch.action.index.IndexAction;
import org.elasticsearch.action.search.SearchAction;
import org.elasticsearch.action.update.UpdateAction;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.transport.TransportRequest;
import org.elasticsearch.xpack.monitoring.action.MonitoringBulkAction;
import org.elasticsearch.xpack.security.authc.Authentication;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the built in reporting user role
 */
public class ReportingUserRoleTests extends ESTestCase {

    public void testCluster() {
        final TransportRequest request = new TransportRequest.Empty();
        final Authentication authentication = mock(Authentication.class);
        assertThat(ReportingUserRole.INSTANCE.cluster().check(ClusterHealthAction.NAME, request, authentication), is(false));
        assertThat(ReportingUserRole.INSTANCE.cluster().check(ClusterStateAction.NAME, request, authentication), is(false));
        assertThat(ReportingUserRole.INSTANCE.cluster().check(ClusterStatsAction.NAME, request, authentication), is(false));
        assertThat(ReportingUserRole.INSTANCE.cluster().check(PutIndexTemplateAction.NAME, request, authentication), is(false));
        assertThat(ReportingUserRole.INSTANCE.cluster().check(ClusterRerouteAction.NAME, request, authentication), is(false));
        assertThat(ReportingUserRole.INSTANCE.cluster().check(ClusterUpdateSettingsAction.NAME, request, authentication), is(false));
        assertThat(ReportingUserRole.INSTANCE.cluster().check(MonitoringBulkAction.NAME, request, authentication), is(false));
    }

    public void testRunAs() {
        assertThat(ReportingUserRole.INSTANCE.runAs().isEmpty(), is(true));
    }

    public void testUnauthorizedIndices() {
        assertThat(ReportingUserRole.INSTANCE.indices().allowedIndicesMatcher(SearchAction.NAME).test("foo"), is(false));
        assertThat(ReportingUserRole.INSTANCE.indices().allowedIndicesMatcher(SearchAction.NAME).test(".reporting"), is(false));
        assertThat(ReportingUserRole.INSTANCE.indices().allowedIndicesMatcher(SearchAction.NAME).test(".kibana"), is(false));
        assertThat(ReportingUserRole.INSTANCE.indices().allowedIndicesMatcher("indices:foo").test(randomAsciiOfLengthBetween(8, 24)),
                is(false));
    }

    public void testReadWriteAccess() {
        final String index = ".reporting-" + randomAsciiOfLength(randomIntBetween(0, 13));
        assertThat(ReportingUserRole.INSTANCE.indices().allowedIndicesMatcher("indices:foo").test(index), is(false));
        assertThat(ReportingUserRole.INSTANCE.indices().allowedIndicesMatcher("indices:bar").test(index), is(false));
        assertThat(ReportingUserRole.INSTANCE.indices().allowedIndicesMatcher(DeleteIndexAction.NAME).test(index), is(false));
        assertThat(ReportingUserRole.INSTANCE.indices().allowedIndicesMatcher(CreateIndexAction.NAME).test(index), is(false));
        assertThat(ReportingUserRole.INSTANCE.indices().allowedIndicesMatcher(UpdateSettingsAction.NAME).test(index), is(false));
        assertThat(ReportingUserRole.INSTANCE.indices().allowedIndicesMatcher(SearchAction.NAME).test(index), is(true));
        assertThat(ReportingUserRole.INSTANCE.indices().allowedIndicesMatcher(GetAction.NAME).test(index), is(true));
        assertThat(ReportingUserRole.INSTANCE.indices().allowedIndicesMatcher(IndexAction.NAME).test(index), is(true));
        assertThat(ReportingUserRole.INSTANCE.indices().allowedIndicesMatcher(UpdateAction.NAME).test(index), is(true));
        assertThat(ReportingUserRole.INSTANCE.indices().allowedIndicesMatcher(DeleteAction.NAME).test(index), is(true));
        assertThat(ReportingUserRole.INSTANCE.indices().allowedIndicesMatcher(BulkAction.NAME).test(index), is(true));
    }
}
