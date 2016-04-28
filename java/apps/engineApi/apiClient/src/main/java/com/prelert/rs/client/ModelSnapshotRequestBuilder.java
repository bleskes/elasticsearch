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


package com.prelert.rs.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.prelert.job.ModelSnapshot;
import com.prelert.rs.data.SingleDocument;

public class ModelSnapshotRequestBuilder extends BaseJobRequestBuilder<ModelSnapshot> {
    public static final String ENDPOINT = "/modelsnapshots/";

    private Map<String, String> m_Params;

    public ModelSnapshotRequestBuilder(EngineApiClient client, String jobId)
    {
        super(client, jobId);
        m_Params = new HashMap<>();
    }

    /**
     * Sets the number of snapshots to skip. Default is 0.
     *
     * @param value The number of snapshots to skip
     * @return this {@code Builder} object
     */
    public ModelSnapshotRequestBuilder skip(long value)
    {
        m_Params.put(SKIP_QUERY_PARAM, Long.toString(value));
        return this;
    }

    /**
     * Sets the max number of snapshots to request. Default is 100.
     *
     * @param value The number of snapshots to request
     * @return this {@code Builder} object
     */
    public ModelSnapshotRequestBuilder take(long value)
    {
        m_Params.put(TAKE_QUERY_PARAM, Long.toString(value));
        return this;
    }

    /**
     * Filters out snapshots that start before the given value.
     * Value is expected in seconds from the Epoch.
     *
     * @param value The start date as seconds from the Epoch
     * @return this {@code Builder} object
     */
    public ModelSnapshotRequestBuilder start(long value)
    {
        m_Params.put(START_QUERY_PARAM, Long.toString(value));
        return this;
    }

    /**
     * Filters out snapshots that start before the given value.
     * Value is expected as an ISO 8601 date String.
     *
     * @param value The start date as an ISO 8601 String
     * @return this {@code Builder} object
     * @throws UnsupportedEncodingException If UTF-8 not supported
     */
    public ModelSnapshotRequestBuilder start(String value) throws UnsupportedEncodingException
    {
        m_Params.put(START_QUERY_PARAM, URLEncoder.encode(value, "UTF-8"));
        return this;
    }

    /**
     * Filters out snapshots that start at or after the given value.
     * Value is expected in seconds from the Epoch.
     *
     * @param value The end date as seconds from the Epoch
     * @return this {@code Builder} object
     */
    public ModelSnapshotRequestBuilder end(long value)
    {
        m_Params.put(END_QUERY_PARAM, Long.toString(value));
        return this;
    }

    /**
     * Filters out snapshots that start at or after the given value.
     * Value is expected as an ISO 8601 date String.
     *
     * @param value The end date as an ISO 8601 String
     *
     * @return this {@code Builder} object
     * @throws UnsupportedEncodingException If UTF-8 not supported
     */
    public ModelSnapshotRequestBuilder end(String value) throws UnsupportedEncodingException
    {
        m_Params.put(END_QUERY_PARAM, URLEncoder.encode(value, "UTF-8"));
        return this;
    }

    /**
     * Filter by description
     *
     * @param description The description to filter on
     * @return this {@code Builder} object
     */
    public ModelSnapshotRequestBuilder description(String description)
    {
        m_Params.put(ModelSnapshot.DESCRIPTION, description);
        return this;
    }

    /**
     * Sets the field to sort by
     *
     * @param field The field to sort by
     * @return this {@code Builder} object
     */
    public ModelSnapshotRequestBuilder sortField(String field)
    {
        m_Params.put(SORT_QUERY_PARAM, field);
        return this;
    }

    /**
     * Sets whether the sorting order is descending
     *
     * @param descending Should the sorting order be descending or not
     * @return this {@code Builder} object
     */
    public ModelSnapshotRequestBuilder descending(boolean descending)
    {
        m_Params.put(DESCENDING_ORDER, Boolean.toString(descending));
        return this;
    }

    /**
     * Returns a single document with the snapshot that was requested
     *
     * @return A {@link SingleDocument} object containing the requested {@link ModelSnapshot} object
     * @throws IOException If HTTP GET fails
     */
    public ModelSnapshot get()
    throws IOException
    {
        StringBuilder url = new StringBuilder();
        url.append(baseUrl()).append(ENDPOINT).append(jobId());
        appendParams(m_Params, url);

        return createHttpGetRequester().get(url.toString(), new TypeReference<ModelSnapshot>() {});
    }

}
