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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;
import java.io.OutputStreamWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.prelert.job.ModelDebugConfig;

public class ModelDebugConfigWriterTest
{
    @Mock private OutputStreamWriter m_Writer;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown()
    {
        verifyNoMoreInteractions(m_Writer);
    }

    @Test
    public void testWrite_GivenEmptyConfig() throws IOException
    {
        ModelDebugConfig modelDebugConfig = new ModelDebugConfig();
        ModelDebugConfigWriter writer = new ModelDebugConfigWriter(modelDebugConfig, m_Writer);

        writer.write();

        verify(m_Writer).write("boundspercentile = null\nterms = \n");
    }

    @Test
    public void testWrite_GivenFullConfig() throws IOException
    {
        ModelDebugConfig modelDebugConfig = new ModelDebugConfig(65.0, "foo,bar");
        ModelDebugConfigWriter writer = new ModelDebugConfigWriter(modelDebugConfig, m_Writer);

        writer.write();

        verify(m_Writer).write("boundspercentile = 65.0\nterms = foo,bar\n");
    }

}
