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

package com.prelert.rs.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.util.Arrays;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.exceptions.UnknownJobException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.results.Bucket;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;

public class BucketsTest extends ServiceTest
{
    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    private Buckets m_Buckets;

    @Before
    public void setUp() throws UnknownJobException
    {
        m_Buckets = new Buckets();
        configureService(m_Buckets);
    }

    @Test
    public void testBuckets_GivenOnePage() throws UnknownJobException, NativeProcessRunException, URISyntaxException
    {
        Pagination<Bucket> results = new Pagination<>();
        results.setHitCount(1);
        results.setDocuments(Arrays.asList(new Bucket()));

        when(jobManager().buckets("foo", false, false, 0, 100, 0.0, 0.0)).thenReturn(results);

        assertEquals(results, m_Buckets.buckets("foo", false, false, 0, 100, "", "", 0.0, 0.0));
        assertNull(results.getPreviousPage());
        assertNull(results.getNextPage());
    }

    @Test
    public void testBuckets_GivenNoStartOrEndParams() throws UnknownJobException, NativeProcessRunException, URISyntaxException
    {
        Pagination<Bucket> results = new Pagination<>();
        results.setHitCount(300);
        results.setTake(100);

        when(jobManager().buckets("foo", false, false, 0, 100, 0.0, 0.0)).thenReturn(results);

        assertEquals(results, m_Buckets.buckets("foo", false, false, 0, 100, "", "", 0.0, 0.0));
        assertNull(results.getPreviousPage());
        String nextPageUri = results.getNextPage().toString();
        assertEquals("http://localhost/test/results/foo/buckets?skip=100&take=100&expand=false&"
                + "includeInterim=false&anomalyScore=0.0&maxNormalizedProbability=0.0",
                nextPageUri);
    }

    @Test
    public void testBuckets_GivenStartAndEndParams() throws UnknownJobException, NativeProcessRunException
    {
        Pagination<Bucket> results = new Pagination<>();
        results.setHitCount(300);
        results.setTake(100);

        when(jobManager().buckets("foo", false, false, 0, 100, 1000, 2000, 0.0, 0.0)).thenReturn(results);

        assertEquals(results, m_Buckets.buckets("foo", false, false, 0, 100, "1", "2", 0.0, 0.0));
        assertNull(results.getPreviousPage());
        String nextPageUri = results.getNextPage().toString();
        assertEquals("http://localhost/test/results/foo/buckets?skip=100&take=100&start=1&end=2&"
                + "expand=false&includeInterim=false&anomalyScore=0.0&maxNormalizedProbability=0.0",
                nextPageUri);
    }

    @Test
    public void testBucket_GivenExistingBucket() throws NativeProcessRunException, UnknownJobException
    {
        SingleDocument<Bucket> result = new SingleDocument<>();
        Bucket bucket = new Bucket();
        bucket.setId("42");
        result.setDocument(bucket);
        result.setExists(true);

        when(jobManager().bucket("bar", "42", false, true)).thenReturn(result);

        Response response = m_Buckets.bucket("bar", "42", false, true);

        assertEquals(result, response.getEntity());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testBucket_GivenNonExistingBucket() throws NativeProcessRunException, UnknownJobException
    {
        SingleDocument<Bucket> result = new SingleDocument<>();
        result.setDocumentId("42");
        result.setExists(false);

        when(jobManager().bucket("bar", "42", false, true)).thenReturn(result);

        Response response = m_Buckets.bucket("bar", "42", false, true);

        assertEquals(result, response.getEntity());
        assertEquals(404, response.getStatus());
    }
}
