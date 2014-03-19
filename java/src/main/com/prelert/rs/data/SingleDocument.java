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

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Generic wrapper class for returning a single document requested through
 * the REST API. If the requested document does not exist {@link #isExists()}
 * will be false and {@link #getDocument()} will return <code>null</code>.
 *
 * @param <T>
 */
@JsonPropertyOrder({"id", "exists", "type", "document"})
public class SingleDocument<T> 
{
	private boolean m_Exists;
	private String m_Type;
	private String m_Id;
	
	private T m_Document;
	
	/**
	 * Return true if the requested document exists
	 * @return
	 */
	public boolean isExists() 
	{
		return m_Exists;
	}

	public void setExists(boolean exists) 
	{
		this.m_Exists = exists;
	}

	/**
	 * The type of the requested document
	 * @return
	 */
	public String getType() 
	{
		return m_Type;
	}

	public void setType(String type) 
	{
		this.m_Type = type;
	}

	/**
	 * The id of the requested document
	 * @return
	 */
	public String getId() 
	{
		return m_Id;
	}

	public void setId(String id) 
	{
		this.m_Id = id;
	}
	
	/**
	 * Get the requested document or null 
	 * @return The document or <code>null</code>
	 */
	public T getDocument()
	{
		return m_Document;
	}
	
	/**
	 * Set the requested document.</br>
	 * If the doc is non-null {@link #isExists() Exists} is set to true 
	 * else it is false
	 * @param doc
	 */
	public void setDocument(T doc)
	{		
		m_Document = doc;
		m_Exists = (doc != null) ? true : false;
	}
	
}
