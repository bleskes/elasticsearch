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

package demo.app.data;


/**
 * Extension of DatePagingLoadConfig which for paging evidence views, adding
 * support for a source name, filter attribute and value.
 * @author Pete Harverson
 */
public class EvidencePagingLoadConfig extends DatePagingLoadConfig
{

	
	/**
	 * Returns the name of the source (server) for the evidence data.
	 * @return the name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 */
	public String getSource()
	{
		return get("source");
	}
	
	
	/**
	 * Sets the name of the source (server) for the evidence data.
	 * @param source the name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 */
	public void setSource(String source)
	{
		set("source", source);
	}
	
	
	/**
	 * Returns the name of the attribute that the evidence list is filtered on.
     * @return the filter attribute, or <code>null</code> if the view is not filtered.
     */
    public String getFilterAttribute()
    {
    	return get("filterAttribute");
    }


	/**
	 * Sets the name of the attribute that the evidence list is filtered on.
     * @param filterAttribute the filter attribute, or <code>null</code> 
     * 		if the view is not filtered.
     */
    public void setFilterAttribute(String filterAttribute)
    {
    	set("filterAttribute", filterAttribute);
    }


	/**
	 * Returns the value of the attribute that the evidence list is filtered on.
     * @return the filter value, or <code>null</code> if the view is not filtered.
     */
    public String getFilterValue()
    {
    	return get("filterValue");
    }


	/**
	 * Sets the value of the attribute that the evidence list is filtered on.
     * @param filterValue the filter value, or <code>null</code> 
     * 		if the view is not filtered.
     */
    public void setFilterValue(String filterValue)
    {
    	set("filterValue", filterValue);
    }

	
}
