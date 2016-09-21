
package org.elasticsearch.xpack.prelert.job;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;


/**
 * Scheduler configuration options.  Describes where to proactively pull input
 * data from.
 * <p>
 * If a value has not been set it will be <code>null</code>.
 * Object wrappers are used around integral types and booleans so they can take
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
    public static final String DATA_SOURCE_COMPATIBILITY = "dataSourceCompatibility";
    public static final String QUERY_DELAY = "queryDelay";
    public static final String FREQUENCY = "frequency";
    public static final String FILE_PATH = "filePath";
    public static final String TAIL_FILE = "tailFile";
    public static final String BASE_URL = "baseUrl";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String ENCRYPTED_PASSWORD = "encryptedPassword";
    public static final String INDEXES = "indexes";
    public static final String TYPES = "types";
    public static final String QUERY = "query";
    public static final String RETRIEVE_WHOLE_SOURCE = "retrieveWholeSource";
    public static final String SCROLL_SIZE = "scrollSize";
    public static final String AGGREGATIONS = "aggregations";
    public static final String AGGS = "aggs";

    /**
     * Named to match Elasticsearch, hence lowercase_with_underscores instead
     * of camelCase
     */
    public static final String SCRIPT_FIELDS = "script_fields";

    private static final int DEFAULT_SCROLL_SIZE = 1000;
    private static final long DEFAULT_ELASTICSEARCH_QUERY_DELAY = 60L;

    private DataSource dataSource;
    private String dataSourceCompatibility;

    /**
     * The delay in seconds before starting to query a period of time
     */
    private Long queryDelay;

    /**
     * The frequency in seconds with which queries are executed
     */
    private Long frequency;

    /**
     * These values apply to the FILE data source
     */
    private String filePath;
    private Boolean tailFile;

    /**
     * Used for data sources that require credentials.  May be null in the case
     * where credentials are sometimes needed and sometimes not (e.g. Elasticsearch).
     */
    private String username;
    private String password;
    private String encryptedPassword;

    /**
     * These values apply to the ELASTICSEARCH data source
     */
    private String baseUrl;
    private List<String> indexes;
    private List<String> types;
    private Map<String, Object> query;
    private Map<String, Object> aggregations;
    private Map<String, Object> aggs;
    private Map<String, Object> scriptFields;
    private Boolean retrieveWholeSource;
    private Integer scrollSize;

    /**
     * Default constructor
     */
    public SchedulerConfig()
    {
        this.dataSource = DataSource.FILE;
    }

    /**
     * The data source that the scheduler is to pull data from.
     * @return The data source.
     */
    public DataSource getDataSource()
    {
        return this.dataSource;
    }

    public void setDataSource(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    /**
     * Gets the compatibility for the data source.
     * @return The data source compatibility.
     */
    public String getDataSourceCompatibility()
    {
        return this.dataSourceCompatibility;
    }

    public void setDataSourceCompatibility(String dataSourceCompatibility)
    {
        this.dataSourceCompatibility = dataSourceCompatibility;
    }

    public Long getQueryDelay()
    {
        return this.queryDelay;
    }

    public void setQueryDelay(Long delay)
    {
        this.queryDelay = delay;
    }

    public Long getFrequency()
    {
        return this.frequency;
    }

    public void setFrequency(Long frequency)
    {
        this.frequency = frequency;
    }

    /**
     * For the FILE data source only, the path to the file.
     * @return The path to the file, or <code>null</code> if not set.
     */
    public String getFilePath()
    {
        return this.filePath;
    }

    public void setFilePath(String filePath)
    {
        this.filePath = filePath;
    }

    /**
     * For the FILE data source only, should the file be tailed?  If not it will
     * just be read from once.
     * @return Should the file be tailed?  (<code>null</code> if not set.)
     */
    public Boolean getTailFile()
    {
        return this.tailFile;
    }

    public void setTailFile(Boolean tailFile)
    {
        this.tailFile = tailFile;
    }

    /**
     * For the ELASTICSEARCH data source only, the base URL to connect to
     * Elasticsearch on.
     * @return The URL, or <code>null</code> if not set.
     */
    public String getBaseUrl()
    {
        return this.baseUrl;
    }

    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }

    /**
     * The username to use to connect to the data source (if any).
     * @return The username, or <code>null</code> if not set.
     */
    public String getUsername()
    {
        return this.username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    /**
     * For the ELASTICSEARCH data source only, one or more indexes to search for
     * input data.
     * @return The indexes to search, or <code>null</code> if not set.
     */
    public List<String> getIndexes()
    {
        return this.indexes;
    }

    public void setIndexes(List<String> indexes)
    {
        this.indexes = indexes;
    }

    /**
     * For the ELASTICSEARCH data source only, one or more types to search for
     * input data.
     * @return The types to search, or <code>null</code> if not set.
     */
    public List<String> getTypes()
    {
        return this.types;
    }

    public void setTypes(List<String> types)
    {
        this.types = types;
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
        return this.query;
    }

    public void setQuery(Map<String, Object> query)
    {
        this.query = query;
    }

    /**
     * For the ELASTICSEARCH data source only, should the whole _source document
     * be retrieved for analysis, or just the analysis fields?
     * @return Should the whole of _source be retrieved?  (<code>null</code> if not set.)
     */
    public Boolean getRetrieveWholeSource()
    {
        return this.retrieveWholeSource;
    }

    public void setRetrieveWholeSource(Boolean retrieveWholeSource)
    {
        this.retrieveWholeSource = retrieveWholeSource;
    }

    /**
     * For the ELASTICSEARCH data source only, get the size of documents to
     * be retrieved from each shard via a scroll search
     * @return The size of documents to be retrieved from each shard via a scroll search
     */
    public Integer getScrollSize()
    {
        return this.scrollSize;
    }

    public void setScrollSize(Integer scrollSize)
    {
        this.scrollSize = scrollSize;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public String getPassword() {
        return password;
    }

    /**
     * For the ELASTICSEARCH data source only, optional Elasticsearch
     * script_fields to add to the search to be submitted to Elasticsearch to
     * get the input data.  This class does not attempt to interpret the
     * script fields.  The map will be converted back to an arbitrary JSON object.
     * @return The script fields, or <code>null</code> if not set.
     */
    @JsonProperty("script_fields")
    public Map<String, Object> getScriptFields()
    {
        return this.scriptFields;
    }

    @JsonProperty("script_fields")
    public void setScriptFields(Map<String, Object> scriptFields)
    {
        this.scriptFields = scriptFields;
    }

    /**
     * For the ELASTICSEARCH data source only, optional Elasticsearch
     * aggregations to apply to the search to be submitted to Elasticsearch to
     * get the input data.  This class does not attempt to interpret the
     * aggregations.  The map will be converted back to an arbitrary JSON object.
     * Synonym for {@link #getAggs()} (like Elasticsearch).
     * @return The aggregations, or <code>null</code> if not set.
     */
    public Map<String, Object> getAggregations()
    {
        return this.aggregations;
    }

    public void setAggregations(Map<String, Object> aggregations)
    {
        // It's only expected that one of aggregations or aggs will be set,
        // having two member variables makes it easier to remember which the
        // user used so their input can be recreated
        this.aggregations = aggregations;
    }

    /**
     * For the ELASTICSEARCH data source only, optional Elasticsearch
     * aggregations to apply to the search to be submitted to Elasticsearch to
     * get the input data.  This class does not attempt to interpret the
     * aggregations.  The map will be converted back to an arbitrary JSON object.
     * Synonym for {@link #getAggregations()} (like Elasticsearch).
     * @return The aggregations, or <code>null</code> if not set.
     */
    public Map<String, Object> getAggs()
    {
        return this.aggs;
    }

    public void setAggs(Map<String, Object> aggs)
    {
        // It's only expected that one of aggregations or aggs will be set,
        // having two member variables makes it easier to remember which the
        // user used so their input can be recreated
        this.aggs = aggs;
    }

    /**
     * Convenience method to get either aggregations or aggs.
     * @return The aggregations (whether initially specified in aggregations
     * or aggs), or <code>null</code> if neither are set.
     */
    @JsonIgnore
    public Map<String, Object> getAggregationsOrAggs()
    {
        return (this.aggregations != null) ? this.aggregations : this.aggs;
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
            else if (value instanceof String && FIELD.equals(entry.getKey()))
            {
                orderedFields.put(depth, (String)value);
            }
        }
    }

    public void fillDefaults()
    {
        switch (this.dataSource)
        {
            case ELASTICSEARCH:
                fillElasticsearchDefaults();
                break;
            case FILE:
                fillFileDefaults();
                break;
            default:
                throw new IllegalStateException();
        }
    }

    private void fillElasticsearchDefaults()
    {
        if (this.query == null)
        {
            this.query = new HashMap<>();
            this.query.put(MATCH_ALL_ES_QUERY, new HashMap<String, Object>());
        }
        if (this.queryDelay == null)
        {
            this.queryDelay = DEFAULT_ELASTICSEARCH_QUERY_DELAY;
        }
        if (this.retrieveWholeSource == null)
        {
            this.retrieveWholeSource = false;
        }
        if (this.scrollSize == null)
        {
            this.scrollSize = DEFAULT_SCROLL_SIZE;
        }
    }

    private void fillFileDefaults()
    {
        if (this.tailFile == null)
        {
            this.tailFile = false;
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

        return Objects.equals(this.dataSource, that.dataSource) &&
                Objects.equals(this.dataSourceCompatibility, that.dataSourceCompatibility) &&
                Objects.equals(this.frequency, that.frequency) &&
                Objects.equals(this.queryDelay, that.queryDelay) &&
                Objects.equals(this.filePath, that.filePath) &&
                Objects.equals(this.tailFile, that.tailFile) &&
                Objects.equals(this.baseUrl, that.baseUrl) &&
                Objects.equals(this.username, that.username) &&
                Objects.equals(this.password, that.password) &&
                Objects.equals(this.encryptedPassword, that.encryptedPassword) &&
                Objects.equals(this.indexes, that.indexes) &&
                Objects.equals(this.types, that.types) &&
                Objects.equals(this.query, that.query) &&
                Objects.equals(this.retrieveWholeSource, that.retrieveWholeSource) &&
                Objects.equals(this.scrollSize, that.scrollSize) &&
                Objects.equals(this.getAggregationsOrAggs(), that.getAggregationsOrAggs()) &&
                Objects.equals(this.scriptFields, that.scriptFields);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.dataSource, dataSourceCompatibility, frequency, queryDelay,
                this.filePath, tailFile, baseUrl, username, password, encryptedPassword,
                this.indexes, types, query, retrieveWholeSource, scrollSize,
                getAggregationsOrAggs(), this.scriptFields);
    }
}
