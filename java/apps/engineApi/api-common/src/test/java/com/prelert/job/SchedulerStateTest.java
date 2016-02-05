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

package com.prelert.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SchedulerStateTest
{
    @Test
    public void testEquals_GivenDifferentClass()
    {
        assertFalse(new SchedulerState().equals("a string"));
    }

    @Test
    public void testEquals_GivenSameReference()
    {
        SchedulerState schedulerState = new SchedulerState();
        assertTrue(schedulerState.equals(schedulerState));
    }

    @Test
    public void testEquals_GivenEqualObjects()
    {
        SchedulerState schedulerState1 = new SchedulerState();
        schedulerState1.setStartTimeMillis(18L);
        schedulerState1.setEndTimeMillis(42L);

        SchedulerState schedulerState2 = new SchedulerState();
        schedulerState2.setStartTimeMillis(18L);
        schedulerState2.setEndTimeMillis(42L);

        assertTrue(schedulerState1.equals(schedulerState2));
        assertTrue(schedulerState2.equals(schedulerState1));
        assertEquals(schedulerState1.hashCode(), schedulerState2.hashCode());
    }
}
