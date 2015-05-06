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

package org.elasticsearch.watcher.watch;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.elasticsearch.watcher.WatcherException;
import org.elasticsearch.watcher.WatcherService;
import org.elasticsearch.watcher.WatcherState;
import org.elasticsearch.watcher.execution.ExecutionService;
import org.elasticsearch.watcher.trigger.Trigger;
import org.elasticsearch.watcher.trigger.TriggerEngine;
import org.elasticsearch.watcher.trigger.TriggerService;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 */
public class WatchServiceTests extends ElasticsearchTestCase {

    private TriggerService triggerService;
    private WatchStore watchStore;
    private Watch.Parser watchParser;
    private WatcherService watcherService;
    private ExecutionService executionService;
    private WatchLockService watchLockService;

    @Before
    public void init() throws Exception {
        triggerService = mock(TriggerService.class);
        watchStore = mock(WatchStore.class);
        watchParser = mock(Watch.Parser.class);
        executionService =  mock(ExecutionService.class);
        watchLockService = mock(WatchLockService.class);
        watcherService = new WatcherService(ImmutableSettings.EMPTY, triggerService, watchStore, watchParser, executionService, watchLockService);
        Field field = WatcherService.class.getDeclaredField("state");
        field.setAccessible(true);
        AtomicReference<WatcherState> state = (AtomicReference<WatcherState>) field.get(watcherService);
        state.set(WatcherState.STARTED);
    }

    @Test
    public void testPutWatch() throws Exception {
        IndexResponse indexResponse = mock(IndexResponse.class);
        Watch watch = mock(Watch.class);
        WatchStore.WatchPut watchPut = mock(WatchStore.WatchPut.class);
        when(watchPut.indexResponse()).thenReturn(indexResponse);
        when(watchPut.current()).thenReturn(watch);

        TimeValue timeout = TimeValue.timeValueSeconds(5);
        WatchLockService.Lock lock = mock(WatchLockService.Lock.class);
        when(watchLockService.tryAcquire(any(String.class), eq(timeout))).thenReturn(lock);
        when(watchParser.parseWithSecrets(any(String.class), eq(false), any(BytesReference.class))).thenReturn(watch);
        when(watchStore.put(watch)).thenReturn(watchPut);
        IndexResponse response = watcherService.putWatch("_id", new BytesArray("{}"), timeout);
        assertThat(response, sameInstance(indexResponse));

        verify(triggerService, times(1)).add(any(TriggerEngine.Job.class));
    }

    @Test(expected = WatcherService.TimeoutException.class)
    public void testPutWatch_Timeout() throws Exception {
        TimeValue timeout = TimeValue.timeValueSeconds(5);
        when(watchLockService.tryAcquire("_id", timeout)).thenReturn(null);
        watcherService.putWatch("_id", new BytesArray("{}"), timeout);
    }

    @Test
    public void testPutWatch_NotSchedule() throws Exception {
        Trigger trigger = mock(Trigger.class);

        IndexResponse indexResponse = mock(IndexResponse.class);
        Watch watch = mock(Watch.class);
        when(watch.trigger()).thenReturn(trigger);
        WatchStore.WatchPut watchPut = mock(WatchStore.WatchPut.class);
        when(watchPut.indexResponse()).thenReturn(indexResponse);
        when(watchPut.current()).thenReturn(watch);
        Watch previousWatch = mock(Watch.class);
        when(previousWatch.trigger()).thenReturn(trigger);
        when(watchPut.previous()).thenReturn(previousWatch);

        TimeValue timeout = TimeValue.timeValueSeconds(5);
        WatchLockService.Lock lock = mock(WatchLockService.Lock.class);
        when(watchLockService.tryAcquire(any(String.class), eq(timeout))).thenReturn(lock);
        when(watchParser.parseWithSecrets(any(String.class), eq(false), any(BytesReference.class))).thenReturn(watch);
        when(watchStore.put(watch)).thenReturn(watchPut);
        IndexResponse response = watcherService.putWatch("_id", new BytesArray("{}"), timeout);
        assertThat(response, sameInstance(indexResponse));

        verifyZeroInteractions(triggerService);
    }

