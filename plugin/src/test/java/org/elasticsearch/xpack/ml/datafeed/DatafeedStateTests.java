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
package org.elasticsearch.xpack.ml.datafeed;

import org.elasticsearch.Version;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.job.config.JobState;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatafeedStateTests extends ESTestCase {

    public void testFromString() {
        assertEquals(DatafeedState.fromString("started"), DatafeedState.STARTED);
        assertEquals(DatafeedState.fromString("stopped"), DatafeedState.STOPPED);
    }

    public void testToString() {
        assertEquals("started", DatafeedState.STARTED.toString());
        assertEquals("stopped", DatafeedState.STOPPED.toString());
    }

    public void testValidOrdinals() {
        assertEquals(0, DatafeedState.STARTED.ordinal());
        assertEquals(1, DatafeedState.STOPPED.ordinal());
        assertEquals(2, DatafeedState.STARTING.ordinal());
        assertEquals(3, DatafeedState.STOPPING.ordinal());
    }

    @SuppressWarnings("unchecked")
    public void testStreaming_v54BackwardsCompatibility() throws IOException {
        StreamOutput out = mock(StreamOutput.class);
        when(out.getVersion()).thenReturn(Version.V_5_4_0);
        ArgumentCaptor<Enum> enumCaptor = ArgumentCaptor.forClass(Enum.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) {
                return null;
            }
        }).when(out).writeEnum(enumCaptor.capture());

        // STARTING & STOPPING states were introduced in v5.5.
        // Pre v5.5 STARTING translated as STOPPED
        DatafeedState.STARTING.writeTo(out);
        assertEquals(DatafeedState.STOPPED, enumCaptor.getValue());

        // Pre v5.5 STOPPING means the datafeed is STARTED
        DatafeedState.STOPPING.writeTo(out);
        assertEquals(DatafeedState.STARTED, enumCaptor.getValue());

        // POST 5.5 enums a written as is
        when(out.getVersion()).thenReturn(Version.V_5_5_0_UNRELEASED);

        DatafeedState.STARTING.writeTo(out);
        assertEquals(DatafeedState.STARTING, enumCaptor.getValue());
        DatafeedState.STOPPING.writeTo(out);
        assertEquals(DatafeedState.STOPPING, enumCaptor.getValue());
    }
}
