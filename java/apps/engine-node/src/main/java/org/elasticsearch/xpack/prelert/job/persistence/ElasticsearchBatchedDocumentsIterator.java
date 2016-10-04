
package org.elasticsearch.xpack.prelert.job.persistence;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.apache.log4j.Logger;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.xpack.prelert.job.persistence.BatchedDocumentsIterator;

abstract class ElasticsearchBatchedDocumentsIterator<T> implements BatchedDocumentsIterator<T>
{
    private static final Logger LOGGER = Logger.getLogger(ElasticsearchBatchedDocumentsIterator.class);

    private static final String CONTEXT_ALIVE_DURATION = "5m";
    private static final int BATCH_SIZE = 10000;

    private final Client m_Client;
    private final String m_Index;
    private final ObjectMapper m_ObjectMapper;
    private final ResultsFilterBuilder m_FilterBuilder;
    private volatile long m_Count;
    private volatile long m_TotalHits;
    private volatile String m_ScrollId;
    private volatile boolean m_IsScrollInitialised;

    public ElasticsearchBatchedDocumentsIterator(Client client, String index, ObjectMapper objectMapper)
    {
        m_Client = Objects.requireNonNull(client);
        m_Index = Objects.requireNonNull(index);
        m_ObjectMapper = Objects.requireNonNull(objectMapper);
        m_TotalHits = 0;
        m_Count = 0;
        m_FilterBuilder = new ResultsFilterBuilder();
        m_IsScrollInitialised = false;
    }

    @Override
    public BatchedDocumentsIterator<T> timeRange(long startEpochMs, long endEpochMs)
    {
        m_FilterBuilder.timeRange(ElasticsearchMappings.ES_TIMESTAMP, startEpochMs, endEpochMs);
        return this;
    }

    @Override
    public BatchedDocumentsIterator<T> includeInterim(String interimFieldName)
    {
        m_FilterBuilder.interim(interimFieldName, true);
        return this;
    }

    @Override
    public boolean hasNext()
    {
        return !m_IsScrollInitialised || m_Count != m_TotalHits;
    }

    @Override
    public Deque<T> next()
    {
        if (!hasNext())
        {
            throw new NoSuchElementException();
        }
        SearchResponse searchResponse = (m_ScrollId == null) ? initScroll() :
                m_Client.prepareSearchScroll(m_ScrollId).setScroll(CONTEXT_ALIVE_DURATION).get();
        m_ScrollId = searchResponse.getScrollId();
        return mapHits(searchResponse);
    }

    private SearchResponse initScroll()
    {
        LOGGER.trace("ES API CALL: search all of type " + getType() + " from index " + m_Index);

        m_IsScrollInitialised = true;

        SearchResponse searchResponse = m_Client.prepareSearch(m_Index)
                .setScroll(CONTEXT_ALIVE_DURATION)
                .setSize(BATCH_SIZE)
                .setTypes(getType())
                .setQuery(m_FilterBuilder.build())
                .addSort(SortBuilders.fieldSort(ElasticsearchMappings.ES_DOC))
                .get();
        m_TotalHits = searchResponse.getHits().getTotalHits();
        m_ScrollId = searchResponse.getScrollId();
        return searchResponse;
    }

    private Deque<T> mapHits(SearchResponse searchResponse)
    {
        Deque<T> results = new ArrayDeque<>();

        SearchHit[] hits = searchResponse.getHits().getHits();
        for (SearchHit hit : hits)
        {
            T mapped = map(m_ObjectMapper, hit);
            if (mapped != null)
            {
                results.add(mapped);
            }
        }
        m_Count += hits.length;

        if (!hasNext() && m_ScrollId != null)
        {
            m_Client.prepareClearScroll().setScrollIds(Arrays.asList(m_ScrollId)).get();
        }
        return results;
    }

    protected abstract String getType();

    /**
     * Maps the search hit to the document type
     *
     * @param objectMapper the object mapper
     * @param hit the search hit
     * @return The mapped document or {@code null} if the mapping failed
     */
    protected abstract T map(ObjectMapper objectMapper, SearchHit hit);
}