    @Test
    public void testDeleteWatch() throws Exception {
        TimeValue timeout = TimeValue.timeValueSeconds(5);
        WatchLockService.Lock lock = mock(WatchLockService.Lock.class);
        when(watchLockService.tryAcquire("_id", timeout)).thenReturn(lock);

        WatchStore.WatchDelete expectedWatchDelete = mock(WatchStore.WatchDelete.class);
        DeleteResponse deleteResponse = mock(DeleteResponse.class);
        when(deleteResponse.isFound()).thenReturn(true);
        when(expectedWatchDelete.deleteResponse()).thenReturn(deleteResponse);
        when(watchStore.delete("_id")).thenReturn(expectedWatchDelete);
        WatchStore.WatchDelete watchDelete = watcherService.deleteWatch("_id", timeout);

        assertThat(watchDelete, sameInstance(expectedWatchDelete));
        verify(triggerService, times(1)).remove("_id");
    }

    @Test(expected = WatcherService.TimeoutException.class)
    public void testDeleteWatch_Timeout() throws Exception {
        TimeValue timeout = TimeValue.timeValueSeconds(5);
        when(watchLockService.tryAcquire("_id", timeout)).thenReturn(null);
        watcherService.deleteWatch("_id", timeout);
    }

    @Test
    public void testDeleteWatch_NotFound() throws Exception {
        TimeValue timeout = TimeValue.timeValueSeconds(5);
        WatchLockService.Lock lock = mock(WatchLockService.Lock.class);
        when(watchLockService.tryAcquire("_id", timeout)).thenReturn(lock);

        WatchStore.WatchDelete expectedWatchDelete = mock(WatchStore.WatchDelete.class);
        DeleteResponse deleteResponse = mock(DeleteResponse.class);
        when(deleteResponse.isFound()).thenReturn(false);
        when(expectedWatchDelete.deleteResponse()).thenReturn(deleteResponse);
        when(watchStore.delete("_id")).thenReturn(expectedWatchDelete);
        WatchStore.WatchDelete watchDelete = watcherService.deleteWatch("_id", timeout);

        assertThat(watchDelete, sameInstance(expectedWatchDelete));
        verifyZeroInteractions(triggerService);
    }

    @Test
    public void testAckWatch() throws Exception {
        TimeValue timeout = TimeValue.timeValueSeconds(5);
        WatchLockService.Lock lock = mock(WatchLockService.Lock.class);
        when(watchLockService.tryAcquire("_id", timeout)).thenReturn(lock);
        Watch watch = mock(Watch.class);
        when(watch.ack()).thenReturn(true);
        Watch.Status status = new Watch.Status();
        when(watch.status()).thenReturn(status);
        when(watchStore.get("_id")).thenReturn(watch);

        Watch.Status result = watcherService.ackWatch("_id", timeout);
        assertThat(result, not(sameInstance(status)));

        verify(watchStore, times(1)).updateStatus(watch);
    }

    @Test(expected = WatcherService.TimeoutException.class)
    public void testAckWatch_Timeout() throws Exception {
        TimeValue timeout = TimeValue.timeValueSeconds(5);
        when(watchLockService.tryAcquire("_id", timeout)).thenReturn(null);
        watcherService.ackWatch("_id", timeout);
    }

    @Test
    public void testAckWatch_NotAck() throws Exception {
        TimeValue timeout = TimeValue.timeValueSeconds(5);
        WatchLockService.Lock lock = mock(WatchLockService.Lock.class);
        when(watchLockService.tryAcquire("_id", timeout)).thenReturn(lock);
        Watch watch = mock(Watch.class);
        when(watch.ack()).thenReturn(false);
        Watch.Status status = new Watch.Status();
        when(watch.status()).thenReturn(status);
        when(watchStore.get("_id")).thenReturn(watch);

        Watch.Status result = watcherService.ackWatch("_id", timeout);
        assertThat(result, not(sameInstance(status)));

        verify(watchStore, never()).updateStatus(watch);
    }

    @Test
    public void testAckWatch_NoWatch() throws Exception {
        TimeValue timeout = TimeValue.timeValueSeconds(5);
        WatchLockService.Lock lock = mock(WatchLockService.Lock.class);
        when(watchLockService.tryAcquire("_id", timeout)).thenReturn(lock);
        when(watchStore.get("_id")).thenReturn(null);

        try {
            watcherService.ackWatch("_id", timeout);
            fail();
        } catch (WatcherException e) {
            // expected
        }

        verify(watchStore, never()).updateStatus(any(Watch.class));
    }

}
