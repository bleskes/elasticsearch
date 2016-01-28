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
package com.prelert.job.results;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Defines the field names that we use for our results.
 * Fields from the raw data with these names are not added to any result.  Even
 * different types of results will not have raw data fields with reserved names
 * added to them, as it could create confusion if in some results a given field
 * contains raw data and in others it contains some aspect of our output.
 */
public final class ReservedFieldNames
{
    /**
     * jobId isn't in this package, so redefine.
     */
    private static final String JOB_ID_NAME = "jobId";

    /**
     * This array should be updated to contain all the field names that appear
     * in our results.
     */
    private static final String[] RESERVED_FIELD_NAME_ARRAY = {

        AnomalyCause.PROBABILITY,
        AnomalyCause.OVER_FIELD_NAME,
        AnomalyCause.OVER_FIELD_VALUE,
        AnomalyCause.BY_FIELD_NAME,
        AnomalyCause.BY_FIELD_VALUE,
        AnomalyCause.PARTITION_FIELD_NAME,
        AnomalyCause.PARTITION_FIELD_VALUE,
        AnomalyCause.FUNCTION,
        AnomalyCause.FUNCTION_DESCRIPTION,
        AnomalyCause.TYPICAL,
        AnomalyCause.ACTUAL,
        AnomalyCause.INFLUENCERS,
        AnomalyCause.FIELD_NAME,

        AnomalyRecord.ID,
        AnomalyRecord.PROBABILITY,
        AnomalyRecord.BY_FIELD_NAME,
        AnomalyRecord.BY_FIELD_VALUE,
        AnomalyRecord.PARTITION_FIELD_NAME,
        AnomalyRecord.PARTITION_FIELD_VALUE,
        AnomalyRecord.FUNCTION,
        AnomalyRecord.FUNCTION_DESCRIPTION,
        AnomalyRecord.TYPICAL,
        AnomalyRecord.ACTUAL,
        AnomalyRecord.IS_INTERIM,
        AnomalyRecord.INFLUENCERS,
        AnomalyRecord.FIELD_NAME,
        AnomalyRecord.OVER_FIELD_NAME,
        AnomalyRecord.OVER_FIELD_VALUE,
        AnomalyRecord.CAUSES,
        AnomalyRecord.ANOMALY_SCORE,
        AnomalyRecord.NORMALIZED_PROBABILITY,
        AnomalyRecord.INITIAL_NORMALIZED_PROBABILITY,

        Bucket.ID,
        Bucket.TIMESTAMP,
        Bucket.ANOMALY_SCORE,
        Bucket.MAX_NORMALIZED_PROBABILITY,
        Bucket.IS_INTERIM,
        Bucket.RECORD_COUNT,
        Bucket.EVENT_COUNT,
        Bucket.RECORDS,
        Bucket.BUCKET_INFLUENCERS,
        Bucket.INFLUENCERS,
        Bucket.ES_TIMESTAMP,
        Bucket.INITIAL_ANOMALY_SCORE,

        BucketInfluencer.BUCKET_TIME,
        BucketInfluencer.INFLUENCER_FIELD_NAME,
        BucketInfluencer.INITIAL_ANOMALY_SCORE,
        BucketInfluencer.ANOMALY_SCORE,
        BucketInfluencer.RAW_ANOMALY_SCORE,
        BucketInfluencer.PROBABILITY,

        Influence.INFLUENCER_FIELD_NAME,
        Influence.INFLUENCER_FIELD_VALUES,

        Influencer.ID,
        Influencer.PROBABILITY,
        Influencer.TIMESTAMP,
        Influencer.INFLUENCER_FIELD_NAME,
        Influencer.INFLUENCER_FIELD_VALUE,
        Influencer.INITIAL_ANOMALY_SCORE,
        Influencer.ANOMALY_SCORE,

        JOB_ID_NAME
    };

    /**
     * A set of all reserved field names in our results.  Fields from the raw
     * data with these names are not added to any result.
     */
    public static final Set<String> RESERVED_FIELD_NAMES = new HashSet<>(Arrays.asList(RESERVED_FIELD_NAME_ARRAY));

    private ReservedFieldNames()
    {
    }
}

