/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.job.process.writer;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.process.params.DataLoadParams;
import com.prelert.job.process.params.InterimResultsParams;
import com.prelert.job.process.params.TimeRange;

public class ControlMsgToProcessWriterTest
{
    @Mock private LengthEncodedWriter m_LengthEncodedWriter;
    @Mock private AnalysisConfig m_AnalysisConfig;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        List<String> fields = Arrays.asList("foo", "bar");
        when(m_AnalysisConfig.analysisFields()).thenReturn(fields);
    }

    @Test
    public void testWriteCalcInterimMessage_GivenNoCalcInterimResultsAdvanceTime() throws IOException
    {
        ControlMsgToProcessWriter writer = new ControlMsgToProcessWriter(m_LengthEncodedWriter,
                m_AnalysisConfig);

        writer.writeCalcInterimMessage(new InterimResultsParams(false, new TimeRange(null, 1234567890L)));

        InOrder inOrder = inOrder(m_LengthEncodedWriter);
        inOrder.verify(m_LengthEncodedWriter).writeNumFields(4);
        inOrder.verify(m_LengthEncodedWriter, times(3)).writeField("");
        inOrder.verify(m_LengthEncodedWriter).writeField("t1234567890");
        verifyNoMoreInteractions(m_LengthEncodedWriter);
    }

    @Test
    public void testWriteCalcInterimMessage_GivenCalcInterimResultsWithNoTimeParams() throws IOException
    {
        ControlMsgToProcessWriter writer = new ControlMsgToProcessWriter(m_LengthEncodedWriter,
                m_AnalysisConfig);

        writer.writeCalcInterimMessage(new InterimResultsParams(true, new TimeRange(null, null)));

        InOrder inOrder = inOrder(m_LengthEncodedWriter);
        inOrder.verify(m_LengthEncodedWriter).writeNumFields(4);
        inOrder.verify(m_LengthEncodedWriter, times(3)).writeField("");
        inOrder.verify(m_LengthEncodedWriter).writeField("i");
        verifyNoMoreInteractions(m_LengthEncodedWriter);
    }

    @Test
    public void testWriteCalcInterimMessage_GivenNoCalcInterimResults() throws IOException
    {
        ControlMsgToProcessWriter writer = new ControlMsgToProcessWriter(m_LengthEncodedWriter,
                m_AnalysisConfig);

        writer.writeCalcInterimMessage(new InterimResultsParams(false, new TimeRange(null, null)));

        verifyNoMoreInteractions(m_LengthEncodedWriter);
    }

    @Test
    public void testWriteCalcInterimMessage_GivenCalcInterimResultsWithTimeParams() throws IOException
    {
        ControlMsgToProcessWriter writer = new ControlMsgToProcessWriter(m_LengthEncodedWriter,
                m_AnalysisConfig);

        writer.writeCalcInterimMessage(new InterimResultsParams(true, new TimeRange(120L, 180L)));

        InOrder inOrder = inOrder(m_LengthEncodedWriter);
        inOrder.verify(m_LengthEncodedWriter).writeNumFields(4);
        inOrder.verify(m_LengthEncodedWriter, times(3)).writeField("");
        inOrder.verify(m_LengthEncodedWriter).writeField("i120 180");
        verifyNoMoreInteractions(m_LengthEncodedWriter);
    }

    @Test
    public void testWriteFlushMessage() throws IOException
    {
        ControlMsgToProcessWriter writer = new ControlMsgToProcessWriter(m_LengthEncodedWriter,
                m_AnalysisConfig);
        long firstId = Long.parseLong(writer.writeFlushMessage());
        Mockito.reset(m_LengthEncodedWriter);

        writer.writeFlushMessage();

        InOrder inOrder = inOrder(m_LengthEncodedWriter);

        inOrder.verify(m_LengthEncodedWriter).writeNumFields(4);
        inOrder.verify(m_LengthEncodedWriter, times(3)).writeField("");
        inOrder.verify(m_LengthEncodedWriter).writeField("f" + (firstId + 1));

        inOrder.verify(m_LengthEncodedWriter).writeNumFields(4);
        inOrder.verify(m_LengthEncodedWriter, times(3)).writeField("");
        StringBuilder spaces = new StringBuilder();
        IntStream.rangeClosed(1, 8192).forEach(i -> spaces.append(' '));
        inOrder.verify(m_LengthEncodedWriter).writeField(spaces.toString());

        inOrder.verify(m_LengthEncodedWriter).flush();
        verifyNoMoreInteractions(m_LengthEncodedWriter);
    }

    @Test
    public void testWriteResetBucketsMessage() throws IOException
    {
        ControlMsgToProcessWriter writer = new ControlMsgToProcessWriter(m_LengthEncodedWriter,
                m_AnalysisConfig);

        writer.writeResetBucketsMessage(new DataLoadParams(false, new TimeRange(0L, 600L)));

        InOrder inOrder = inOrder(m_LengthEncodedWriter);
        inOrder.verify(m_LengthEncodedWriter).writeNumFields(4);
        inOrder.verify(m_LengthEncodedWriter, times(3)).writeField("");
        inOrder.verify(m_LengthEncodedWriter).writeField("r0 600");
        verifyNoMoreInteractions(m_LengthEncodedWriter);
    }
}
