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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class JobStatusTest
{
    @Test
    public void testIsAnyOf()
    {
        assertFalse(JobStatus.RUNNING.isAnyOf());
        assertFalse(JobStatus.RUNNING.isAnyOf(JobStatus.CLOSED, JobStatus.CLOSING, JobStatus.FAILED,
                JobStatus.PAUSED, JobStatus.PAUSING));
        assertFalse(JobStatus.CLOSED.isAnyOf(JobStatus.RUNNING, JobStatus.CLOSING, JobStatus.FAILED,
                JobStatus.PAUSED, JobStatus.PAUSING));

        assertTrue(JobStatus.RUNNING.isAnyOf(JobStatus.RUNNING));
        assertTrue(JobStatus.RUNNING.isAnyOf(JobStatus.RUNNING, JobStatus.CLOSED));
        assertTrue(JobStatus.PAUSED.isAnyOf(JobStatus.PAUSED, JobStatus.PAUSING));
        assertTrue(JobStatus.PAUSING.isAnyOf(JobStatus.PAUSED, JobStatus.PAUSING));
    }
}
