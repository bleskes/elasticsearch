/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
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

import java.util.List;

import com.extjs.gxt.ui.client.data.BasePagingLoadConfig;
import com.prelert.data.Attribute;


/**
 * Load configuration for paging through causality data. It defines properties
 * for the id of an item of evidence from the aggregated set of data, and primary
 * and secondary attributes by which the causality data should be filtered 
 * e.g. type='p2pslog' and source='lon-data01'.
 * @author Pete Harverson
 */
public class CausalityDataPagingLoadConfig extends BasePagingLoadConfig
{
    private static final long serialVersionUID = -301252403896633987L;
    
    
    /**
	 * Sets the id of an item of evidence from the aggregated set of causality
	 * data for which related items are being loaded.
	 * @param evidenceId the id of an item of evidence from the aggregated causality data.
	 */
    public void setEvidenceId(int evidenceId)
    {
    	set("evidenceId", evidenceId);
    }
    
    
    /**
	 * Returns the id of an item of evidence from the aggregated set of causality
	 * data for which related items are being loaded.
	 * @return the id of an item of evidence from the aggregated causality data.
	 */
    public int getEvidenceId()
	{
		return get("evidenceId", new Integer(-1));
	}


	/**
	 * Sets an optional list of the names of the attributes that should be included in 
	 * the load results.
     * @param returnAttributes list of the names of any additional attributes to be included 
     * 	in the output, or <code>null</code> to return no extra attributes.
     */
    public void setReturnAttributes(List<String> returnAttributes)
    {
    	set("returnAttributes", returnAttributes);
    }
    
    
    /**
	 * Returns a list of the names of the attributes that should be included in 
	 * the load results.
     * @return  list of the names of any additional attributes to be included 
     * 	in the output, or <code>null</code> to return no extra attributes.
     */
    public List<String> getReturnAttributes()
    {
    	return get("returnAttributes");
    }
    
    
    /**
	 * Sets the list of attributes to use as the primary filter. Attribute values 
	 * may be either non-<code>null</code> or <code>null</code>.
	 * @param attributes list of primary filter attributes.
	 */
    public void setPrimaryFilterAttributes(List<Attribute> attributes)
    {
    	set("primaryFilterAttributes", attributes);
    }
    
    
    /**
	 * Returns the list of attributes to use as the primary filter. Attribute values 
	 * may be either non-<code>null</code> or <code>null</code>.
	 * @return list of primary filter attributes.
	 */
    public List<Attribute> getPrimaryFilterAttributes()
    {
    	return get("primaryFilterAttributes");
    }
    
    
    /**
	 * Optionally sets the name of the attribute to use as the secondary filter.
	 * @param attributeName name of the secondary filter attribute. If <code>null</code>
	 * 	then no secondary filter will be applied.
	 */
	public void setSecondaryFilterName(String attributeName)
    {
		set("secondaryFilterName", attributeName);
    }
    
    
	/**
	 * Returns the name of the attribute to use as the secondary filter.
	 * @return attributeName name of the secondary filter attribute. If <code>null</code>
	 * 	then no secondary filter will be applied.
	 */
    public String getSecondaryFilterName()
    {
    	return get("secondaryFilterName");
    }
    
    
    /**
     * Sets the value of the optional secondary filter attribute.
     * @param attributeValue value of the secondary filter attribute. If <code>null</code>
	 * 	then no secondary filter will be applied.
     */
    public void setSecondaryFilterValue(String attributeValue)
    {
    	set("secondaryFilterValue", attributeValue);
    }
    
    
    /**
     * Returns the value of the optional secondary filter attribute.
     * @return value of the secondary filter attribute. If <code>null</code>
	 * 	then no secondary filter will be applied.
     */
    public String getSecondaryFilterValue()
    {
    	return get("secondaryFilterValue");
    }

    
    /**
	 * Returns a summary of this paging load config.
	 * @return String representation of the load config, showing all fields
	 * and values.
	 */
    @Override
	public String toString()
	{
		StringBuilder strRep = new StringBuilder();
		strRep.append(getProperties());
		
		return strRep.toString();
	}
}
