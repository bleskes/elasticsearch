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

package com.prelert.job.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;

public class AuditActivityTest
{
    private long m_StartMillis;

    @Before
    public void setUp()
    {
        m_StartMillis = System.currentTimeMillis();
    }

    @Test
    public void testDefaultConstructor()
    {
        AuditActivity activity = new AuditActivity();
        assertEquals(0, activity.getTotalJobs());
        assertEquals(0, activity.getTotalDetectors());
        assertEquals(0, activity.getRunningJobs());
        assertEquals(0, activity.getRunningDetectors());
        assertNull(activity.getTimestamp());
    }

    @Test
    public void testNewActivity()
    {
        AuditActivity activity = AuditActivity.newActivity(10, 100, 5, 50);
        assertEquals(10, activity.getTotalJobs());
        assertEquals(100, activity.getTotalDetectors());
        assertEquals(5, activity.getRunningJobs());
        assertEquals(50, activity.getRunningDetectors());
        assertDateBetweenStartAndNow(activity.getTimestamp());
    }

    private void assertDateBetweenStartAndNow(Date timestamp)
    {
        long timestampMillis = timestamp.getTime();
        assertTrue(timestampMillis >= m_StartMillis);
        assertTrue(timestampMillis <= System.currentTimeMillis());
    }
}
