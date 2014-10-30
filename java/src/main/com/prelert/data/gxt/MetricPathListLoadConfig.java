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

import com.extjs.gxt.ui.client.data.BaseListLoadConfig;

import static com.prelert.data.PropertyNames.*;


/**
 * Extension of the GXT <code>BaseListLoadConfig</code> for configuring
 * loading a level of data from the metric path tree for display in a list.
 * @author Pete Harverson
 */
public class MetricPathListLoadConfig extends BaseListLoadConfig
{
    private static final long serialVersionUID = -4923847932227971523L;
    
    
    /**
	 * Returns the type of data to be loaded.
     * @return the data type, or <code>null</code> if loading the top level of the
     * 	metric path tree.
     */
    public String getType()
    {
    	return get(TYPE);
    }
    
    
    /**
     * Sets the type of data to be loaded.
     * @param type the data type, or <code>null</code> if loading the top level 
     * 	of the metric path tree.
     */
    public void setType(String type)
    {
    	set(TYPE, type);
    }
    
    
    /**
	 * Returns the full metric path of the level above that to be loaded.
     * @return the previous path to the level being loaded, or <code>null</code> 
     * 	if loading the top level of the metric path tree.
     */
    public String getPreviousPath()
    {
    	return get(PREVIOUS_PATH);
    }
    
    
    /**
	 * Sets the full metric path of the level above that to be loaded.
     * @param previousPath the previous path to the level being loaded, 
     * 	or <code>null</code> if loading the top level of the metric path tree.
     */
    public void setPreviousPath(String previousPath)
    {
    	set(PREVIOUS_PATH, previousPath);
    }
    
    
    /**
	 * Returns the value of the attribute of the current level to be loaded,
	 * such as a type, source or attribute value.
     * @return the partial path value at the current level to load, or <code>null</code> 
     * 	if loading the top level of the metric path tree.
     */
    public String getCurrentValue()
    {
    	return get("currentValue");
    }
    
    
    /**
	 * Sets the value of the attribute of the current level to be loaded,
	 * such as a type, source or attribute value.
     * @param currentValue the partial path value at the current level to load, 
     * 	or <code>null</code> if loading the top level of the metric path tree.
     */
    public void setCurrentValue(String currentValue)
    {
    	set("currentValue", currentValue);
    }
    
    
    /**
	 * Returns the opaque integer ID representing the level being loaded, used 
	 * by some external plugins to obtain metric path data.
     * @return numeric ID, or 0 if loading the top level of the tree.
     */
    public int getOpaqueNum()
    {
    	return get(OPAQUE_NUM, 0);
    }
    
    
    /**
	 * Sets the opaque integer ID representing the level being loaded, used 
	 * by some external plugins to obtain metric path data.
     * @param opaqueNum numeric ID, or 0 if loading the top level of the tree.
     */
    public void setOpaqueNum(int opaqueNum)
    {
    	set(OPAQUE_NUM, opaqueNum);
    }
    
    
    /**
	 * Returns the value of the textual field holding the GUID for the level being 
	 * loaded, used by some external plugins to obtain metric path data.
     * @return opaque textual GUID, or <code>null</code> if loading the top level 
     * 	of the metric path tree.
     */
    public String getOpaqueString()
    {
    	return get(OPAQUE_STR);
    }
    
    
    /**
	 * Sets the value of the textual field holding the GUID for the level being 
	 * loaded, used by some external plugins to obtain metric path data.
     * @param opaqueString opaque textual GUID, or <code>null</code> if loading 
     * 	the top level of the metric path tree.
     */
    public void setOpaqueString(String opaqueStr)
    {
    	set(OPAQUE_STR, opaqueStr);
    }
    
    
    /**
	 * Returns a summary of this list load config.
	 * @return String representation of the load config, showing all fields
	 * 	and values.
	 */
    @Override
	public String toString()
	{
		StringBuilder strRep = new StringBuilder();
		strRep.append(getProperties());
		
		return strRep.toString();
	}

}
