/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2010     *
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

package com.prelert.data.gxt;

import java.io.Serializable;
import java.util.List;


/**
 * Extension of DatePagingLoadConfig for paging evidence views, adding
 * support for a source name, filter attributes and values.
 * @author Pete Harverson
 */
public class EvidencePagingLoadConfig extends DatePagingLoadConfig implements Serializable
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
	 * Returns the list of attribute names that the evidence list is filtered on.
     * @return the list of attribute names, or <code>null</code> if the view is not filtered.
     */
    public List<String> getFilterAttributes()
    {
    	return get("filterAttribute");
    }


	/**
	 * Sets the list of attribute names that the evidence list is filtered on.
     * @param filterAttributes the list of attribute names, or <code>null</code> 
     * 		if the view is not filtered.
     */
    public void setFilterAttributes(List<String> filterAttributes)
    {
    	set("filterAttribute", filterAttributes);
    }


	/**
	 * Returns the list of attribute values that the evidence list is filtered on.
     * @return the list of attribute values, or <code>null</code> if the view is not filtered.
     */
    public List<String> getFilterValues()
    {
    	return get("filterValue");
    }


	/**
	 * Sets the list of attribute values that the evidence list is filtered on.
     * @param filterValues the list of attribute values, or <code>null</code> 
     * 		if the view is not filtered.
     */
    public void setFilterValues(List<String> filterValue)
    {
    	set("filterValue", filterValue);
    }
    
    
    /**
     * Returns the String, if any, that is contained within one or more of the
     * evidence attribute values.
     * @return the text contained within attribute values, or <code>null</code> 
     * 		if no search text is specified.
     */
    public String getContainsText()
    {
    	return get("containsText");
    }
    
    
    /**
     * Sets the String to search for within one or more of the evidence 
     * attribute values.
     * @param containsText the text to search for within attribute values.
     */
    public void setContainsText(String containsText)
    {
    	set("containsText", containsText);
    }

	
}
