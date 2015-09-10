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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Generic wrapper class for returning a single document requested through
 * the REST API. If the requested document does not exist {@link #isExists()}
 * will be false and {@link #getDocument()} will return <code>null</code>.
 *
 * @param <T> the requested document type
 */
@JsonPropertyOrder({"documentId", "exists", "type", "document"})
@JsonInclude(Include.NON_NULL)
public class SingleDocument<T>
{
    private boolean m_Exists;
    private String m_Type;
    private String m_Id;

    private T m_Document;

    /**
     * Return true if the requested document exists
     * @return true is document exists
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
     * @return The document type
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
     * @return The document Id
     */
    public String getDocumentId()
    {
        return m_Id;
    }

    public void setDocumentId(String id)
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
     * Set the requested document.
     * If the doc is non-null {@link #isExists() Exists} is set to true
     * else it is false
     * @param doc the requested document
     */
    public void setDocument(T doc)
    {
        m_Document = doc;
        m_Exists = (doc != null) ? true : false;
    }
}
