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

import org.junit.Test;

public class AlertTypeTest {

    @Test
    public void testAlertTypes()
    {
        assertEquals("bucket", AlertType.BUCKET.toString());
        assertEquals("bucketinfluencer", AlertType.BUCKETINFLUENCER.toString());
        assertEquals("influencer", AlertType.INFLUENCER.toString());

        assertEquals(AlertType.BUCKET, AlertType.fromString("bucket"));
        assertEquals(AlertType.BUCKETINFLUENCER, AlertType.fromString("bucketinfluencer"));
        assertEquals(AlertType.INFLUENCER, AlertType.fromString("influencer"));

        boolean exception = false;
        try
        {
            AlertType.fromString("Non-Existent alert type here");
            assert(false);
        }
        catch (IllegalArgumentException ex)
        {
            exception = true;
        }
        assert(exception);
    }

}
