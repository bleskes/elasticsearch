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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;

import org.elasticsearch.action.search.ClearScrollRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prelert.job.UnknownJobException;

public class ElasticsearchBatchedDocumentsIteratorTest
{
    private static final String INDEX_NAME = "prelertresults-foo";
    private static final String SCROLL_ID = "someScrollId";

    @Mock private Client m_Client;
    @Mock private ObjectMapper m_ObjectMapper;
    private boolean m_WasScrollCleared;

    private TestIterator m_TestIterator;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        m_WasScrollCleared = false;
        m_TestIterator = new TestIterator(m_Client, INDEX_NAME, m_ObjectMapper);
        givenClearScrollRequest();
    }

    @Test
    public void testQueryReturnsNoResults() throws UnknownJobException
    {
        new ScrollResponsesMocker().finishMock();

        assertTrue(m_TestIterator.hasNext());
        assertTrue(m_TestIterator.next().isEmpty());
        assertFalse(m_TestIterator.hasNext());
        assertTrue(m_WasScrollCleared);
    }

    @Test (expected = NoSuchElementException.class)
    public void testCallingNextWhenHasNextIsFalseThrows() throws UnknownJobException
    {
        new ScrollResponsesMocker().addBatch("a", "b", "c").finishMock();
        m_TestIterator.next();
        assertFalse(m_TestIterator.hasNext());

        m_TestIterator.next();
    }

    @Test
    public void testQueryReturnsSingleBatch() throws UnknownJobException
    {
        new ScrollResponsesMocker().addBatch("a", "b", "c").finishMock();

        assertTrue(m_TestIterator.hasNext());
        Deque<String> batch = m_TestIterator.next();
        assertEquals(3, batch.size());
        assertTrue(batch.containsAll(Arrays.asList("a", "b", "c")));
        assertFalse(m_TestIterator.hasNext());
        assertTrue(m_WasScrollCleared);
    }

    @Test
    public void testQueryReturnsThreeBatches() throws UnknownJobException
    {
        new ScrollResponsesMocker()
                .addBatch("a", "b", "c")
                .addBatch("d", "e")
                .addBatch("f")
                .finishMock();

        assertTrue(m_TestIterator.hasNext());

        Deque<String> batch = m_TestIterator.next();
        assertEquals(3, batch.size());
        assertTrue(batch.containsAll(Arrays.asList("a", "b", "c")));

        batch = m_TestIterator.next();
        assertEquals(2, batch.size());
        assertTrue(batch.containsAll(Arrays.asList("d", "e")));

        batch = m_TestIterator.next();
        assertEquals(1, batch.size());
        assertTrue(batch.containsAll(Arrays.asList("f")));

        assertFalse(m_TestIterator.hasNext());
        assertTrue(m_WasScrollCleared);
    }

    private void givenClearScrollRequest()
    {
        ClearScrollRequestBuilder requestBuilder = mock(ClearScrollRequestBuilder.class);
        when(m_Client.prepareClearScroll()).thenReturn(requestBuilder);
        when(requestBuilder.setScrollIds(Arrays.asList(SCROLL_ID))).thenReturn(requestBuilder);
        when(requestBuilder.get()).thenAnswer((invocation) -> {m_WasScrollCleared = true; return null;});
    }

    private class ScrollResponsesMocker
    {
        private List<String[]> m_Batches = new ArrayList<>();
        private long m_TotalHits = 0;
        private List<SearchScrollRequestBuilder> m_NextRequestBuilders = new ArrayList<>();

        ScrollResponsesMocker addBatch(String... hits)
        {
            m_TotalHits += hits.length;
            m_Batches.add(hits);
            return this;
        }

        void finishMock()
        {
            if (m_Batches.isEmpty())
            {
                givenInitialResponse();
                return;
            }
            givenInitialResponse(m_Batches.get(0));
            for (int i = 1; i < m_Batches.size(); ++i)
            {
                givenNextResponse(m_Batches.get(i));
            }
            if (m_NextRequestBuilders.size() > 0)
            {
                SearchScrollRequestBuilder first = m_NextRequestBuilders.get(0);
                if (m_NextRequestBuilders.size() > 1)
                {
                    SearchScrollRequestBuilder[] rest = new SearchScrollRequestBuilder[m_Batches.size() - 1];
                    for (int i = 1; i < m_NextRequestBuilders.size(); ++i)
                    {
                        rest[i - 1] = m_NextRequestBuilders.get(i);
                    }
                    when(m_Client.prepareSearchScroll(SCROLL_ID)).thenReturn(first, rest);
                }
                else
                {
                    when(m_Client.prepareSearchScroll(SCROLL_ID)).thenReturn(first);
                }
            }
        }

        private void givenInitialResponse(String... hits)
        {
            SearchResponse searchResponse = createSearchResponseWithHits(hits);
            SearchRequestBuilder requestBuilder = mock(SearchRequestBuilder.class);
            when(m_Client.prepareSearch(INDEX_NAME)).thenReturn(requestBuilder);
            when(requestBuilder.setScroll("5m")).thenReturn(requestBuilder);
            when(requestBuilder.setSize(10000)).thenReturn(requestBuilder);
            when(requestBuilder.setTypes("String")).thenReturn(requestBuilder);
            when(requestBuilder.setQuery(any(QueryBuilder.class))).thenReturn(requestBuilder);
            when(requestBuilder.addSort(any(SortBuilder.class))).thenReturn(requestBuilder);
            when(requestBuilder.get()).thenReturn(searchResponse);
        }

        private void givenNextResponse(String... hits)
        {
            SearchResponse searchResponse = createSearchResponseWithHits(hits);
            SearchScrollRequestBuilder requestBuilder = mock(SearchScrollRequestBuilder.class);
            when(requestBuilder.setScrollId(SCROLL_ID)).thenReturn(requestBuilder);
            when(requestBuilder.setScroll("5m")).thenReturn(requestBuilder);
            when(requestBuilder.get()).thenReturn(searchResponse);
            m_NextRequestBuilders.add(requestBuilder);
        }

        private SearchResponse createSearchResponseWithHits(String... hits)
        {
            SearchHits searchHits = createHits(hits);
            SearchResponse searchResponse = mock(SearchResponse.class);
            when(searchResponse.getScrollId()).thenReturn(SCROLL_ID);
            when(searchResponse.getHits()).thenReturn(searchHits);
            return searchResponse;
        }

        private SearchHits createHits(String... values)
        {
            SearchHits searchHits = mock(SearchHits.class);
            List<SearchHit> hits = new ArrayList<>();
            for (String value : values)
            {
                SearchHit hit = mock(SearchHit.class);
                when(hit.getSourceAsString()).thenReturn(value);
                hits.add(hit);
            }
            when(searchHits.getTotalHits()).thenReturn(m_TotalHits);
            when(searchHits.getHits()).thenReturn(hits.toArray(new SearchHit[hits.size()]));
            return searchHits;
        }
    }

    private static class TestIterator extends ElasticsearchBatchedDocumentsIterator<String>
    {
        public TestIterator(Client client, String jobId, ObjectMapper objectMapper)
        {
            super(client, jobId, objectMapper);
        }

        @Override
        protected String getType()
        {
            return "String";
        }

        @Override
        protected String map(ObjectMapper objectMapper, SearchHit hit)
        {
            return hit.getSourceAsString();
        }
    }

}
