/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Inc 2006-2014     *
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
 * on +44 (0)20 7953 7243 or email to legal@prelert.com.    *
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
 * Generic wrapper class for results returned by the Web Service.
 * 
 * The service automatically pages results if more than 
 * {@link com.prelert.job.JobManager#DEFAULT_PAGE_SIZE}
 * are returned in this case {@link #getNextPage()} will not by <code>null</code> 
 * and the link is to the next page of results. Similarly if this is not the 
 * first page of results {@link #getPreviousPage()}. Skip and Take are the
 * arguments used in the query. 
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
	
	private boolean m_GotAllResults;
	
	private List<T> m_Documents;
	
	/**
	 * The number of hits in the request. This does not
	 * necessarily match the length of the number of documents returned 
	 * if paging is in action.
	 * @return
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
	 * @return
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
	 * @return
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
	 * @return may be <code>null</code>
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
	 * @return
	 */
	public int getDocumentCount()
	{
		return (m_Documents == null) ? 0 : m_Documents.size();
	}
	
	/**
	 * True if all results are return i.e. there is no next page or previous page
	 * and skip == 0
	 * 
	 * @return
	 */
	@JsonIgnore()
	public boolean isAllResults()
	{
		return m_GotAllResults;
	}
	
	public void setAllResults(boolean value)
	{
		m_GotAllResults = value;
	}
}
