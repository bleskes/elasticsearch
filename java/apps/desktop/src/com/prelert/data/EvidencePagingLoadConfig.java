/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2009     *
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
 ***********************************************************/

package com.prelert.data;


/**
 * Extension of DatePagingLoadConfig which for paging evidence views, adding
 * support for a filter attribute and value.
 * @author Pete Harverson
 */
public class EvidencePagingLoadConfig extends DatePagingLoadConfig
{
	private String	m_FilterAttribute;
	private String	m_FilterValue;
	
	
	/**
	 * Returns the name of the attribute that the evidence list is filtered on.
     * @return the filter attribute, or <code>null</code> if the view is not filtered.
     */
    public String getFilterAttribute()
    {
    	return m_FilterAttribute;
    }


	/**
	 * Sets the name of the attribute that the evidence list is filtered on.
     * @param filterAttribute the filter attribute, or <code>null</code> 
     * 		if the view is not filtered.
     */
    public void setFilterAttribute(String filterAttribute)
    {
    	m_FilterAttribute = filterAttribute;
    }


	/**
	 * Returns the value of the attribute that the evidence list is filtered on.
     * @return the filter value, or <code>null</code> if the view is not filtered.
     */
    public String getFilterValue()
    {
    	return m_FilterValue;
    }


	/**
	 * Sets the value of the attribute that the evidence list is filtered on.
     * @param filterValue the filter value, or <code>null</code> 
     * 		if the view is not filtered.
     */
    public void setFilterValue(String filterValue)
    {
    	m_FilterValue = filterValue;
    }
}
