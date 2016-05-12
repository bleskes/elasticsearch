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
import static org.junit.Assert.assertNull;
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
import com.prelert.job.results.Influencer;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.exception.InvalidParametersException;

public class InfluencersTest extends ServiceTest
{
    private static final String JOB_ID = "foo";

    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    private Influencers m_Influencers;

    @Before
    public void setUp() throws UnknownJobException
    {
        m_Influencers = new Influencers();
        configureService(m_Influencers);
    }

    @Test
    public void testInfluencers_GivenNegativeSkip() throws UnknownJobException
    {
        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage("Parameter 'skip' cannot be < 0");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_SKIP_PARAM));

        m_Influencers.influencers(JOB_ID, -1, 100, "", "", null, false, 0.0, false);
    }

    @Test
    public void testInfluencers_GivenNegativeTake() throws UnknownJobException
    {
        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage("Parameter 'take' cannot be < 0");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_TAKE_PARAM));

        m_Influencers.influencers(JOB_ID, 0, -1, "", "", null, false, 0.0, false);
    }

    @Test
    public void testInfluencers_GivenAllResultsInOnePage() throws UnknownJobException
    {
        Influencer inf1 = new Influencer();
        Influencer inf2 = new Influencer();
        Influencer inf3 = new Influencer();
        QueryPage<Influencer> page = new QueryPage<>(Arrays.asList(inf1, inf2, inf3), 3);

        when(jobReader().influencers(JOB_ID, 0, 100, 0, 0, null, false, 0.0, true)).thenReturn(page);

        Pagination<Influencer> results = m_Influencers.influencers(JOB_ID, 0, 100, "", "", null,
                false, 0.0, true);

        assertEquals(3, results.getHitCount());
        assertEquals(inf1, results.getDocuments().get(0));
        assertEquals(inf2, results.getDocuments().get(1));
        assertEquals(inf3, results.getDocuments().get(2));

        assertNull(results.getNextPage());
        assertNull(results.getPreviousPage());
    }
}
