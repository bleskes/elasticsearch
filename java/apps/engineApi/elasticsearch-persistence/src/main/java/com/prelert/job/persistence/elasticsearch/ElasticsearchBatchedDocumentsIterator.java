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

package com.prelert.job.persistence.elasticsearch;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.apache.log4j.Logger;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prelert.job.UnknownJobException;
import com.prelert.job.persistence.BatchedDocumentsIterator;

abstract class ElasticsearchBatchedDocumentsIterator<T> implements BatchedDocumentsIterator<T>
{
    private static final Logger LOGGER = Logger.getLogger(ElasticsearchBatchedDocumentsIterator.class);

    private static final String CONTEXT_ALIVE_DURATION = "5m";
    private static final int BATCH_SIZE = 10000;

    private final Client m_Client;
    private final ElasticsearchJobId m_JobId;
    private final ObjectMapper m_ObjectMapper;
    private String m_ScrollId;
    private long m_TotalHits;
    private long m_Count;
    private final ResultsFilterBuilder m_FilterBuilder;

    public ElasticsearchBatchedDocumentsIterator(Client client, String jobId, ObjectMapper objectMapper)
    {
        m_Client = Objects.requireNonNull(client);
        m_JobId = new ElasticsearchJobId(jobId);
        m_ObjectMapper = Objects.requireNonNull(objectMapper);
        m_TotalHits = 0;
        m_Count = 0;
        m_FilterBuilder = new ResultsFilterBuilder();
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
        return m_ScrollId == null || m_Count != m_TotalHits;
    }

    @Override
    public Deque<T> next() throws UnknownJobException
    {
        if (!hasNext())
        {
            throw new NoSuchElementException();
        }
        SearchResponse searchResponse = (m_ScrollId == null) ? initScroll() :
                m_Client.prepareSearchScroll(m_ScrollId).setScroll(CONTEXT_ALIVE_DURATION).get();
        return mapHits(searchResponse);
    }

    private SearchResponse initScroll() throws UnknownJobException
    {
        LOGGER.trace("ES API CALL: search all of type " + getType() + " from index " + m_JobId.getIndex());
        SearchResponse searchResponse = null;
        try
        {
            searchResponse = m_Client.prepareSearch(m_JobId.getIndex())
                    .setScroll(CONTEXT_ALIVE_DURATION)
                    .setSize(BATCH_SIZE)
                    .setTypes(getType())
                    .setQuery(m_FilterBuilder.build())
                    .addSort(SortBuilders.fieldSort(ElasticsearchMappings.ES_DOC))
                    .get();
            m_TotalHits = searchResponse.getHits().getTotalHits();
            m_ScrollId = searchResponse.getScrollId();
        }
        catch (IndexNotFoundException e)
        {
            throw new UnknownJobException(m_JobId.getId());
        }
        return searchResponse;
    }

    private Deque<T> mapHits(SearchResponse searchResponse)
    {
        Deque<T> results = new ArrayDeque<>();

        for (SearchHit hit : searchResponse.getHits().getHits())
        {
            results.add(map(m_ObjectMapper, hit));
        }
        m_Count += results.size();

        if (!hasNext())
        {
            m_Client.prepareClearScroll().setScrollIds(Arrays.asList(m_ScrollId)).get();
        }
        return results;
    }

    protected abstract String getType();
    protected abstract T map(ObjectMapper objectMapper, SearchHit hit);
}
