/****************************************************************************
 *                                                                          *
 * Copyright 2015-2016 Prelert Ltd                                          *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 *                                                                          *
 ***************************************************************************/

package com.prelert.rs.client;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ModelSnapshotsRequestBuilderTest
{
    private static final String BASE_URL = "http://localhost:8080";

    @Mock private EngineApiClient m_Client;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        when(m_Client.getBaseUrl()).thenReturn(BASE_URL);
    }

    @Test
    public void testGet_GivenNoOptionalParameters() throws IOException
    {
        new ModelSnapshotsRequestBuilder(m_Client, "foo").get();
        verify(m_Client).get(eq(BASE_URL + "/modelsnapshots/foo"), any(), eq(true));
    }

    @Test
    public void testGet_GivenSkipAndTake() throws IOException
    {
        new ModelSnapshotsRequestBuilder(m_Client, "foo").skip(42L).take(420L).get();
        verify(m_Client).get(eq(BASE_URL + "/modelsnapshots/foo?skip=42&take=420"), any(), eq(true));
    }

    @Test
    public void testGet_GivenEpochStartAndEnd() throws IOException
    {
        new ModelSnapshotsRequestBuilder(m_Client, "foo").start(4200L).end(8400L).get();
        verify(m_Client).get(eq(BASE_URL + "/modelsnapshots/foo?start=4200&end=8400"), any(), eq(true));
    }

    @Test
    public void testGet_GivenStringStartAndEnd() throws IOException
    {
        new ModelSnapshotsRequestBuilder(m_Client, "foo")
                .start("2016-01-01T00:00:00Z").end("2016-02-01T00:00:00Z").get();
        verify(m_Client).get(
                eq(BASE_URL + "/modelsnapshots/foo?start=2016-01-01T00%3A00%3A00Z&end=2016-02-01T00%3A00%3A00Z"),
                any(), eq(true));
    }

    @Test
    public void testGet_GivenSortAndOrder() throws IOException
    {
        new ModelSnapshotsRequestBuilder(m_Client, "foo").sortField("timestamp").descending(false).get();
        verify(m_Client).get(eq(BASE_URL + "/modelsnapshots/foo?sort=timestamp&desc=false"), any(),
                eq(true));
    }

    @Test
    public void testGet_GivenDescription() throws IOException
    {
        new ModelSnapshotsRequestBuilder(m_Client, "foo").description("foo").get();
        verify(m_Client).get(eq(BASE_URL + "/modelsnapshots/foo?description=foo"), any(), eq(true));
    }
}
