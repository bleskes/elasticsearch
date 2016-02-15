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

package com.prelert.job;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
         * Works with ELASTICSEARCH, Elasticsearch, ElasticSearch, etc.
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
     * The field name used to specify aggregation fields in Elasticsearch aggregations
     */
    private static final String FIELD = "field";

    /**
     * The field name used to specify document counts in Elasticsearch aggregations
     */
    public static final String DOC_COUNT = "doc_count";

    /**
     * The default query for elasticsearch searches
     */
    private static final String MATCH_ALL_ES_QUERY = "match_all";

    /**
     * Serialisation names
     */
    public static final String DATA_SOURCE = "dataSource";
    public static final String QUERY_DELAY = "queryDelay";
    public static final String FREQUENCY = "frequency";
    public static final String PATH = "path";
    public static final String TAIL = "tail";
    public static final String BASE_URL = "baseUrl";
    public static final String INDEXES = "indexes";
    public static final String TYPES = "types";
    public static final String QUERY = "query";
    public static final String AGGREGATIONS = "aggregations";
    public static final String AGGS = "aggs";
    public static final String START_TIME = "startTime";
    public static final String END_TIME = "endTime";

    private static final long DEFAULT_ELASTICSEARCH_QUERY_DELAY = 60L;

    private DataSource m_DataSource;

    /**
     * The delay in seconds before starting to query a period of time
     */
    private Long m_QueryDelay;

    /**
     * The frequency in seconds with which queries are executed
     */
    private Long m_Frequency;

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
    private Map<String, Object> m_Query;
    private Map<String, Object> m_Aggregations;
    private Map<String, Object> m_Aggs;
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

    public Long getQueryDelay()
    {
        return m_QueryDelay;
    }

    public void setQueryDelay(Long delay)
    {
        m_QueryDelay = delay;
    }

    public Long getFrequency()
    {
        return m_Frequency;
    }

    public void setFrequency(Long frequency)
    {
        m_Frequency = frequency;
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
     * representing the query to submit to Elasticsearch to get the input data.
     * This should not include time bounds, as these are added separately.
     * This class does not attempt to interpret the query.  The map will be
     * converted back to an arbitrary JSON object.
     * @return The search query, or <code>null</code> if not set.
     */
    public Map<String, Object> getQuery()
    {
        return m_Query;
    }

    public void setQuery(Map<String, Object> query)
    {
        m_Query = query;
    }

    /**
     * For the ELASTICSEARCH data source only, optional Elasticsearch
     * aggregations to apply to the search to be submitted to Elasticsearch to
     * get the input data.  This class does not attempt to interpret the
     * aggregations.  The map will be converted back to an arbitrary JSON object.
     * Synonym for {@link getAggs()} (like Elasticsearch).
     * @return The aggregations, or <code>null</code> if not set.
     */
    public Map<String, Object> getAggregations()
    {
        return m_Aggregations;
    }

    public void setAggregations(Map<String, Object> aggregations)
    {
        // It's only expected that one of aggregations or aggs will be set,
        // having two member variables makes it easier to remember which the
        // user used so their input can be recreated
        m_Aggregations = aggregations;
    }

    /**
     * For the ELASTICSEARCH data source only, optional Elasticsearch
     * aggregations to apply to the search to be submitted to Elasticsearch to
     * get the input data.  This class does not attempt to interpret the
     * aggregations.  The map will be converted back to an arbitrary JSON object.
     * Synonym for {@link getAggregations()} (like Elasticsearch).
     * @return The aggregations, or <code>null</code> if not set.
     */
    public Map<String, Object> getAggs()
    {
        return m_Aggs;
    }

    public void setAggs(Map<String, Object> aggs)
    {
        // It's only expected that one of aggregations or aggs will be set,
        // having two member variables makes it easier to remember which the
        // user used so their input can be recreated
        m_Aggs = aggs;
    }

    /**
     * Convenience method to get either aggregations or aggs.
     * @return The aggregations (whether initially specified in aggregations
     * or aggs), or <code>null</code> if neither are set.
     */
    @JsonIgnore
    public Map<String, Object> getAggregationsOrAggs()
    {
        return (m_Aggregations != null) ? m_Aggregations : m_Aggs;
    }

    /**
     * Build the list of fields expected in the output from aggregations
     * submitted to Elasticsearch.
     * @return The list of fields, or empty list if there are no aggregations.
     */
    public List<String> buildAggregatedFieldList()
    {
        Map<String, Object> aggs = getAggregationsOrAggs();
        if (aggs == null)
        {
            return Collections.emptyList();
        }

        SortedMap<Integer, String> orderedFields = new TreeMap<>();

        scanSubLevel(aggs, 0, orderedFields);

        return new ArrayList<>(orderedFields.values());
    }

    @SuppressWarnings("unchecked")
    private void scanSubLevel(Map<String, Object> subLevel, int depth,
            SortedMap<Integer, String> orderedFields)
    {
        for (Map.Entry<String, Object> entry : subLevel.entrySet())
        {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?>)
            {
                scanSubLevel((Map<String, Object>)value, depth + 1, orderedFields);
            }
            else if (value instanceof String)
            {
                if (FIELD.equals(entry.getKey()))
                {
                    orderedFields.put(depth, (String)value);
                }
            }
        }
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

    public void fillDefaults()
    {
        switch (m_DataSource)
        {
            case ELASTICSEARCH:
                fillElasticsearchDefaults();
                break;
            case FILE:
                break;
            default:
                throw new IllegalStateException();
        }
    }

    private void fillElasticsearchDefaults()
    {
        if (m_Query == null)
        {
            m_Query = new HashMap<>();
            m_Query.put(MATCH_ALL_ES_QUERY, new HashMap<String, Object>());
        }
        if (m_QueryDelay == null)
        {
            m_QueryDelay = DEFAULT_ELASTICSEARCH_QUERY_DELAY;
        }
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
                Objects.equals(this.m_Frequency, that.m_Frequency) &&
                Objects.equals(this.m_QueryDelay, that.m_QueryDelay) &&
                Objects.equals(this.m_Path, that.m_Path) &&
                Objects.equals(this.m_Tail, that.m_Tail) &&
                Objects.equals(this.m_BaseUrl, that.m_BaseUrl) &&
                Objects.equals(this.m_Indexes, that.m_Indexes) &&
                Objects.equals(this.m_Types, that.m_Types) &&
                Objects.equals(this.m_Query, that.m_Query) &&
                Objects.equals(this.getAggregationsOrAggs(), that.getAggregationsOrAggs()) &&
                Objects.equals(this.m_StartTime, that.m_StartTime) &&
                Objects.equals(this.m_EndTime, that.m_EndTime);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(m_DataSource, m_Frequency, m_QueryDelay, m_Path, m_Tail, m_BaseUrl,
                m_Indexes, m_Types, m_Query, m_Aggregations, m_Aggs, m_StartTime, m_EndTime);
    }
}
