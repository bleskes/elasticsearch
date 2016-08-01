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
