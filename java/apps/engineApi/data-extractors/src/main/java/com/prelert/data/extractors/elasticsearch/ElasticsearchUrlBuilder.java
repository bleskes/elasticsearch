/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
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

package com.prelert.data.extractors.elasticsearch;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ElasticsearchUrlBuilder
{
    private static final String SLASH = "/";
    private static final String COMMA = ",";
    private static final int SCROLL_CONTEXT_MINUTES = 60;
    private static final String INDEX_SETTINGS_END_POINT = "%s/_settings";
    private static final String SEARCH_SIZE_ONE_END_POINT = "_search?size=1";
    private static final String SEARCH_SCROLL_END_POINT = "_search?scroll=" + SCROLL_CONTEXT_MINUTES + "m&size=%d";
    private static final String CONTINUE_SCROLL_END_POINT = "_search/scroll?scroll=" + SCROLL_CONTEXT_MINUTES + "m";
    private static final String CLEAR_SCROLL_END_POINT = "_search/scroll";
    private static final String FIELD_STATS_END_POINT = "_field_stats?level=indices";

    private final String m_BaseUrl;
    private String m_Types;

    private ElasticsearchUrlBuilder(String baseUrl, String types)
    {
        m_BaseUrl = Objects.requireNonNull(baseUrl);
        m_Types = Objects.requireNonNull(types);
    }

    public static ElasticsearchUrlBuilder create(String baseUrl, List<String> types)
    {
        String sanitisedBaseUrl = baseUrl.endsWith(SLASH) ? baseUrl : baseUrl + SLASH;
        String typesAsString = types.stream().collect(Collectors.joining(COMMA));
        return new ElasticsearchUrlBuilder(sanitisedBaseUrl, typesAsString);
    }

    public String buildIndexSettingsUrl(String index)
    {
        return newUrlBuilder().append(String.format(INDEX_SETTINGS_END_POINT, index)).toString();
    }

    public String buildSearchSizeOneUrl(List<String> indices)
    {
        return buildUrlWithIndicesAndTypes(indices)
                .append(SEARCH_SIZE_ONE_END_POINT).toString();
    }

    public String buildInitScrollUrl(List<String> indices, int scrollSize)
    {
        return buildUrlWithIndicesAndTypes(indices)
                .append(String.format(SEARCH_SCROLL_END_POINT, scrollSize))
                .toString();
    }

    public String buildContinueScrollUrl()
    {
        return newUrlBuilder().append(CONTINUE_SCROLL_END_POINT).toString();
    }

    public String buildClearScrollUrl()
    {
        return newUrlBuilder().append(CLEAR_SCROLL_END_POINT).toString();
    }

    public String buildFieldStatsUrl(List<String> indices)
    {
        return buildUrlWithIndices(indices).append(FIELD_STATS_END_POINT).toString();
    }

    private StringBuilder newUrlBuilder()
    {
        return new StringBuilder(m_BaseUrl);
    }

    private StringBuilder buildUrlWithIndicesAndTypes(List<String> indices)
    {
        StringBuilder urlBuilder = buildUrlWithIndices(indices);
        if (!m_Types.isEmpty())
        {
            urlBuilder.append(m_Types).append(SLASH);
        }
        return urlBuilder;
    }

    private StringBuilder buildUrlWithIndices(List<String> indices)
    {
        return newUrlBuilder()
                .append(indices.stream().collect(Collectors.joining(COMMA))).append(SLASH);
    }

    public String getBaseUrl()
    {
        return m_BaseUrl;
    }
}
