/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;
import java.io.OutputStreamWriter;

import org.junit.Test;

import com.prelert.job.AnalysisLimits;

public class AnalysisLimitsWriterTest
{
    @Test
    public void testWrite_GivenUnsetValues() throws IOException
    {
        AnalysisLimits limits = new AnalysisLimits();
        OutputStreamWriter writer = mock(OutputStreamWriter.class);
        AnalysisLimitsWriter analysisLimitsWriter = new AnalysisLimitsWriter(limits, writer);

        analysisLimitsWriter.write();

        verify(writer).write("[memory]\n");
        verifyNoMoreInteractions(writer);
    }

    @Test
    public void testWrite_GivenModelMemoryLimitWasSet() throws IOException
    {
        AnalysisLimits limits = new AnalysisLimits();
        limits.setModelMemoryLimit(10);
        OutputStreamWriter writer = mock(OutputStreamWriter.class);
        AnalysisLimitsWriter analysisLimitsWriter = new AnalysisLimitsWriter(limits, writer);

        analysisLimitsWriter.write();

        verify(writer).write("[memory]\nmodelmemorylimit = 10\n");
        verifyNoMoreInteractions(writer);
    }
}
