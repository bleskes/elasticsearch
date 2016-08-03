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
package com.prelert.job.alert;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;


public class AlertTest {

    @Test
    public void testGettersAndSetters() throws URISyntaxException
    {
        Alert alert = new Alert();

        String s = new String("Mysterons");
        alert.setJobId(s);
        assertEquals(s, alert.getJobId());

        Date dt = new Date();
        alert.setTimestamp(dt);
        assertEquals(dt, alert.getTimestamp());

        double d = 66.5398281;
        alert.setAnomalyScore(d);
        assertEquals(d, alert.getAnomalyScore(), 0.0001);

        d = 99.99999;
        alert.setMaxNormalizedProbability(d);
        assertEquals(d, alert.getMaxNormalizedProbability() , 0.0001);

        URI u = new URI("http://beware.mysterons.com/index?message=death");
        alert.setUri(u);
        assertEquals(u, alert.getUri());

        assert(!alert.isTimeout());
        alert.setTimeout(true);
        assert(alert.isTimeout());

        assert(!alert.isInterim());
        alert.setInterim(true);
        assert(alert.isInterim());

        Bucket b = new Bucket();
        s = "Captain Scarlet";
        b.setId(s);
        alert.setBucket(b);
        assertEquals(b, alert.getBucket());
        assertEquals(s, alert.getBucket().getId());

        List<AnomalyRecord> l = new ArrayList<>();
        AnomalyRecord r = new AnomalyRecord();
        s = "Ozimandius";
        r.setId(s);
        l.add(r);
        alert.setRecords(l);
        assertEquals(l, alert.getRecords());
        assertEquals(s, alert.getRecords().get(0).getId());

        assertEquals(AlertType.BUCKET, alert.getAlertType());
        alert.setAlertType(AlertType.BUCKETINFLUENCER);
        assertEquals(AlertType.BUCKETINFLUENCER, alert.getAlertType());
    }
}
