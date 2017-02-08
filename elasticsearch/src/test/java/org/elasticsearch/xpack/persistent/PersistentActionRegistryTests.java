/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
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

package org.elasticsearch.xpack.persistent;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.ThreadPool;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PersistentActionRegistryTests extends ESTestCase {

    public void testActionLookup() {
        PersistentActionRegistry registry = new PersistentActionRegistry(Settings.EMPTY);
        TransportPersistentAction<?> action1 = mock(TransportPersistentAction.class);
        when(action1.getExecutor()).thenReturn(ThreadPool.Names.MANAGEMENT);
        TransportPersistentAction<?> action2 = mock(TransportPersistentAction.class);
        when(action2.getExecutor()).thenReturn(ThreadPool.Names.GENERIC);
        registry.registerPersistentAction("test1", action1);
        registry.registerPersistentAction("test2", action2);

        assertEquals(registry.getPersistentActionHolderSafe("test1").getAction(), "test1");
        assertEquals(registry.getPersistentActionHolderSafe("test1").getExecutor(), ThreadPool.Names.MANAGEMENT);
        assertEquals(registry.getPersistentActionHolderSafe("test1").getPersistentAction(), action1);
        assertEquals(registry.getPersistentActionSafe("test1"), action1);

        assertEquals(registry.getPersistentActionHolderSafe("test2").getAction(), "test2");
        assertEquals(registry.getPersistentActionHolderSafe("test2").getExecutor(), ThreadPool.Names.GENERIC);
        assertEquals(registry.getPersistentActionHolderSafe("test2").getPersistentAction(), action2);
        assertEquals(registry.getPersistentActionSafe("test2"), action2);

        try {
            registry.getPersistentActionHolderSafe("test3");
            fail("Should have failed");
        } catch (IllegalStateException ex) {
            assertEquals(ex.getMessage(), "Unknown persistent action [test3]");
        }

        try {
            registry.getPersistentActionSafe("test3");
            fail("Should have failed");
        } catch (IllegalStateException ex) {
            assertEquals(ex.getMessage(), "Unknown persistent action [test3]");
        }
    }
}
