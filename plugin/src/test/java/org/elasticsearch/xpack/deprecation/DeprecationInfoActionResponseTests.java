/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2017 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.deprecation;

import org.elasticsearch.Build;
import org.elasticsearch.Version;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.LocalTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.test.AbstractStreamableTestCase;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsEqual.equalTo;

public class DeprecationInfoActionResponseTests extends AbstractStreamableTestCase<DeprecationInfoAction.Response> {

    @Override
    protected DeprecationInfoAction.Response createTestInstance() {
        List<DeprecationIssue> clusterIssues = Stream.generate(DeprecationIssueTests::createTestInstance)
            .limit(randomIntBetween(0, 10)).collect(Collectors.toList());
        List<DeprecationIssue> nodeIssues = Stream.generate(DeprecationIssueTests::createTestInstance)
            .limit(randomIntBetween(0, 10)).collect(Collectors.toList());
        Map<String, List<DeprecationIssue>> indexIssues = new HashMap<>();
        for (int i = 0; i < randomIntBetween(0, 10); i++) {
            List<DeprecationIssue> perIndexIssues = Stream.generate(DeprecationIssueTests::createTestInstance)
                .limit(randomIntBetween(0, 10)).collect(Collectors.toList());
            indexIssues.put(randomAlphaOfLength(10), perIndexIssues);
        }
        return new DeprecationInfoAction.Response(clusterIssues, nodeIssues, indexIssues);
    }

    @Override
    protected DeprecationInfoAction.Response createBlankInstance() {
        return new DeprecationInfoAction.Response();
    }

    public void testFrom() throws IOException {
        XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject("_all");
        mapping.field("enabled", false);
        mapping.endObject().endObject();

        MetaData metadata = MetaData.builder().put(IndexMetaData.builder("test")
            .putMapping("testUnderscoreAll", mapping.string())
            .settings(settings(Version.CURRENT))
            .numberOfShards(1)
            .numberOfReplicas(0))
            .build();

        DiscoveryNode discoveryNode = DiscoveryNode.createLocal(Settings.EMPTY,
            new LocalTransportAddress("_fake_this"), "test");
        ClusterState state = ClusterState.builder(ClusterName.DEFAULT).metaData(metadata).build();
        List<NodeInfo> nodeInfos = Collections.singletonList(new NodeInfo(Version.CURRENT, Build.CURRENT,
            discoveryNode, null, null, null, null,
            null, null, null, null, null, null));
        IndexNameExpressionResolver resolver = new IndexNameExpressionResolver(Settings.EMPTY);
        IndicesOptions indicesOptions = IndicesOptions.fromOptions(false, false,
            true, true);
        boolean clusterIssueFound = randomBoolean();
        boolean nodeIssueFound = randomBoolean();
        boolean indexIssueFound = randomBoolean();
        DeprecationIssue foundIssue = DeprecationIssueTests.createTestInstance();
        List<BiFunction<List<NodeInfo>, ClusterState, DeprecationIssue>> clusterSettingsChecks =
            Collections.unmodifiableList(Arrays.asList(
                (ln, s) -> clusterIssueFound ? foundIssue : null
            ));
        List<BiFunction<List<NodeInfo>, ClusterState, DeprecationIssue>> nodeSettingsChecks =
            Collections.unmodifiableList(Arrays.asList(
                (ln, s) -> nodeIssueFound ? foundIssue : null
            ));

        List<Function<IndexMetaData, DeprecationIssue>> indexSettingsChecks =
            Collections.unmodifiableList(Arrays.asList(
                (idx) -> indexIssueFound ? foundIssue : null
            ));

        DeprecationInfoAction.Response response = DeprecationInfoAction.Response.from(nodeInfos, state,
            resolver, Strings.EMPTY_ARRAY, indicesOptions,
            clusterSettingsChecks, nodeSettingsChecks, indexSettingsChecks);

        if (clusterIssueFound) {
            assertThat(response.getClusterSettingsIssues(), equalTo(Collections.singletonList(foundIssue)));
        } else {
            assertThat(response.getClusterSettingsIssues(), empty());
        }

        if (nodeIssueFound) {
            assertThat(response.getNodeSettingsIssues(), equalTo(Collections.singletonList(foundIssue)));
        } else {
            assertTrue(response.getNodeSettingsIssues().isEmpty());
        }

        if (indexIssueFound) {
            assertThat(response.getIndexSettingsIssues(), equalTo(Collections.singletonMap("test",
                Collections.singletonList(foundIssue))));
        } else {
            assertTrue(response.getIndexSettingsIssues().isEmpty());
        }
    }
}
