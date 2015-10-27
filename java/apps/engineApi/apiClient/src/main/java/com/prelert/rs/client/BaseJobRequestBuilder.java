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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

class BaseJobRequestBuilder<T>
{
    public static final String START_QUERY_PARAM = "start";
    public static final String END_QUERY_PARAM = "end";
    public static final String SORT_QUERY_PARAM = "sort";
    public static final String DESCENDING_ORDER = "desc";

    private final EngineApiClient m_Client;
    private final String m_JobId;

    /**
     * @param client The Engine API client
     * @param jobId The Job's unique Id
     */
    public BaseJobRequestBuilder(EngineApiClient client, String jobId)
    {
        m_Client = client;
        m_JobId = jobId;
    }

    protected String baseUrl()
    {
        return m_Client.getBaseUrl();
    }

    protected String jobId()
    {
        return m_JobId;
    }

    protected HttpGetRequester<T> createHttpGetRequester()
    {
        return new HttpGetRequester<>(m_Client);
    }

    protected static void appendParams(Map<String, String> params, StringBuilder url)
    {
        if (!params.isEmpty())
        {
            List<String> paramPairs = new ArrayList<>();
            params.forEach((key, value) -> paramPairs.add(key + "=" + value));
            url.append('?');

            StringJoiner joiner = new StringJoiner("&");
            paramPairs.forEach(pair -> joiner.add(pair));
            url.append(joiner.toString());
        }
    }
}
