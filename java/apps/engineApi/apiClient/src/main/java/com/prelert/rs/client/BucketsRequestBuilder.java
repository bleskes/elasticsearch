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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.resources.Buckets;

public class BucketsRequestBuilder extends BaseJobRequestBuilder<Bucket>
{
    private final Map<String, String> m_Params;

    /**
     * @param client The engine API client
     * @param jobId The Job's unique Id
     */
    public BucketsRequestBuilder(EngineApiClient client, String jobId)
    {
        super(client, jobId);
        m_Params = new HashMap<>();
    }

    /**
     * Sets whether anomaly records should be in-lined with the results. Default is false.
     *
     * @param shouldExpand Should the buckets be expanded to contain the records or not
     * @return this {@code Builder} object
     */
    public BucketsRequestBuilder expand(boolean shouldExpand)
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
    public BucketsRequestBuilder includeInterim(boolean includeInterim)
    {
        m_Params.put(Buckets.INCLUDE_INTERIM_QUERY_PARAM, Boolean.toString(includeInterim));
        return this;
    }

    /**
     * Return only buckets with an anomalyScore &gt;= this value.
     *
     * @param value The anomaly score threshold
     * @return this {@code Builder} object
     */
    public BucketsRequestBuilder anomalyScoreThreshold(double value)
    {
        m_Params.put(Bucket.ANOMALY_SCORE, Double.toString(value));
        return this;
    }

    /**
     * Return only buckets with a maxNormalizedProbability &gt;= this value.
     *
     * @param value The normalized probability threshold
     * @return this {@code Builder} object
     */
    public BucketsRequestBuilder normalizedProbabilityThreshold(double value)
    {
        m_Params.put(Bucket.MAX_NORMALIZED_PROBABILITY, Double.toString(value));
        return this;
    }

    /**
     * Sets the number of buckets to skip. Default is 0.
     *
     * @param value The number of buckets to skip
     * @return this {@code Builder} object
     */
    public BucketsRequestBuilder skip(long value)
    {
        m_Params.put("skip", Long.toString(value));
        return this;
    }

    /**
     * Sets the max number of buckets to request. Default is 100.
     *
     * @param value The number of buckets to request
     * @return this {@code Builder} object
     */
    public BucketsRequestBuilder take(long value)
    {
        m_Params.put("take", Long.toString(value));
        return this;
    }

    /**
     * Filters out buckets that start before the given value.
     * Value is expected in seconds from the Epoch.
     *
     * @param value The start date as seconds from the Epoch
     * @return this {@code Builder} object
     */
    public BucketsRequestBuilder start(long value)
    {
        m_Params.put(Buckets.START_QUERY_PARAM, Long.toString(value));
        return this;
    }

    /**
     * Filters out buckets that start before the given value.
     * Value is expected as an ISO 8601 date String.
     *
     * @param value The start date as an ISO 8601 String
     * @return this {@code Builder} object
     * @throws UnsupportedEncodingException
     */
    public BucketsRequestBuilder start(String value) throws UnsupportedEncodingException
    {
        m_Params.put(Buckets.START_QUERY_PARAM, URLEncoder.encode(value, "UTF-8"));
        return this;
    }

    /**
     * Filters out buckets that start at or after the given value.
     * Value is expected in seconds from the Epoch.
     *
     * @param value The end data as seconds from the Epoch
     * @return this {@code Builder} object
     */
    public BucketsRequestBuilder end(long value)
    {
        m_Params.put(Buckets.END_QUERY_PARAM, Long.toString(value));
        return this;
    }

    /**
     * Filters out buckets that start at or after the given value.
     * Value is expected as an ISO 8601 date String.
     *
     * @param value The end date as an ISO 8601 String
     *
     * @return this {@code Builder} object
     * @throws UnsupportedEncodingException
     */
    public BucketsRequestBuilder end(String value) throws UnsupportedEncodingException
    {
        m_Params.put(Buckets.END_QUERY_PARAM, URLEncoder.encode(value, "UTF-8"));
        return this;
    }

    /**
     * Returns the page with the buckets that were requested
     *
     * @return A {@link Pagination} object containing the resulted {@link Bucket} objects
     * @throws IOException
     */
    public Pagination<Bucket> get() throws IOException
    {
        StringBuilder url = new StringBuilder();
        url.append(baseUrl()).append("/results/").append(jobId()).append("/buckets");
        appendParams(m_Params, url);
        return createHttpGetRequester().getPage(url.toString(),
                new TypeReference<Pagination<Bucket>>() {});
    }
}
