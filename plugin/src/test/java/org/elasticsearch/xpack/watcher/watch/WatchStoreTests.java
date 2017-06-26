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

package org.elasticsearch.xpack.watcher.watch;

import org.elasticsearch.Version;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.routing.IndexRoutingTable;
import org.elasticsearch.cluster.routing.IndexShardRoutingTable;
import org.elasticsearch.cluster.routing.RoutingTable;
import org.elasticsearch.cluster.routing.ShardRoutingState;
import org.elasticsearch.cluster.routing.TestShardRouting;
import org.elasticsearch.cluster.routing.UnassignedInfo;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.engine.DocumentMissingException;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.indices.TypeMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.VersionUtils;
import org.elasticsearch.xpack.watcher.actions.ActionWrapper;
import org.elasticsearch.xpack.watcher.actions.ExecutableAction;
import org.elasticsearch.xpack.watcher.condition.AlwaysCondition;
import org.elasticsearch.xpack.watcher.condition.NeverCondition;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.input.none.ExecutableNoneInput;
import org.elasticsearch.xpack.watcher.support.init.proxy.WatcherClientProxy;
import org.elasticsearch.xpack.watcher.support.xcontent.XContentSource;
import org.elasticsearch.xpack.watcher.transform.ExecutableTransform;
import org.elasticsearch.xpack.watcher.transform.Transform;
import org.elasticsearch.xpack.watcher.trigger.schedule.Schedule;
import org.elasticsearch.xpack.watcher.trigger.schedule.ScheduleTrigger;
import org.hamcrest.Matchers;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class WatchStoreTests extends ESTestCase {

    private WatchStore watchStore;
    private WatcherClientProxy clientProxy;
    private Watch.Parser parser;

    @Before
    public void init() {
        clientProxy = mock(WatcherClientProxy.class);
        parser = mock(Watch.Parser.class);
        watchStore = new WatchStore(Settings.EMPTY, clientProxy, parser);
    }

    public void testStartNoPreviousWatchesIndex() throws Exception {
        ClusterState.Builder csBuilder = new ClusterState.Builder(new ClusterName("_name"));
        MetaData.Builder metaDataBuilder = MetaData.builder();
        csBuilder.metaData(metaDataBuilder);
        ClusterState cs = csBuilder.build();

        assertThat(watchStore.validate(cs), is(true));
        watchStore.start(cs);
        assertThat(watchStore.started(), is(true));
        assertThat(watchStore.watches().size(), equalTo(0));
        verifyZeroInteractions(clientProxy);

        watchStore.start(cs);
        verifyZeroInteractions(clientProxy);
    }

    public void testStartPrimaryShardNotReady() {
        ClusterState.Builder csBuilder = new ClusterState.Builder(new ClusterName("_name"));
        MetaData.Builder metaDataBuilder = MetaData.builder();
        RoutingTable.Builder routingTableBuilder = RoutingTable.builder();
        Settings settings = settings(Version.CURRENT)
                .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1)
                .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 1)
                .build();
        metaDataBuilder.put(IndexMetaData.builder(WatchStore.INDEX).settings(settings).numberOfShards(1).numberOfReplicas(1));
        final Index index = metaDataBuilder.get(WatchStore.INDEX).getIndex();
        IndexRoutingTable.Builder indexRoutingTableBuilder = IndexRoutingTable.builder(index);
        indexRoutingTableBuilder.addIndexShard(new IndexShardRoutingTable.Builder(new ShardId(index, 0))
                .addShard(TestShardRouting.newShardRouting(WatchStore.INDEX, 0, "_node_id", null, true,
                        ShardRoutingState.UNASSIGNED, new UnassignedInfo(UnassignedInfo.Reason.INDEX_CREATED, "")))
                .build());
        indexRoutingTableBuilder.addReplica();
        routingTableBuilder.add(indexRoutingTableBuilder.build());
        csBuilder.metaData(metaDataBuilder);
        csBuilder.routingTable(routingTableBuilder.build());

        ClusterState cs = csBuilder.build();
        assertThat(watchStore.validate(cs), is(false));
        verifyZeroInteractions(clientProxy);
    }

    public void testStartRefreshFailed() {
        ClusterState.Builder csBuilder = new ClusterState.Builder(new ClusterName("_name"));
        createWatchIndexMetaData(csBuilder, VersionUtils.randomVersion(random()));

        RefreshResponse refreshResponse = mockRefreshResponse(1, 0);
        when(clientProxy.refresh(any(RefreshRequest.class))).thenReturn(refreshResponse);

        ClusterState cs = csBuilder.build();

        assertThat(watchStore.validate(cs), is(true));
        try {
            watchStore.start(cs);
        } catch (Exception e) {
            assertThat(e.getMessage(), equalTo("not all required shards have been refreshed"));
        }
        verify(clientProxy, times(1)).refresh(any(RefreshRequest.class));
        verify(clientProxy, never()).search(any(SearchRequest.class), any(TimeValue.class));
        verify(clientProxy, never()).clearScroll(anyString());
    }

    public void testStartSearchFailed() {
        ClusterState.Builder csBuilder = new ClusterState.Builder(new ClusterName("_name"));
        createWatchIndexMetaData(csBuilder, VersionUtils.randomVersion(random()));

        RefreshResponse refreshResponse = mockRefreshResponse(1, 1);
        when(clientProxy.refresh(any(RefreshRequest.class))).thenReturn(refreshResponse);

        SearchResponse searchResponse = mockSearchResponse(1, 0, 0);
        when(clientProxy.search(any(SearchRequest.class), any(TimeValue.class))).thenReturn(searchResponse);

        when(clientProxy.clearScroll(anyString())).thenReturn(new ClearScrollResponse(true, 0));

        ClusterState cs = csBuilder.build();
        assertThat(watchStore.validate(cs), is(true));
        try {
            watchStore.start(cs);
        } catch (Exception e) {
            assertThat(e.getMessage(), equalTo("Partial response while loading watches"));
        }
        verify(clientProxy, times(1)).refresh(any(RefreshRequest.class));
        verify(clientProxy, times(1)).search(any(SearchRequest.class), any(TimeValue.class));
        verify(clientProxy, times(1)).clearScroll(anyString());
    }

    public void testStartNoWatchStored() throws Exception {
        ClusterState.Builder csBuilder = new ClusterState.Builder(new ClusterName("_name"));
        createWatchIndexMetaData(csBuilder, VersionUtils.randomVersion(random()));

        RefreshResponse refreshResponse = mockRefreshResponse(1, 1);
        when(clientProxy.refresh(any(RefreshRequest.class))).thenReturn(refreshResponse);

        SearchResponse searchResponse = mockSearchResponse(1, 1, 0);
        when(clientProxy.search(any(SearchRequest.class), any(TimeValue.class))).thenReturn(searchResponse);

        when(clientProxy.clearScroll(anyString())).thenReturn(new ClearScrollResponse(true, 0));

        ClusterState cs = csBuilder.build();
        assertThat(watchStore.validate(cs), is(true));
        watchStore.start(cs);
        assertThat(watchStore.started(), is(true));
        assertThat(watchStore.watches().size(), equalTo(0));
        verify(clientProxy, times(1)).refresh(any(RefreshRequest.class));
        verify(clientProxy, times(1)).search(any(SearchRequest.class), any(TimeValue.class));
        verify(clientProxy, times(1)).clearScroll(anyString());
    }

    public void testStartWatchStored() throws Exception {
        boolean requireUpgradeSource = randomBoolean();
        Version indexVersion;
        if (requireUpgradeSource) {
            indexVersion = VersionUtils.randomVersionBetween(random(), Version.V_2_0_0, Version.V_2_4_4);
        } else {
            indexVersion = VersionUtils.randomVersionBetween(random(), Version.V_5_0_0_alpha1, Version.CURRENT);
        }
        logger.info("Using index version [{}]", indexVersion);
        ClusterState.Builder csBuilder = new ClusterState.Builder(new ClusterName("_name"));
        createWatchIndexMetaData(csBuilder, indexVersion);

        RefreshResponse refreshResponse = mockRefreshResponse(1, 1);
        when(clientProxy.refresh(any(RefreshRequest.class))).thenReturn(refreshResponse);

        BytesReference source = new BytesArray("{}");
        SearchHit hit1 = new SearchHit(0, "_id1", new Text("type"), Collections.<String, SearchHitField>emptyMap());
        hit1.sourceRef(source);
        SearchHit hit2 = new SearchHit(1, "_id2", new Text("type"), Collections.<String, SearchHitField>emptyMap());
        hit2.sourceRef(source);
        SearchResponse searchResponse1 = mockSearchResponse(1, 1, 2, hit1, hit2);

        when(clientProxy.search(any(SearchRequest.class), any(TimeValue.class))).thenReturn(searchResponse1);

        SearchHit hit3 = new SearchHit(2, "_id3", new Text("type"), Collections.<String, SearchHitField>emptyMap());
        hit3.sourceRef(source);
        SearchHit hit4 = new SearchHit(3, "_id4", new Text("type"), Collections.<String, SearchHitField>emptyMap());
        hit4.sourceRef(source);
        SearchResponse searchResponse2 = mockSearchResponse(1, 1, 2, hit3, hit4);
        SearchResponse searchResponse3 = mockSearchResponse(1, 1, 2);
        when(clientProxy.searchScroll(anyString(), any(TimeValue.class))).thenReturn(searchResponse2, searchResponse3);

        Watch watch1 = mock(Watch.class);
        WatchStatus status = mock(WatchStatus.class);
        when(watch1.status()).thenReturn(status);
        Watch watch2 = mock(Watch.class);
        when(watch2.status()).thenReturn(status);
        Watch watch3 = mock(Watch.class);
        when(watch3.status()).thenReturn(status);
        Watch watch4 = mock(Watch.class);
        when(watch4.status()).thenReturn(status);
        when(parser.parse("_id1", true, source, XContentType.JSON, requireUpgradeSource)).thenReturn(watch1);
        when(parser.parse("_id2", true, source, XContentType.JSON, requireUpgradeSource)).thenReturn(watch2);
        when(parser.parse("_id3", true, source, XContentType.JSON, requireUpgradeSource)).thenReturn(watch3);
        when(parser.parse("_id4", true, source, XContentType.JSON, requireUpgradeSource)).thenReturn(watch4);

        when(clientProxy.clearScroll(anyString())).thenReturn(new ClearScrollResponse(true, 0));

        ClusterState cs = csBuilder.build();
        assertThat(watchStore.validate(cs), is(true));
        watchStore.start(cs);
        assertThat(watchStore.started(), is(true));
        assertThat(watchStore.watches().size(), equalTo(4));
        verify(clientProxy, times(1)).refresh(any(RefreshRequest.class));
        verify(clientProxy, times(1)).search(any(SearchRequest.class), any(TimeValue.class));
        verify(clientProxy, times(2)).searchScroll(anyString(), any(TimeValue.class));
        verify(clientProxy, times(1)).clearScroll(anyString());
    }

    public void testUsageStats() throws Exception {
        ClusterState.Builder csBuilder = new ClusterState.Builder(new ClusterName("_name"));
        createWatchIndexMetaData(csBuilder, VersionUtils.randomVersion(random()));

        RefreshResponse refreshResponse = mockRefreshResponse(1, 1);
        when(clientProxy.refresh(any(RefreshRequest.class))).thenReturn(refreshResponse);

        BytesReference source = new BytesArray("{}");
        int hitCount = randomIntBetween(50, 100);
        int activeHitCount = 0;

        List<SearchHit> hits = new ArrayList<>();
        for (int i = 0; i < hitCount; i++) {
            SearchHit hit = new SearchHit(0, "_id" + i, new Text("type"), Collections.<String, SearchHitField>emptyMap());
            hits.add(hit.sourceRef(source));

            Watch watch = mock(Watch.class);
            WatchStatus status = mock(WatchStatus.class);
            when(watch.status()).thenReturn(status);

            boolean isActive = usually();
            WatchStatus.State state = mock(WatchStatus.State.class);
            when(state.isActive()).thenReturn(isActive);
            when(status.state()).thenReturn(state);
            if (isActive) {
                activeHitCount++;
            }

            // random schedule
            ScheduleTrigger mockTricker = mock(ScheduleTrigger.class);
            when(watch.trigger()).thenReturn(mockTricker);
            when(mockTricker.type()).thenReturn("schedule");
            String scheduleType = randomFrom("a", "b", "c");
            Schedule mockSchedule = mock(Schedule.class);
            when(mockSchedule.type()).thenReturn(scheduleType);
            when(mockTricker.getSchedule()).thenReturn(mockSchedule);

            // either a none input, or null
            when(watch.input()).thenReturn(randomFrom(new ExecutableNoneInput(logger), null));

            // random conditions
            when(watch.condition()).thenReturn(randomFrom(AlwaysCondition.INSTANCE, null,
                    NeverCondition.INSTANCE));

            // random actions
            ActionWrapper actionWrapper = mock(ActionWrapper.class);
            ExecutableAction action = mock(ExecutableAction.class);
            when(actionWrapper.action()).thenReturn(action);
            when(action.type()).thenReturn(randomFrom("a", "b", "c"));
            when(watch.actions()).thenReturn(Arrays.asList(actionWrapper));

            // random transform, not always set
            Transform mockTransform = mock(Transform.class);
            when(mockTransform.type()).thenReturn("TYPE");

            @SuppressWarnings("unchecked")
            ExecutableTransform testTransform = new ExecutableTransform(mockTransform, logger) {
                @Override
                public Transform.Result execute(WatchExecutionContext ctx, Payload payload) {
                    return null;
                }
            };
            when(watch.transform()).thenReturn(randomFrom(testTransform, null));

            when(parser.parse(eq("_id" + i), eq(true), eq(source), eq(XContentType.JSON), anyBoolean())).thenReturn(watch);
        }

        SearchResponse searchResponse = mockSearchResponse(1, 1, hitCount, hits.toArray(new SearchHit[] {}));
        when(clientProxy.search(any(SearchRequest.class), any(TimeValue.class))).thenReturn(searchResponse);
        SearchResponse noHitsResponse = mockSearchResponse(1, 1, 2);
        when(clientProxy.searchScroll(anyString(), any(TimeValue.class))).thenReturn(noHitsResponse);
        when(clientProxy.clearScroll(anyString())).thenReturn(new ClearScrollResponse(true, 0));

        ClusterState cs = csBuilder.build();
        watchStore.start(cs);

        XContentSource stats = new XContentSource(jsonBuilder().map(watchStore.usageStats()));

        assertThat(stats.getValue("count.total"), is(hitCount));
        assertThat(stats.getValue("count.active"), is(activeHitCount));

        // schedule count
        int scheduleCountA = stats.getValue("watch.trigger.schedule.a.active");
        int scheduleCountB = stats.getValue("watch.trigger.schedule.b.active");
        int scheduleCountC = stats.getValue("watch.trigger.schedule.c.active");
        assertThat(scheduleCountA + scheduleCountB + scheduleCountC, is(activeHitCount));

        // input count
        assertThat(stats.getValue("watch.input.none.active"), is(greaterThan(0)));
        assertThat(stats.getValue("watch.input.none.total"), is(greaterThan(0)));
        assertThat(stats.getValue("watch.input.none.total"), is(lessThan(activeHitCount)));

        // condition count
        assertThat(stats.getValue("watch.condition.never.active"), is(greaterThan(0)));
        assertThat(stats.getValue("watch.condition.always.active"), is(greaterThan(0)));

        // action count
        int actionCountA = stats.getValue("watch.action.a.active");
        int actionCountB = stats.getValue("watch.action.b.active");
        int actionCountC = stats.getValue("watch.action.c.active");
        assertThat(actionCountA + actionCountB + actionCountC, is(activeHitCount));

        // transform count
        assertThat(stats.getValue("watch.transform.TYPE.active"), is(greaterThan(0)));
    }

    public void testThatCleaningWatchesWorks() throws Exception {
        ClusterState.Builder csBuilder = new ClusterState.Builder(new ClusterName("_name"));
        createWatchIndexMetaData(csBuilder, VersionUtils.randomVersion(random()));

        RefreshResponse refreshResponse = mockRefreshResponse(1, 1);
        when(clientProxy.refresh(any(RefreshRequest.class))).thenReturn(refreshResponse);

        BytesReference source = new BytesArray("{}");
        SearchHit hit = new SearchHit(0, "_id1", new Text("type"), Collections.emptyMap());
        hit.sourceRef(source);

        SearchResponse searchResponse = mockSearchResponse(1, 1, 1, hit);
        when(clientProxy.search(any(SearchRequest.class), any(TimeValue.class))).thenReturn(searchResponse);

        SearchResponse finalSearchResponse = mockSearchResponse(1, 1, 0);
        when(clientProxy.searchScroll(anyString(), any(TimeValue.class))).thenReturn(finalSearchResponse);

        Watch watch = mock(Watch.class);
        WatchStatus status = mock(WatchStatus.class);
        when(watch.status()).thenReturn(status);
        when(parser.parse(eq("_id1"), eq(true), eq(source), eq(XContentType.JSON), anyBoolean())).thenReturn(watch);

        when(clientProxy.clearScroll(anyString())).thenReturn(new ClearScrollResponse(true, 0));

        ClusterState cs = csBuilder.build();
        assertThat(watchStore.validate(cs), is(true));
        watchStore.start(cs);
        assertThat(watchStore.started(), is(true));
        assertThat(watchStore.watches(), hasSize(1));

        watchStore.clearWatchesInMemory();
        assertThat(watchStore.started(), is(true));
        assertThat(watchStore.watches(), hasSize(0));
        assertThat(watchStore.activeWatches(), hasSize(0));
    }

    // the elasticsearch migration helper is doing reindex using aliases, so we have to
    // make sure that the watch store supports a single alias pointing to the watch index
    public void testThatStartingWithWatchesIndexAsAliasWorks() throws Exception {
        ClusterState.Builder csBuilder = new ClusterState.Builder(new ClusterName("_name"));

        MetaData.Builder metaDataBuilder = MetaData.builder();
        RoutingTable.Builder routingTableBuilder = RoutingTable.builder();
        Settings settings = settings(Version.CURRENT)
                .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1)
                .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 1)
                .build();
        metaDataBuilder.put(IndexMetaData.builder("watches-alias").settings(settings).numberOfShards(1).numberOfReplicas(1)
                .putAlias(new AliasMetaData.Builder(WatchStore.INDEX).build()));

        final Index index = metaDataBuilder.get("watches-alias").getIndex();
        IndexRoutingTable.Builder indexRoutingTableBuilder = IndexRoutingTable.builder(index);
        indexRoutingTableBuilder.addIndexShard(new IndexShardRoutingTable.Builder(new ShardId(index, 0))
                .addShard(TestShardRouting.newShardRouting("watches-alias", 0, "_node_id", null, true, ShardRoutingState.STARTED))
                .build());
        indexRoutingTableBuilder.addReplica();
        routingTableBuilder.add(indexRoutingTableBuilder.build());
        csBuilder.metaData(metaDataBuilder);
        csBuilder.routingTable(routingTableBuilder.build());

        RefreshResponse refreshResponse = mockRefreshResponse(1, 1);
        when(clientProxy.refresh(any(RefreshRequest.class))).thenReturn(refreshResponse);

        BytesReference source = new BytesArray("{}");
        SearchHit hit1 = new SearchHit(0, "_id1", new Text("type"), Collections.<String, SearchHitField>emptyMap());
        hit1.sourceRef(source);
        SearchHit hit2 = new SearchHit(1, "_id2", new Text("type"), Collections.<String, SearchHitField>emptyMap());
        hit2.sourceRef(source);
        SearchResponse searchResponse1 = mockSearchResponse(1, 1, 2, hit1, hit2);

        when(clientProxy.search(any(SearchRequest.class), any(TimeValue.class))).thenReturn(searchResponse1);

        SearchHit hit3 = new SearchHit(2, "_id3", new Text("type"), Collections.<String, SearchHitField>emptyMap());
        hit3.sourceRef(source);
        SearchHit hit4 = new SearchHit(3, "_id4", new Text("type"), Collections.<String, SearchHitField>emptyMap());
        hit4.sourceRef(source);
        SearchResponse searchResponse2 = mockSearchResponse(1, 1, 2, hit3, hit4);
        SearchResponse searchResponse3 = mockSearchResponse(1, 1, 2);
        when(clientProxy.searchScroll(anyString(), any(TimeValue.class))).thenReturn(searchResponse2, searchResponse3);

        Watch watch1 = mock(Watch.class);
        WatchStatus status = mock(WatchStatus.class);
        when(watch1.status()).thenReturn(status);
        Watch watch2 = mock(Watch.class);
        when(watch2.status()).thenReturn(status);
        Watch watch3 = mock(Watch.class);
        when(watch3.status()).thenReturn(status);
        Watch watch4 = mock(Watch.class);
        when(watch4.status()).thenReturn(status);
        when(parser.parse(eq("_id1"), eq(true), eq(source), eq(XContentType.JSON), anyBoolean())).thenReturn(watch1);
        when(parser.parse(eq("_id2"), eq(true), eq(source), eq(XContentType.JSON), anyBoolean())).thenReturn(watch2);
        when(parser.parse(eq("_id3"), eq(true), eq(source), eq(XContentType.JSON), anyBoolean())).thenReturn(watch3);
        when(parser.parse(eq("_id4"), eq(true), eq(source), eq(XContentType.JSON), anyBoolean())).thenReturn(watch4);

        when(clientProxy.clearScroll(anyString())).thenReturn(new ClearScrollResponse(true, 0));

        ClusterState cs = csBuilder.build();
        assertThat(watchStore.validate(cs), is(true));
        watchStore.start(cs);
        assertThat(watchStore.started(), is(true));
        assertThat(watchStore.watches().size(), equalTo(4));
        verify(clientProxy, times(1)).refresh(any(RefreshRequest.class));
        verify(clientProxy, times(1)).search(any(SearchRequest.class), any(TimeValue.class));
        verify(clientProxy, times(1)).clearScroll(anyString());
    }

    // the elasticsearch migration helper is doing reindex using aliases, so we have to
    // make sure that the watch store supports only a single index in an alias
    public void testThatWatchesIndexWithTwoAliasesFails() throws Exception {
        ClusterState.Builder csBuilder = new ClusterState.Builder(new ClusterName("_name"));

        MetaData.Builder metaDataBuilder = MetaData.builder();
        RoutingTable.Builder routingTableBuilder = RoutingTable.builder();
        Settings settings = settings(Version.CURRENT)
                .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1)
                .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 1)
                .build();
        metaDataBuilder.put(IndexMetaData.builder("watches-alias").settings(settings).numberOfShards(1).numberOfReplicas(1)
                .putAlias(new AliasMetaData.Builder(WatchStore.INDEX).build()));
        metaDataBuilder.put(IndexMetaData.builder("whatever").settings(settings).numberOfShards(1).numberOfReplicas(1)
                .putAlias(new AliasMetaData.Builder(WatchStore.INDEX).build()));

        final Index index = metaDataBuilder.get("watches-alias").getIndex();
        IndexRoutingTable.Builder indexRoutingTableBuilder = IndexRoutingTable.builder(index);
        indexRoutingTableBuilder.addIndexShard(new IndexShardRoutingTable.Builder(new ShardId(index, 0))
                .addShard(TestShardRouting.newShardRouting("watches-alias", 0, "_node_id", null, true, ShardRoutingState.STARTED))
                .build());
        indexRoutingTableBuilder.addReplica();
        final Index otherIndex = metaDataBuilder.get("whatever").getIndex();
        IndexRoutingTable.Builder otherIndexRoutingTableBuilder = IndexRoutingTable.builder(otherIndex);
        otherIndexRoutingTableBuilder.addIndexShard(new IndexShardRoutingTable.Builder(new ShardId(index, 0))
                .addShard(TestShardRouting.newShardRouting("whatever", 0, "_node_id", null, true, ShardRoutingState.STARTED))
                .build());
        otherIndexRoutingTableBuilder.addReplica();
        routingTableBuilder.add(otherIndexRoutingTableBuilder.build());
        csBuilder.metaData(metaDataBuilder);
        csBuilder.routingTable(routingTableBuilder.build());

        ClusterState cs = csBuilder.build();
        assertThat(watchStore.validate(cs), is(false));
        IllegalStateException exception = expectThrows(IllegalStateException.class, () -> watchStore.start(cs));
        assertThat(exception.getMessage(), is("Alias [.watches] points to more than one index"));
    }

    public void testValidateStartWithClosedIndex() throws Exception {
        ClusterState.Builder csBuilder = new ClusterState.Builder(new ClusterName("_name"));
        MetaData.Builder metaDataBuilder = MetaData.builder();
        Settings indexSettings = settings(Version.CURRENT)
                .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1)
                .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 1)
                .build();
        metaDataBuilder.put(IndexMetaData.builder(WatchStore.INDEX).state(IndexMetaData.State.CLOSE).settings(indexSettings));
        csBuilder.metaData(metaDataBuilder);

        assertThat(watchStore.validate(csBuilder.build()), Matchers.is(false));
    }

    public void testWatchStoreTriesToWriteNewWatchFirst() throws Exception {
        ClusterState.Builder csBuilder = new ClusterState.Builder(new ClusterName("_name"));
        ClusterState cs = csBuilder.build();
        assertThat(watchStore.validate(cs), is(true));
        watchStore.start(cs);
        assertThat(watchStore.started(), is(true));

        Watch watch = mock(Watch.class);
        WatchStatus watchStatus = mock(WatchStatus.class);
        when(watchStatus.dirty()).thenReturn(true);
        when(watch.status()).thenReturn(watchStatus);
        boolean refresh = randomBoolean();

        UpdateResponse response = mock(UpdateResponse.class);
        when(response.getVersion()).thenReturn(10L);
        when(clientProxy.update(anyObject()))
                .thenThrow(new DocumentMissingException(new ShardId(new Index(WatchStore.INDEX, "uuid"), 0), "doc", "bar"))
                .thenReturn(response);

        watchStore.updateStatus(watch, refresh);

        verify(watchStatus).version(eq(10L));
        verify(watchStatus).resetDirty();
        verify(watch).version(eq(10L));
    }

    public void testThatWatchStoreUsesDocTypeByDefault() throws Exception {
        ClusterState.Builder csBuilder = new ClusterState.Builder(new ClusterName("_name"));
        ClusterState cs = csBuilder.build();
        assertThat(watchStore.validate(cs), is(true));
        watchStore.start(cs);
        assertThat(watchStore.started(), is(true));

        Watch watch = mock(Watch.class);
        when(watch.id()).thenReturn(randomAlphaOfLength(10));
        WatchStatus watchStatus = mock(WatchStatus.class);
        when(watch.status()).thenReturn(watchStatus);
        when(watch.toXContent(any(), any())).thenReturn(jsonBuilder());

        doAnswer(invocation -> {
            IndexRequest request = (IndexRequest) invocation.getArguments()[0];
            ShardId shardId = new ShardId(new Index(request.index(), "uuid"), 0);
            IndexResponse response = new IndexResponse(shardId, request.type(), request.id(), 1, true);
            return response;
        }).when(clientProxy).index(any(), any(TimeValue.class));

        WatchStore.WatchPut watchPut = watchStore.put(watch);
        verify(clientProxy, times(1)).index(any(), any(TimeValue.class));
        assertThat(watchPut.indexResponse().getType(), is(WatchStore.DOC_TYPE));
        assertThat(watchPut.current(), is(watch));
        assertThat(watchPut.previous(), is(nullValue()));
    }

    public void testThatWatchStoreUsesLegacyDocType() throws Exception {
        ClusterState.Builder csBuilder = new ClusterState.Builder(new ClusterName("_name"));
        ClusterState cs = csBuilder.build();
        assertThat(watchStore.validate(cs), is(true));
        watchStore.start(cs);
        assertThat(watchStore.started(), is(true));

        Watch watch = mock(Watch.class);
        when(watch.id()).thenReturn(randomAlphaOfLength(10));
        WatchStatus watchStatus = mock(WatchStatus.class);
        when(watch.status()).thenReturn(watchStatus);
        when(watch.toXContent(any(), any())).thenReturn(jsonBuilder());

        doAnswer(invocation -> {
            IndexRequest request = (IndexRequest) invocation.getArguments()[0];
            Index index = new Index(request.index(), "uuid");
            if (WatchStore.LEGACY_DOC_TYPE.equals(request.type()) == false) {
                throw new TypeMissingException(index, request.type());
            }
            return new IndexResponse(new ShardId(index, 0), request.type(), request.id(), 1, true);
        }).when(clientProxy).index(any(), any(TimeValue.class));

        WatchStore.WatchPut watchPut = watchStore.put(watch);
        verify(clientProxy, times(2)).index(any(), any(TimeValue.class));
        assertThat(watchPut.indexResponse().getType(), is(WatchStore.LEGACY_DOC_TYPE));
        assertThat(watchPut.current(), is(watch));
        assertThat(watchPut.previous(), is(nullValue()));
    }

    /**
     * Creates the standard cluster state metadata for the watches index
     * with shards/replicas being marked as started
     */
    private void createWatchIndexMetaData(ClusterState.Builder builder, Version version) {
        MetaData.Builder metaDataBuilder = MetaData.builder();
        RoutingTable.Builder routingTableBuilder = RoutingTable.builder();
        Settings settings = settings(version)
                .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1)
                .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 1)
                .build();
        metaDataBuilder.put(IndexMetaData.builder(WatchStore.INDEX).settings(settings).numberOfShards(1).numberOfReplicas(1));
        final Index index = metaDataBuilder.get(WatchStore.INDEX).getIndex();
        IndexRoutingTable.Builder indexRoutingTableBuilder = IndexRoutingTable.builder(index);
        indexRoutingTableBuilder.addIndexShard(new IndexShardRoutingTable.Builder(new ShardId(index, 0))
                .addShard(TestShardRouting.newShardRouting(WatchStore.INDEX, 0, "_node_id", null, true, ShardRoutingState.STARTED))
                .build());
        indexRoutingTableBuilder.addReplica();
        routingTableBuilder.add(indexRoutingTableBuilder.build());
        builder.metaData(metaDataBuilder);
        builder.routingTable(routingTableBuilder.build());
    }

    private RefreshResponse mockRefreshResponse(int total, int successful) {
        RefreshResponse refreshResponse = mock(RefreshResponse.class);
        when(refreshResponse.getTotalShards()).thenReturn(total);
        when(refreshResponse.getSuccessfulShards()).thenReturn(successful);
        return refreshResponse;
    }

    private SearchResponse mockSearchResponse(int total, int successful, int totalHits, SearchHit... hits) {
        SearchHits searchHits = new SearchHits(hits, totalHits, 1f);
        SearchResponse searchResponse = mock(SearchResponse.class);
        when(searchResponse.getTotalShards()).thenReturn(total);
        when(searchResponse.getSuccessfulShards()).thenReturn(successful);
        when(searchResponse.getHits()).thenReturn(searchHits);
        return searchResponse;
    }

}
