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

package org.elasticsearch.prelert.job;

import java.util.Arrays;

/**
 * Jobs whether running or complete are in one of these states.
 * When a job is created it is initialised in to the status closed
 * i.e. it is not running.
 */
public enum JobStatus
{
    RUNNING, CLOSING, CLOSED, FAILED, PAUSING, PAUSED;

    /**
     * @return {@code true} if status matches any of the given {@code candidates}
     */
    public boolean isAnyOf(JobStatus... candidates)
    {
        return Arrays.stream(candidates).anyMatch(candidate -> this == candidate);
    }
}
