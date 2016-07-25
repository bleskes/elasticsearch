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

package com.prelert.rs.resources;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.UnknownJobException;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.persistence.QueryPage;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.exception.InvalidParametersException;

public class RecordsTest extends ServiceTest
{
    private static final String JOB_ID = "foo";

    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    private final Records m_Records = new Records();

    @Before
    public void setUp()
    {
        configureService(m_Records);
    }

    @Test
    public void testRecords_GivenNegativeSkip() throws NativeProcessRunException, UnknownJobException
    {
        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage("Parameter 'skip' cannot be < 0");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_SKIP_PARAM));

        m_Records.records(JOB_ID, -1, 100, "", "", false, "", false, 0, 0);
    }

    @Test
    public void testRecords_GivenNegativeTake() throws NativeProcessRunException, UnknownJobException
    {
        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage("Parameter 'take' cannot be < 0");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_TAKE_PARAM));

        m_Records.records(JOB_ID, 0, -1, "", "", false, "", false, 0, 0);
    }

    @Test
    public void testRecords_GivenOnePage() throws UnknownJobException, NativeProcessRunException
    {
        QueryPage<AnomalyRecord> queryResult = new QueryPage<>(Arrays.asList(new AnomalyRecord()), 1);

        when(jobReader().records(JOB_ID, 0, 100, false, "normalizedProbability", true, 0, 0))
                .thenReturn(queryResult);

        Pagination<AnomalyRecord> results = m_Records.records(JOB_ID, 0, 100, "", "", false,
                "normalizedProbability", true, 0, 0);
        assertEquals(1, results.getHitCount());
        assertEquals(100, results.getTake());
    }

    @Test
    public void testRecords_GivenSortOnTimestamp() throws UnknownJobException,
            NativeProcessRunException
    {
        QueryPage<AnomalyRecord> queryResult = new QueryPage<>(Arrays.asList(new AnomalyRecord()), 1);

        when(jobReader().records(JOB_ID, 0, 100, false, "timestamp", true, 0, 0))
                .thenReturn(queryResult);

        Pagination<AnomalyRecord> results = m_Records.records(JOB_ID, 0, 100, "", "", false,
                "timestamp", true, 0, 0);
        assertEquals(1, results.getHitCount());
        assertEquals(100, results.getTake());
    }

    @Test
    public void testRecords_GivenTimeRange() throws UnknownJobException,
            NativeProcessRunException
    {
        QueryPage<AnomalyRecord> queryResult = new QueryPage<>(Arrays.asList(new AnomalyRecord()), 1);

        when(jobReader().records(JOB_ID, 0, 100, 3600000L, 7200000L, false, "normalizedProbability", true, 0, 0))
                .thenReturn(queryResult);

        Pagination<AnomalyRecord> results = m_Records.records(JOB_ID, 0, 100, "3600", "7200", false,
                "normalizedProbability", true, 0, 0);
        assertEquals(1, results.getHitCount());
        assertEquals(100, results.getTake());
    }

    @Test
    public void testRecords_GivenManyPages() throws UnknownJobException,
            NativeProcessRunException
    {
        QueryPage<AnomalyRecord> queryResult = new QueryPage<>(Arrays.asList(new AnomalyRecord()), 3);

        when(jobReader().records(JOB_ID, 0, 10, false, "normalizedProbability", true, 1000, 2000))
                .thenReturn(queryResult);

        Pagination<AnomalyRecord> results = m_Records.records(JOB_ID, 0, 10, "", "", false,
                "normalizedProbability", true, 1000, 2000);
        assertEquals(3, results.getHitCount());
        assertEquals(10, results.getTake());
    }

}
