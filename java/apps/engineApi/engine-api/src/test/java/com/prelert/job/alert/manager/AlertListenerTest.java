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

package com.prelert.job.alert.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.util.Arrays;
import java.util.Date;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.UriBuilder;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.prelert.job.alert.Alert;
import com.prelert.job.alert.AlertTrigger;
import com.prelert.job.alert.AlertType;
import com.prelert.job.manager.JobManager;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.Detector;

public class AlertListenerTest
{
    @Mock private JobProvider m_JobProvider;
    @Mock private JobManager m_JobManager;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }


    @Test
    public void testGetMethods()
    {
        AlertManager manager = mock(AlertManager.class);
        AsyncResponse response = mock(AsyncResponse.class);
        URI baseUri = UriBuilder.fromPath("testing").build();

        AlertListener listener = new AlertListener(response, manager, "foo",
                createAlertTypes(AlertType.BUCKET, 20.0, 30.0), baseUri);

        assertEquals(baseUri, listener.getBaseUri());
        assertEquals("foo", listener.getJobId());
        assertEquals(response, listener.getResponse());
    }

    @Test
    public void testFire()
    {
        AlertManager manager = mock(AlertManager.class);
        AsyncResponse response = mock(AsyncResponse.class);
        URI baseUri = UriBuilder.fromUri("http://testing").build();

        ArgumentCaptor<Alert> argument = ArgumentCaptor.forClass(Alert.class);

        AlertListener listener = new AlertListener(response, manager, "foo",
                createAlertTypes(AlertType.BUCKET, 20.0, 30.0), baseUri);

        Bucket bucket = createBucket();
        listener.fire(bucket);

        Mockito.verify(manager, Mockito.times(1)).deregisterResponse(response);
        Mockito.verify(response).resume(argument.capture());

        assertEquals("foo", argument.getValue().getJobId());
        assertEquals(bucket.getAnomalyScore(), argument.getValue().getAnomalyScore(), 0000.1);
        assertEquals(bucket.getMaxNormalizedProbability(), argument.getValue().getMaxNormalizedProbability(), 0000.1);

        URI uri = UriBuilder.fromUri(baseUri)
                                .path("results")
                                .path("foo")
                                .path("buckets")
                                .path(bucket.getId())
                                .queryParam("expand", true)
                                .build();
        assertEquals(uri, argument.getValue().getUri());
    }

    @Test
    public void testFire_isBucketAlert()
    {
        AlertManager manager = mock(AlertManager.class);
        AsyncResponse response = mock(AsyncResponse.class);
        URI baseUri = UriBuilder.fromUri("http://testing").build();

        ArgumentCaptor<Alert> argument = ArgumentCaptor.forClass(Alert.class);

        AlertListener listener = new AlertListener(response, manager, "foo",
                createAlertTypes(AlertType.BUCKET, 20.0, 30.0), baseUri);

        Bucket bucket = createBucket();
        listener.fire(bucket);

        Mockito.verify(response).resume(argument.capture());

        assertEquals(null, argument.getValue().getRecords());
        assertTrue(argument.getValue().getBucket() != null);
        assertEquals(8, argument.getValue().getBucket().getRecordCount());
        assertEquals(8, argument.getValue().getBucket().getRecords().size());

        for (AnomalyRecord r : argument.getValue().getBucket().getRecords())
        {
            assertTrue(r.getNormalizedProbability() >= 30.0);
        }
    }


    @Test
    public void testFire_OnlyRecordsInAlert()
    {
        AlertManager manager = mock(AlertManager.class);
        AsyncResponse response = mock(AsyncResponse.class);
        URI baseUri = UriBuilder.fromUri("http://testing").build();

        ArgumentCaptor<Alert> argument = ArgumentCaptor.forClass(Alert.class);

        AlertListener listener = new AlertListener(response, manager, "foo",
                createAlertTypes(AlertType.BUCKET, 80.0, 50.0), baseUri);

        Bucket bucket = createBucket();
        listener.fire(bucket);

        Mockito.verify(response).resume(argument.capture());

        assertEquals(null, argument.getValue().getBucket());
        assertTrue(argument.getValue().getRecords() != null);
        assertEquals(6, argument.getValue().getRecords().size());

        for (AnomalyRecord r : argument.getValue().getRecords())
        {
            assertTrue(r.getNormalizedProbability() >= 50.0);
        }
    }

    /**
     * create bucket with 10 anomaly records with anomaly scores
     * 10, 20, 30,... ,100
     * @return
     */
    private Bucket createBucket()
    {
        Detector d = new Detector("d1");
        for (int i=10; i<=100; i=i+10)
        {
            AnomalyRecord a1 = new AnomalyRecord();
            a1.setNormalizedProbability(i);
            d.addRecord(a1);
        }

        Bucket b = new Bucket();
        b.setAnomalyScore(40);
        b.setDetectors(Arrays.asList(d));
        b.setEventCount(5);
        b.setId("testbucket");
        b.setInterim(false);
        b.setMaxNormalizedProbability(60);
        b.setRecordCount(0);
        b.setTimestamp(new Date());
        return b;
    }

    private AlertTrigger [] createAlertTypes(AlertType type, double a, double b)
    {
        return new AlertTrigger [] {new AlertTrigger(a, b, type)};
    }
}
