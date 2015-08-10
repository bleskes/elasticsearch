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

package com.prelert.rs.data;

import java.net.URI;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Generic wrapper class for results returned by the RESTful API.
 *
 * The API automatically pages results if more than the <code>skip</code>
 * query parameter are returned. If this is not the last page of results
 * {@link #getNextPage()} will return a non <code>null</code> value
 * that is the link to the next page of results. Similarly if this is not the
 * first page of results {@link #getPreviousPage()} will return a non
 * <code>null</code> value. {@link #getDocuments()} Returns the actual list
 * of requested documents the size of that list will always be <= {@link #getTake()}
 * <br>
 * Skip and Take are set to the argument values used in the query.
 *
 * @param <T> The type of the result
 */
@JsonPropertyOrder({"hitCount", "skip", "take", "nextPage", "previousPage", "documents"})
@JsonIgnoreProperties({"documentCount"})
public class Pagination<T>
{
    private long m_HitCount;

    private int m_Skip;
    private int m_Take;

    private URI m_NextPage;
    private URI m_PreviousPage;

    private List<T> m_Documents;

    /**
     * The number of hits in the request. This does not
     * necessarily match the length of the number of documents returned
     * if paging is in action.
     * @return The number of search hits
     */
    public long getHitCount()
    {
        return m_HitCount;
    }

    public void setHitCount(long hitCount)
    {
        m_HitCount = hitCount;
    }

    /**
     * If the results are paged this is the starting point of that page.
     *
     * @return The skip query parameter used in the query
     */
    public int getSkip()
    {
        return m_Skip;
    }

    public void setSkip(int skip)
    {
        m_Skip = skip;
    }

    /**
     * The number of documents requested this value can be greater than
     * the number actually returned.
     *
     * @return The take query parameter used in the query
     */
    public int getTake()
    {
        return m_Take;
    }

    public void setTake(int take)
    {
        m_Take = take;
    }

    /**
     * If there is another page of results then this URI points to that page.
     * @return The next page or <code>null</code>
     */
    public URI getNextPage()
    {
        return m_NextPage;
    }

    public void setNextPage(URI nextPage)
    {
        m_NextPage = nextPage;
    }

    /**
     * If there is a previous page of results then this URI points to that page.
     * @return The previous page or <code>null</code>
     */
    public URI getPreviousPage()
    {
        return m_PreviousPage;
    }

    public void setPreviousPage(URI previousPage)
    {
        m_PreviousPage = previousPage;
    }

    /**
     * The documents.
     * @return The list of documents or <code>null</code> if
     * {@link #getHitCount()} == 0
     */
    public List<T> getDocuments()
    {
        return m_Documents;
    }

    public void setDocuments(List<T> documents)
    {
        m_Documents = documents;
    }

    /**
     * The number of documents returned in this document page.
     * This property calculated on the fly so shouldn't be serialised
     * @return The number of documents in this page
     */
    public int getDocumentCount()
    {
        return (m_Documents == null) ? 0 : m_Documents.size();
    }

    /**
     * True if all results are return i.e. <code>documents.size() == hitcount</code>
     * and there is no next page or previous page
     * If the object contains 0 documents it returns false.
     *
     * @return True if all the requested documents are returned in this page
     */
    @JsonIgnore()
    public boolean isAllResults()
    {
        return (m_Documents == null) ? false : m_Documents.size() == m_HitCount;
    }
}
