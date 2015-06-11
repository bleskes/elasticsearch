/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
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

package com.prelert.rs.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.prelert.job.results.Bucket;
import com.prelert.rs.data.SingleDocument;
import com.prelert.rs.resources.Buckets;

public class BucketRequestBuilder extends BaseJobRequestBuilder<Bucket>
{
    private final String m_BucketId;

    private final Map<String, String> m_Params;

    /**
     * @param client The Engine API client
     * @param jobId The Job's unique Id
     * @param bucketId The id of the requested bucket
     */
    public BucketRequestBuilder(EngineApiClient client, String jobId, String bucketId)
    {
        super(client, jobId);
        m_Params = new HashMap<>();
        m_BucketId = bucketId;
    }

    /**
     * Sets whether anomaly records should be in-lined with the results. Default is false.
     *
     * @param shouldExpand Should the buckets be expanded to contain the records or not
     * @return this {@code Builder} object
     */
    public BucketRequestBuilder expand(boolean shouldExpand)
    {
        m_Params.put(Buckets.EXPAND_QUERY_PARAM, Boolean.toString(shouldExpand));
        return this;
    }

    /**
     * Sets whether interim results are included in result. Default is false.
     *
     * @param includeInterim Should interim results be included or not
     * @return this {@code Builder} object
     */
    public BucketRequestBuilder includeInterim(boolean includeInterim)
    {
        m_Params.put(Buckets.INCLUDE_INTERIM_QUERY_PARAM, Boolean.toString(includeInterim));
        return this;
    }

    /**
     * Returns a single document with the bucket that was requested
     *
     * @return A {@link SingleDocument} object containing the requested {@link Bucket} object
     * @throws IOException
     */
    public SingleDocument<Bucket> get() throws IOException
    {
        StringBuilder url = new StringBuilder();
        url.append(baseUrl()).append("/results/").append(jobId()).append("/buckets/").append(m_BucketId);
        appendParams(m_Params, url);
        return createHttpGetRequester().getSingleDocument(url.toString(),
                new TypeReference<SingleDocument<Bucket>>() {});
    }
}
