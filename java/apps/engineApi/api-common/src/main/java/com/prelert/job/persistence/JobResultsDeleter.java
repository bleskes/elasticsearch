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

import com.prelert.job.results.Bucket;
import com.prelert.job.results.Influencer;

public interface JobResultsDeleter
{
    /**
     * Delete a {@code Bucket} and its records
     * @param bucket the bucket to delete
     */
    void deleteBucket(Bucket bucket);

    /**
     * Delete the records of a {@code Bucket}
     * @param bucket the bucket whose records to delete
     */
    void deleteRecords(Bucket bucket);

    /**
     * Delete an {@code Influencer}
     * @param influencer the influencer to delete
     */
    void deleteInfluencer(Influencer influencer);

    /**
     * Commit the deletions and give the chance to implementors
     * to perform clean-up
     */
    void commit();
}
