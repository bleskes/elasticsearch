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

package com.prelert.job.status.elasticsearch;

import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.log4j.Logger;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.prelert.job.JobDetails;
import com.prelert.job.usage.UsageReporter;
import com.prelert.rs.data.DataCounts;

public class ElasticsearchStatusReporterTest
{
    @Mock private Client m_Client;
    @Mock private UsageReporter m_UsageReporter;
    @Mock private DataCounts m_Counts;
    @Mock private Logger m_Logger;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testReportStatus_ExecutesRequestWithRetryOnConfictSetToThree()
    {
        UpdateRequest updateRequest = mock(UpdateRequest.class);
        UpdateRequestBuilder updateRequestBuilder = mock(UpdateRequestBuilder.class);
        when(updateRequestBuilder.request()).thenReturn(updateRequest);
        when(updateRequestBuilder.setDoc(anyMap())).thenReturn(updateRequestBuilder);
        when(m_Client.prepareUpdate("foo", JobDetails.TYPE, "foo")).thenReturn(updateRequestBuilder);

        @SuppressWarnings("unchecked")
        ActionFuture<UpdateResponse> actionFuture = mock(ActionFuture.class);

        when(m_Client.update(updateRequest)).thenReturn(actionFuture);

        ElasticsearchStatusReporter reporter = createReporter("foo");
        reporter.reportStatus(0);

        verify(updateRequestBuilder).setRetryOnConflict(3);
    }

    private ElasticsearchStatusReporter createReporter(String jobId)
    {
        return new ElasticsearchStatusReporter(m_Client, m_UsageReporter, jobId, m_Counts, m_Logger);
    }
}
