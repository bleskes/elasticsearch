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

package com.prelert.job.persistence;

import com.prelert.job.ModelSnapshot;
import com.prelert.job.UnknownJobException;
import com.prelert.job.audit.Auditor;
import com.prelert.job.quantiles.Quantiles;

public interface JobProvider extends JobDetailsProvider, JobResultsProvider
{
    /**
     * Get the persisted quantiles state for the job
     */
    public Quantiles getQuantiles(String jobId)
    throws UnknownJobException;

    /**
     * Get the model snapshot for the job that has the highest restore priority
     */
    public ModelSnapshot getModelSnapshotByPriority(String jobId)
    throws UnknownJobException;

    /**
     * Refresh the datastore index so that all recent changes are
     * available to search operations. This is a synchronous blocking
     * call that should not return until the index has been refreshed.
     *
     * @param jobId
     */
    public void refreshIndex(String jobId);

    /**
     * Get an auditor for the given job
     *
     * @param the job id
     * @return the {@code Auditor}
     */
    Auditor audit(String jobId);
}
