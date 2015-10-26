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
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.prelert.job.results.Influencer;
import com.prelert.rs.data.Pagination;

public class InfluencersRequestBuilder extends BaseJobRequestBuilder<Influencer>
{
    private final Map<String, String> m_Params;

    /**
     * @param client The engine API client
     * @param jobId The Job's unique Id
     */
    public InfluencersRequestBuilder(EngineApiClient client, String jobId)
    {
        super(client, jobId);
        m_Params = new LinkedHashMap<>();
    }

    /**
     * Sets the number of influencers to skip. Default is 0.
     *
     * @param value The number of category definitions to skip
     * @return this {@code Builder} object
     */
    public InfluencersRequestBuilder skip(long value)
    {
        m_Params.put("skip", Long.toString(value));
        return this;
    }

    /**
     * Sets the max number of influencers to request. Default is 100.
     *
     * @param value The number of category definitions to request
     * @return this {@code Builder} object
     */
    public InfluencersRequestBuilder take(long value)
    {
        m_Params.put("take", Long.toString(value));
        return this;
    }

    /**
     * Returns the page with the influencers that were requested
     *
     * @return A {@link Pagination} object containing the resulted {@link Influencer}
     * objects
     * @throws IOException
     */
    public Pagination<Influencer> get() throws IOException
    {
        StringBuilder url = new StringBuilder();
        url.append(baseUrl()).append("/results/").append(jobId()).append("/influencers");
        appendParams(m_Params, url);
        return createHttpGetRequester().getPage(url.toString(),
                new TypeReference<Pagination<Influencer>>() {});
    }
}
