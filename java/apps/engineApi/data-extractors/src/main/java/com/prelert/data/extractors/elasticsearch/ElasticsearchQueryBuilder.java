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

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import org.apache.log4j.Logger;

import com.prelert.job.ElasticsearchDataSourceCompatibility;

public class ElasticsearchQueryBuilder
{
    /**
     * The search body for Elasticsearch version 1.7.x contains sorting
     * based on the time field and a query. The query is composed by
     * a bool query with two must clauses, the recommended way to perform an AND query.
     * There are 6 placeholders:
     * <ol>
     *   <li> sort field
     *   <li> user defined query
     *   <li> time field
     *   <li> start time (String in date_time format)
     *   <li> end time (String in date_time format)
     *   <li> extra (may be empty or contain aggregations, fields, etc.)
     * </ol
     */
    private static final String SEARCH_BODY_TEMPLATE_1_7_X = "{"
            + "\"sort\": ["
            +   "{\"%s\": {\"order\": \"asc\"}}"
            + "],"
            + "\"query\": {"
            +   "\"filtered\": {"
            +     "\"filter\": {"
            +       "\"bool\": {"
            +         "\"must\": {%s},"
            +         "\"must\": {"
            +           "\"range\": {"
            +             "\"%s\": {"
            +               "\"gte\": \"%s\","
            +               "\"lt\": \"%s\","
            +               "\"format\": \"date_time\""
            +             "}"
            +           "}"
            +         "}"
            +       "}"
            +     "}"
            + "  }"
            + "}%s"
            + "}";

    /**
     * The search body for Elasticsearch version 2.x contains sorting
     * based on the time field and a query. The query is composed by
     * a bool query with two must clauses, the recommended way to perform an AND query.
     * There are 6 placeholders:
     * <ol>
     *   <li> sort field
     *   <li> user defined query
     *   <li> time field
     *   <li> start time (String in date_time format)
     *   <li> end time (String in date_time format)
     *   <li> extra (may be empty or contain aggregations, fields, etc.)
     * </ol
     */
    private static final String SEARCH_BODY_TEMPLATE_2_X = "{"
            + "\"sort\": ["
            +   "{\"%s\": {\"order\": \"asc\"}}"
            + "],"
            + "\"query\": {"
            +   "\"bool\": {"
            +     "\"filter\": ["
            +       "{%s},"
            +       "{"
            +         "\"range\": {"
            +           "\"%s\": {"
            +             "\"gte\": \"%s\","
            +             "\"lt\": \"%s\","
            +             "\"format\": \"date_time\""
            +           "}"
            +         "}"
            +       "}"
            +     "]"
            +   "}"
            + "}%s"
            + "}";

    private static final String DATA_SUMMARY_SORT_FIELD = "_doc";

    /**
     * Aggregations in order to retrieve the earliest and latest record times.
     * The single placeholder expects the time field.
     */
    private static final String DATA_SUMMARY_AGGS_TEMPLATE = ""
    + "{"
    +   "\"earliestTime\":{"
    +     "\"min\":{\"field\":\"%1$s\"}"
    +   "},"
    +   "\"latestTime\":{"
    +     "\"max\":{\"field\":\"%1$s\"}"
    +   "}"
    + "}";

    private static final String AGGREGATION_TEMPLATE = ", \"aggs\": %s";
    private static final String SCRIPT_FIELDS_TEMPLATE = ", \"script_fields\": %s";
    private static final String FIELDS_TEMPLATE = "%s,  \"fields\": %s";

    private final ElasticsearchDataSourceCompatibility m_Compatibility;
    private final String m_Search;
    private final String m_Aggregations;
    private final String m_ScriptFields;
    private final String m_Fields;
    private final String m_TimeField;

    public ElasticsearchQueryBuilder(ElasticsearchDataSourceCompatibility compatibility,
            String search, String aggs, String scriptFields, String fields, String timeField)
    {
        m_Compatibility = Objects.requireNonNull(compatibility);
        m_Search = Objects.requireNonNull(search);
        m_Aggregations = aggs;
        m_ScriptFields = scriptFields;
        m_Fields = fields;
        m_TimeField = Objects.requireNonNull(timeField);
    }

    public String createSearchBody(long start, long end)
    {
        return createSearchBody(start, end, m_TimeField, m_Aggregations);
    }

    private String createSearchBody(long start, long end, String sortField, String aggs)
    {
        String template = null;
        switch (m_Compatibility)
        {
            case V_1_7_X:
                template = SEARCH_BODY_TEMPLATE_1_7_X;
                break;
            case V_2_X_X:
                template = SEARCH_BODY_TEMPLATE_2_X;
                break;
            default:
                throw new IllegalStateException();
        }
        return String.format(template, sortField, m_Search, m_TimeField, formatAsDateTime(start),
                formatAsDateTime(end), createResultsFormatSpec(aggs));
    }

    private static String formatAsDateTime(long epochMs)
    {
        Instant instant = Instant.ofEpochMilli(epochMs);
        ZonedDateTime dateTime = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
    }

    private String createResultsFormatSpec(String aggs)
    {
        return (aggs != null) ? createAggregations(aggs) :
                ((m_Fields != null) ? createFieldDataFields() : "");
    }

    private String createAggregations(String aggs)
    {
        return String.format(AGGREGATION_TEMPLATE, aggs);
    }

    private String createFieldDataFields()
    {
        return String.format(FIELDS_TEMPLATE, createScriptFields(), m_Fields);
    }

    private String createScriptFields()
    {
        return (m_ScriptFields != null) ? String.format(SCRIPT_FIELDS_TEMPLATE, m_ScriptFields) : "";
    }

    public String createDataSummaryQuery(long start, long end)
    {
        String aggs = String.format(DATA_SUMMARY_AGGS_TEMPLATE, m_TimeField);
        return createSearchBody(start, end, DATA_SUMMARY_SORT_FIELD, aggs);
    }

    public void logQueryInfo(Logger logger)
    {
        if (m_Aggregations != null)
        {
            logger.debug("Will use the following Elasticsearch aggregations: "
                    + m_Aggregations);
        }
        else
        {
            if (m_Fields != null)
            {
                logger.debug("Will request only the following field(s) from Elasticsearch: "
                        + String.join(" ", m_Fields));
            }
            else
            {
                logger.debug("Will retrieve whole _source document from Elasticsearch");
            }
        }
    }

    public boolean isAggregated()
    {
        return m_Aggregations != null;
    }
}
