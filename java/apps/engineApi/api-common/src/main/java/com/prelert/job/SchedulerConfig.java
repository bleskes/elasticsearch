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

package com.prelert.job;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;


/**
 * Scheduler configuration options.  Describes where to proactively pull input
 * data from.
 * <p>
 * If a value has not been set it will be <code>null</code>.
 * Object wrappers are used around integral types &amp; booleans so they can take
 * <code>null</code> values.
 */
@JsonInclude(Include.NON_NULL)
public class SchedulerConfig
{
    /**
     * Enum of the acceptable data sources.
     */
    public enum DataSource
    {
        FILE, ELASTICSEARCH;

        /**
         * Case-insensitive from string method.
         * Works with either ELASTICSEARCH, Elasticsearch, ElasticSearch, etc.
         *
         * @param value String representation
         * @return The data source
         */
        @JsonCreator
        public static DataSource forString(String value)
        {
            String valueUpperCase = value.toUpperCase();
            return DataSource.valueOf(valueUpperCase);
        }
    }

    /**
     * Serialisation names
     */
    public static final String DATA_SOURCE = "dataSource";
    public static final String PATH = "path";
    public static final String TAIL = "tail";
    public static final String BASE_URL = "baseUrl";
    public static final String INDEXES = "indexes";
    public static final String TYPES = "types";
    public static final String SEARCH = "search";
    public static final String START_TIME = "startTime";
    public static final String END_TIME = "endTime";

    private DataSource m_DataSource;

    /**
     * These values apply to the FILE data source
     */
    private String m_Path;
    private Boolean m_Tail;

    /**
     * These values apply to the ELASTICSEARCH data source
     */
    private String m_BaseUrl;
    private List<String> m_Indexes;
    private List<String> m_Types;
    private Map<String, Object> m_Search;
    private Date m_StartTime;
    private Date m_EndTime;

    /**
     * Default constructor
     */
    public SchedulerConfig()
    {
        m_DataSource = DataSource.FILE;
    }

    /**
     * The data source that the sceduler is to pull data from.
     * @return The data source.
     */
    public DataSource getDataSource()
    {
        return m_DataSource;
    }

    public void setDataSource(DataSource dataSource)
    {
        m_DataSource = dataSource;
    }

    /**
     * For the FILE data source only, the path to the file.
     * @return The path to the file, or <code>null</code> if not set.
     */
    public String getPath()
    {
        return m_Path;
    }

    public void setPath(String path)
    {
        m_Path = path;
    }

    /**
     * For the FILE data source only, should the file be tailed?  If not it will
     * just be read from once.
     * @return Should the file be tailed?  (<code>null</code> if not set.)
     */
    public Boolean getTail()
    {
        return m_Tail;
    }

    public void setTail(Boolean tail)
    {
        m_Tail = tail;
    }

    /**
     * For the ELASTICSEARCH data source only, the base URL to connect to
     * Elasticsearch on.
     * @return The URL, or <code>null</code> if not set.
     */
    public String getBaseUrl()
    {
        return m_BaseUrl;
    }

    public void setBaseUrl(String baseUrl)
    {
        m_BaseUrl = baseUrl;
    }

    /**
     * For the ELASTICSEARCH data source only, one or more indexes to search for
     * input data.
     * @return The indexes to search, or <code>null</code> if not set.
     */
    public List<String> getIndexes()
    {
        return m_Indexes;
    }

    public void setIndexes(List<String> indexes)
    {
        m_Indexes = indexes;
    }

    /**
     * For the ELASTICSEARCH data source only, one or more types to search for
     * input data.
     * @return The types to search, or <code>null</code> if not set.
     */
    public List<String> getTypes()
    {
        return m_Types;
    }

    public void setTypes(List<String> types)
    {
        m_Types = types;
    }

    /**
     * For the ELASTICSEARCH data source only, the Elasticsearch query DSL
     * representing the search to submit to Elasticsearch to get the input data.
     * This should not include time bounds, as these are added separately.
     * This class does not attempt to interpret the search.  The map will be
     * converted back to an arbitrary JSON object.
     * @return The search query, or <code>null</code> if not set.
     */
    public Map<String, Object> getSearch()
    {
        return m_Search;
    }

    public void setSearch(Map<String, Object> search)
    {
        m_Search = search;
    }

    /**
     * For the ELASTICSEARCH data source only, the time from which to pull input
     * data.  <code>null</code> means no earliest time.
     * @return The earliest time to search, or <code>null</code> if not set.
     */
    public Date getStartTime()
    {
        return m_StartTime;
    }

    public void setStartTime(Date startTime)
    {
        m_StartTime = startTime;
    }

    /**
     * For the ELASTICSEARCH data source only, the most recent time to which to
     * pull input data.  <code>null</code> means no latest time, which in turn
     * means the scheduler will periodically pull data on an ongoing basis.
     * @return The latest time to search, or <code>null</code> if not set.
     */
    public Date getEndTime()
    {
        return m_EndTime;
    }

    public void setEndTime(Date endTime)
    {
        m_EndTime = endTime;
    }

    /**
     * The lists of indexes and types are compared for equality but they are not
     * sorted first so this test could fail simply because the indexes and types
     * lists are in different orders.
     */
    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (other instanceof SchedulerConfig == false)
        {
            return false;
        }

        SchedulerConfig that = (SchedulerConfig)other;

        return Objects.equals(this.m_DataSource, that.m_DataSource) &&
                Objects.equals(this.m_Path, that.m_Path) &&
                Objects.equals(this.m_Tail, that.m_Tail) &&
                Objects.equals(this.m_BaseUrl, that.m_BaseUrl) &&
                Objects.equals(this.m_Indexes, that.m_Indexes) &&
                Objects.equals(this.m_Types, that.m_Types) &&
                Objects.equals(this.m_Search, that.m_Search) &&
                Objects.equals(this.m_StartTime, that.m_StartTime) &&
                Objects.equals(this.m_EndTime, that.m_EndTime);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(m_DataSource, m_Path, m_Tail, m_BaseUrl, m_Indexes,
                m_Types, m_Search, m_StartTime, m_EndTime);
    }
}
