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
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Optional;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.UnknownJobException;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.persistence.QueryPage;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.results.Bucket;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;
import com.prelert.rs.exception.InvalidParametersException;
import com.prelert.rs.provider.RestApiException;

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
    public void testBuckets_GivenNegativeSkip() throws UnknownJobException, NativeProcessRunException
    {
        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage("Parameter 'skip' cannot be < 0");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_SKIP_PARAM));

        m_Buckets.buckets("foo", false, false, -1, 100, "", "", 0, 0);
    }

    @Test
    public void testBuckets_GivenNegativeTake() throws UnknownJobException, NativeProcessRunException
    {
        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage("Parameter 'take' cannot be < 0");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_TAKE_PARAM));

        m_Buckets.buckets("foo", false, false, 0, -1, "", "", 0, 0);
    }

    @Test
    public void testBuckets_GivenOnePage() throws UnknownJobException, NativeProcessRunException, URISyntaxException
    {
        QueryPage<Bucket> queryResult = new QueryPage<>(Arrays.asList(new Bucket()), 1);

        when(jobReader().buckets("foo", false, false, 0, 100, 0.0, 0.0)).thenReturn(queryResult);

        Pagination<Bucket> results = m_Buckets.buckets("foo", false, false, 0, 100, "", "", 0.0, 0.0);
        assertEquals(1, results.getHitCount());
        assertEquals(100, results.getTake());
    }

    @Test
    public void testBuckets_GivenNoStartOrEndParams() throws UnknownJobException, NativeProcessRunException, URISyntaxException
    {
        QueryPage<Bucket> queryResult = new QueryPage<>(Arrays.asList(new Bucket()), 300);

        when(jobReader().buckets("foo", false, false, 0, 100, 0.0, 0.0)).thenReturn(queryResult);

        Pagination<Bucket> results = m_Buckets.buckets("foo", false, false, 0, 100, "", "", 0.0, 0.0);
        assertEquals(300, results.getHitCount());
        assertEquals(100, results.getTake());

        assertNull(results.getPreviousPage());
        String nextPageUri = results.getNextPage().toString();
        assertEquals("http://localhost/test/results/foo/buckets?skip=100&take=100&expand=false&"
                + "includeInterim=false&anomalyScore=0.0&maxNormalizedProbability=0.0",
                nextPageUri);
    }

    @Test
    public void testBuckets_GivenEpochStartAndEpochEndParams() throws UnknownJobException,
            NativeProcessRunException
    {
        QueryPage<Bucket> queryResult = new QueryPage<>(Arrays.asList(new Bucket()), 300);

        when(jobReader().buckets("foo", false, false, 0, 100, 1000, 2000, 0.0, 0.0)).thenReturn(queryResult);

        Pagination<Bucket> buckets = m_Buckets.buckets("foo", false, false, 0, 100, "1", "2", 0.0, 0.0);

        assertEquals(300l, buckets.getHitCount());
        assertEquals(100l, buckets.getTake());
        assertEquals(0l, buckets.getSkip());

        assertNull(buckets.getPreviousPage());
        String nextPageUri = buckets.getNextPage().toString();
        assertEquals("http://localhost/test/results/foo/buckets?skip=100&take=100&start=1&end=2&"
                + "expand=false&includeInterim=false&anomalyScore=0.0&maxNormalizedProbability=0.0",
                nextPageUri);
    }

    @Test
    public void testBuckets_GivenIsoWithoutMillisStartAndEpochEndParams() throws UnknownJobException,
            NativeProcessRunException
    {
        QueryPage<Bucket> queryResult = new QueryPage<>(Arrays.asList(new Bucket()), 300);

        when(jobReader().buckets("foo", false, false, 0, 100, 1420113600000L, 1420117200000L, 0.0, 0.0))
                .thenReturn(queryResult);

        Pagination<Bucket> buckets = m_Buckets.buckets("foo", false, false, 0, 100,
                "2015-01-01T12:00:00Z", "2015-01-01T13:00:00Z", 0.0, 0.0);

        assertEquals(300l, buckets.getHitCount());
        assertEquals(100l, buckets.getTake());
        assertEquals(0l, buckets.getSkip());

        assertNull(buckets.getPreviousPage());
        String nextPageUri = buckets.getNextPage().toString();
        assertEquals(
                "http://localhost/test/results/foo/buckets?skip=100&take=100&start=2015-01-01T12%3A00%3A00Z&end=2015-01-01T13%3A00%3A00Z&expand=false&includeInterim=false&anomalyScore=0.0&maxNormalizedProbability=0.0",
                nextPageUri);
    }

    @Test
    public void testBuckets_GivenIsoWithMillisStartAndEpochEndParams() throws UnknownJobException,
            NativeProcessRunException
    {
        QueryPage<Bucket> queryResult = new QueryPage<>(Arrays.asList(new Bucket()), 300);

        when(jobReader().buckets("foo", false, false, 0, 100, 1420113600042L, 1420117200142L, 0.0, 0.0))
                .thenReturn(queryResult);

        Pagination<Bucket> buckets = m_Buckets.buckets("foo", false, false, 0, 100,
                "2015-01-01T12:00:00.042Z", "2015-01-01T13:00:00.142+00:00", 0.0, 0.0);

        assertEquals(300l, buckets.getHitCount());
        assertEquals(100l, buckets.getTake());
        assertEquals(0l, buckets.getSkip());

        assertNull(buckets.getPreviousPage());
        String nextPageUri = buckets.getNextPage().toString();
        assertEquals(
                "http://localhost/test/results/foo/buckets?skip=100&take=100&start=2015-01-01T12%3A00%3A00.042Z&end=2015-01-01T13%3A00%3A00.142%2B00%3A00&expand=false&includeInterim=false&anomalyScore=0.0&maxNormalizedProbability=0.0",
                nextPageUri);
    }

    @Test
    public void testBuckets_GivenIsoWithoutColonInOffsetStartAndEpochEndParams() throws UnknownJobException,
            NativeProcessRunException
    {
        QueryPage<Bucket> queryResult = new QueryPage<>(Arrays.asList(new Bucket()), 300);

        when(jobReader().buckets("foo", false, false, 0, 100, 1420113600042L, 1420117200142L, 0.0, 0.0))
                .thenReturn(queryResult);

        Pagination<Bucket> buckets = m_Buckets.buckets("foo", false, false, 0, 100,
                "2015-01-01T12:00:00.042+0000", "2015-01-01T15:00:00.142+0200", 0.0, 0.0);

        assertEquals(300l, buckets.getHitCount());
        assertEquals(100l, buckets.getTake());
        assertEquals(0l, buckets.getSkip());

        assertNull(buckets.getPreviousPage());
        String nextPageUri = buckets.getNextPage().toString();
        assertEquals(
                "http://localhost/test/results/foo/buckets?skip=100&take=100&start=2015-01-01T12%3A00%3A00.042%2B0000&end=2015-01-01T15%3A00%3A00.142%2B0200&expand=false&includeInterim=false&anomalyScore=0.0&maxNormalizedProbability=0.0",
                nextPageUri);
    }

    @Test
    public void testBuckets_GivenInvalidStartAndEpochEndParams() throws UnknownJobException,
            NativeProcessRunException
    {
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage("Query param 'start' with value 'invalid' cannot be parsed as a date or converted to a number (epoch)");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.UNPARSEABLE_DATE_ARGUMENT));

        m_Buckets.buckets("foo", false, false, 0, 100, "invalid", "also invalid", 0.0, 0.0);
    }

    @Test
    public void testBucket_GivenExistingBucket() throws NativeProcessRunException, UnknownJobException
    {
        Optional<Bucket> queryResult = Optional.of(new Bucket());

        when(jobReader().bucket("bar", 42000L, false, true)).thenReturn(queryResult);

        Response response = m_Buckets.bucket("bar", "42", false, true);

        @SuppressWarnings("unchecked")
        SingleDocument<Bucket> result = (SingleDocument<Bucket>)response.getEntity();
        assertTrue(result.isExists());
        assertEquals(Bucket.TYPE, result.getType());

        assertEquals(200, response.getStatus());
    }

    @Test
    public void testBucket_GivenNonExistingBucket() throws NativeProcessRunException, UnknownJobException
    {
        Optional<Bucket> queryResult = Optional.empty();

        when(jobReader().bucket("bar", 42000L, false, true)).thenReturn(queryResult);

        Response response = m_Buckets.bucket("bar", "42", false, true);
        @SuppressWarnings("unchecked")
        SingleDocument<Bucket> result = (SingleDocument<Bucket>)response.getEntity();

        assertEquals(false, result.isExists());
        assertEquals(404, response.getStatus());
    }
}
