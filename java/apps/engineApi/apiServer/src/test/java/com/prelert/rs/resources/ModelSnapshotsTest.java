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

import java.net.URISyntaxException;
import java.util.Arrays;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.ModelSnapshot;
import com.prelert.job.NoSuchModelSnapshotException;
import com.prelert.job.UnknownJobException;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.manager.CannotDeleteSnapshotException;
import com.prelert.job.manager.DescriptionAlreadyUsedException;
import com.prelert.job.persistence.QueryPage;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.quantiles.Quantiles;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;
import com.prelert.rs.exception.InvalidParametersException;
import com.prelert.rs.provider.RestApiException;

public class ModelSnapshotsTest extends ServiceTest
{
    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    private ModelSnapshots m_ModelSnapshots;

    @Before
    public void setUp() throws UnknownJobException
    {
        m_ModelSnapshots = new ModelSnapshots();
        configureService(m_ModelSnapshots);
    }

    @Test
    public void testModelSnapshots_GivenNegativeSkip() throws UnknownJobException, NativeProcessRunException
    {
        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage("Parameter 'skip' cannot be < 0");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_SKIP_PARAM));

        m_ModelSnapshots.modelSnapshots("foo", -1, 100, "", "", "", "");
    }

    @Test
    public void testModelSnapshots_GivenNegativeTake() throws UnknownJobException, NativeProcessRunException
    {
        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage("Parameter 'take' cannot be < 0");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_TAKE_PARAM));

        m_ModelSnapshots.modelSnapshots("foo", 0, -1, "", "", "", "");
    }

    @Test
    public void testModelSnapshots_GivenOnePage() throws UnknownJobException, NativeProcessRunException, URISyntaxException
    {
        ModelSnapshot modelSnapshot = new ModelSnapshot();
        modelSnapshot.setSnapshotId("123");
        modelSnapshot.setQuantiles(new Quantiles());
        QueryPage<ModelSnapshot> queryResult = new QueryPage<>(Arrays.asList(modelSnapshot), 1);

        when(jobManager().modelSnapshots("foo", 0, 100, 0, 0, "", "")).thenReturn(queryResult);

        Pagination<ModelSnapshot> modelSnapshots = m_ModelSnapshots.modelSnapshots("foo", 0, 100, "", "", "", "");
        assertEquals(1, modelSnapshots.getHitCount());
        assertEquals(100, modelSnapshots.getTake());
        assertEquals("123", modelSnapshots.getDocuments().get(0).getSnapshotId());
        assertNull(modelSnapshots.getDocuments().get(0).getQuantiles());
    }

    @Test
    public void testModelSnapshots_GivenNoStartOrEndParams() throws UnknownJobException, NativeProcessRunException, URISyntaxException
    {
        QueryPage<ModelSnapshot> queryResult = new QueryPage<>(Arrays.asList(new ModelSnapshot()), 300);

        when(jobManager().modelSnapshots("foo", 0, 100, 0, 0, "", "")).thenReturn(queryResult);

        Pagination<ModelSnapshot> modelSnapshots = m_ModelSnapshots.modelSnapshots("foo", 0, 100, "", "", "", "");
        assertEquals(300, modelSnapshots.getHitCount());
        assertEquals(100, modelSnapshots.getTake());

        assertNull(modelSnapshots.getPreviousPage());
        String nextPageUri = modelSnapshots.getNextPage().toString();
        assertEquals("http://localhost/test/modelsnapshots/foo?skip=100&take=100",
                nextPageUri);
    }

    @Test
    public void testModelSnapshots_GivenEpochStartAndEpochEndParams() throws UnknownJobException,
            NativeProcessRunException
    {
        QueryPage<ModelSnapshot> queryResult = new QueryPage<>(Arrays.asList(new ModelSnapshot()), 300);

        when(jobManager().modelSnapshots("foo", 0, 100, 1000, 2000, "", "")).thenReturn(queryResult);

        Pagination<ModelSnapshot> modelSnapshots = m_ModelSnapshots.modelSnapshots("foo", 0, 100, "1", "2", "", "");

        assertEquals(300l, modelSnapshots.getHitCount());
        assertEquals(100l, modelSnapshots.getTake());
        assertEquals(0l, modelSnapshots.getSkip());

        assertNull(modelSnapshots.getPreviousPage());
        String nextPageUri = modelSnapshots.getNextPage().toString();
        assertEquals("http://localhost/test/modelsnapshots/foo?skip=100&take=100&start=1&end=2",
                nextPageUri);
    }

    @Test
    public void testModelSnapshots_GivenIsoWithoutMillisStartAndEpochEndParams() throws UnknownJobException,
            NativeProcessRunException
    {
        QueryPage<ModelSnapshot> queryResult = new QueryPage<>(Arrays.asList(new ModelSnapshot()), 300);

        when(jobManager().modelSnapshots("foo", 0, 100, 1420113600000L, 1420117200000L, "", ""))
                .thenReturn(queryResult);

        Pagination<ModelSnapshot> modelSnapshots = m_ModelSnapshots.modelSnapshots("foo", 0, 100,
                "2015-01-01T12:00:00Z", "2015-01-01T13:00:00Z", "", "");

        assertEquals(300l, modelSnapshots.getHitCount());
        assertEquals(100l, modelSnapshots.getTake());
        assertEquals(0l, modelSnapshots.getSkip());

        assertNull(modelSnapshots.getPreviousPage());
        String nextPageUri = modelSnapshots.getNextPage().toString();
        assertEquals(
                "http://localhost/test/modelsnapshots/foo?skip=100&take=100&start=2015-01-01T12%3A00%3A00Z&end=2015-01-01T13%3A00%3A00Z",
                nextPageUri);
    }

    @Test
    public void testModelSnapshots_GivenIsoWithMillisStartAndEpochEndParams() throws UnknownJobException,
            NativeProcessRunException
    {
        QueryPage<ModelSnapshot> queryResult = new QueryPage<>(Arrays.asList(new ModelSnapshot()), 300);

        when(jobManager().modelSnapshots("foo", 0, 100, 1420113600042L, 1420117200142L, "", ""))
                .thenReturn(queryResult);

        Pagination<ModelSnapshot> modelSnapshots = m_ModelSnapshots.modelSnapshots("foo", 0, 100,
                "2015-01-01T12:00:00.042Z", "2015-01-01T13:00:00.142+00:00", "", "");

        assertEquals(300l, modelSnapshots.getHitCount());
        assertEquals(100l, modelSnapshots.getTake());
        assertEquals(0l, modelSnapshots.getSkip());

        assertNull(modelSnapshots.getPreviousPage());
        String nextPageUri = modelSnapshots.getNextPage().toString();
        assertEquals(
                "http://localhost/test/modelsnapshots/foo?skip=100&take=100&start=2015-01-01T12%3A00%3A00.042Z&end=2015-01-01T13%3A00%3A00.142%2B00%3A00",
                nextPageUri);
    }

    @Test
    public void testModelSnapshots_GivenInvalidStartAndEpochEndParams() throws UnknownJobException,
            NativeProcessRunException
    {
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage("Query param 'start' with value 'invalid' cannot be parsed as a date or converted to a number (epoch)");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.UNPARSEABLE_DATE_ARGUMENT));

        m_ModelSnapshots.modelSnapshots("foo", 0, 100, "invalid", "also invalid", "", "");
    }

    @Test
    public void testRevert_GivenNoArgs() throws JobInUseException, UnknownJobException,
            NoSuchModelSnapshotException
    {
        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage("Cannot revert to a model snapshot as no parameters were specified.");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_REVERT_PARAMS));

        m_ModelSnapshots.revertToSnapshot("foo", "", "", "", false);
    }

    @Test
    public void testRevert_GivenInvalidArgs() throws JobInUseException, UnknownJobException,
            NoSuchModelSnapshotException
    {
        m_ExpectedException.expect(RestApiException.class);
        m_ExpectedException.expectMessage("Query param 'time' with value 'invalid' cannot be parsed as a date or converted to a number (epoch)");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.UNPARSEABLE_DATE_ARGUMENT));

        m_ModelSnapshots.revertToSnapshot("foo", "invalid", "ok", "fine", false);
    }

    @Test
    public void testRevert_GivenValidTime() throws JobInUseException, UnknownJobException,
            NoSuchModelSnapshotException
    {
        ModelSnapshot modelSnapshot = new ModelSnapshot();
        modelSnapshot.setSnapshotId("foo");
        when(jobManager().revertToSnapshot("foo", 1001L, "", "", false)).thenReturn(modelSnapshot);

        Response response = m_ModelSnapshots.revertToSnapshot("foo", "1", "", "", false);

        assertEquals(200, response.getStatus());
    }

    @Test
    public void testRevert_GivenValidId() throws JobInUseException, UnknownJobException,
            NoSuchModelSnapshotException
    {
        ModelSnapshot modelSnapshot = new ModelSnapshot();
        modelSnapshot.setSnapshotId("foo");
        modelSnapshot.setQuantiles(new Quantiles());
        when(jobManager().revertToSnapshot("foo", 0L, "123", "", false)).thenReturn(modelSnapshot);

        Response response = m_ModelSnapshots.revertToSnapshot("foo", "", "123", "", false);

        assertEquals(200, response.getStatus());
        @SuppressWarnings("unchecked")
        SingleDocument<ModelSnapshot> entity = (SingleDocument<ModelSnapshot>)response.getEntity();
        assertEquals("foo", entity.getDocument().getSnapshotId());
        assertNull(entity.getDocument().getQuantiles());
    }

    @Test
    public void testRevert_GivenValidDescription() throws JobInUseException, UnknownJobException,
            NoSuchModelSnapshotException
    {
        ModelSnapshot modelSnapshot = new ModelSnapshot();
        modelSnapshot.setSnapshotId("foo");
        when(jobManager().revertToSnapshot("foo", 0L, "", "my description", false)).thenReturn(modelSnapshot);

        Response response = m_ModelSnapshots.revertToSnapshot("foo", "", "", "my description", false);

        assertEquals(200, response.getStatus());
    }

    @Test
    public void testUpdateDescription_GivenMissingArg() throws JobInUseException, UnknownJobException,
            NoSuchModelSnapshotException, DescriptionAlreadyUsedException, MalformedJsonException
    {
        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage("Both snapshot ID and new description must be provided when changing a model snapshot description.");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_DESCRIPTION_PARAMS));

        m_ModelSnapshots.updateDescription("foo", "123", "");
    }

    @Test
    public void testUpdateDescription_GivenWrongJson() throws JobInUseException, UnknownJobException,
            NoSuchModelSnapshotException, DescriptionAlreadyUsedException, MalformedJsonException
    {
        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage("Both snapshot ID and new description must be provided when changing a model snapshot description.");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_DESCRIPTION_PARAMS));

        m_ModelSnapshots.updateDescription("foo", "123", "{ \"escription\" : \"new description\" }");
    }

    @Test
    public void testUpdateDescription_GivenValidDescription() throws JobInUseException, UnknownJobException,
            NoSuchModelSnapshotException, DescriptionAlreadyUsedException, MalformedJsonException
    {
        ModelSnapshot modelSnapshot = new ModelSnapshot();
        modelSnapshot.setSnapshotId("123");
        modelSnapshot.setQuantiles(new Quantiles());
        when(jobManager().updateModelSnapshotDescription("foo", "123", "new description")).thenReturn(modelSnapshot);

        Response response = m_ModelSnapshots.updateDescription("foo", "123", "{ \"description\" : \"new description\" }");

        assertEquals(200, response.getStatus());
        @SuppressWarnings("unchecked")
        SingleDocument<ModelSnapshot> entity = (SingleDocument<ModelSnapshot>)response.getEntity();
        assertEquals("123", entity.getDocument().getSnapshotId());
        assertNull(entity.getDocument().getQuantiles());
    }

    @Test
    public void testDelete_GivenValidId() throws JobInUseException, UnknownJobException,
            NoSuchModelSnapshotException, CannotDeleteSnapshotException
    {
        ModelSnapshot modelSnapshot = new ModelSnapshot();
        modelSnapshot.setSnapshotId("123");
        when(jobManager().deleteModelSnapshot("foo", "123")).thenReturn(modelSnapshot);

        Response response = m_ModelSnapshots.deleteModelSnapshot("foo", "123");

        assertEquals(200, response.getStatus());
    }
}
