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

import java.util.Collections;
import java.util.List;

import com.extjs.gxt.ui.client.data.BaseTreeModel;

import static com.prelert.data.PropertyNames.*;

/**
 * Extension of the GXT <code>BaseTreeModel</code> for a tree of attribute data
 * analysing time series features and notifications in an activity.
 * @author Pete Harverson
 */
public class ActivityTreeModel extends BaseTreeModel
{
    private static final long serialVersionUID = 578176664108185488L;
    
    @SuppressWarnings("unused")
    private CausalityDataModel	m_CausalityDataFieldSerializer;
    
    /**
     * Sets the name of the attribute displayed by this tree model.
     * @param name the attribute name.
     */
    public void setAttributeName(String name)
    {
    	set(NAME, name);
    }
    
    
    /**
     * Returns the name of the attribute displayed by this tree model.
     * @return the attribute name.
     */
    public String getAttributeName()
    {
    	return get(NAME);
    }

    
    /**
     * Sets the value of the attribute displayed by this tree model.
     * @param value the attribute value.
     */
    public void setAttributeValue(String value)
    {
    	set(VALUE, value);
    }
    
    
    /**
     * Returns the value of the attribute displayed by this tree model.
     * @return the attribute value, which may be <code>null</code>.
     */
    public String getAttributeValue()
    {
    	return get(VALUE);
    }
    
    
    /**
     * Sets a value to use as the display text for this tree model.
     * @param displayValue the text to use as the display value.
     */
    public void setDisplayValue(String displayValue)
    {
    	set("displayVal", displayValue);
    }
    
    
    /**
     * Returns the value to use as the display text for this tree model.
     * @return the text to use as the display value. Returns the attribute
     * 	value if no display value property has been set specifically.
     */
    public String getDisplayValue()
    {
    	return get("displayVal", getAttributeValue());
    }
    
    
    /**
     * Sets the count property for this activity tree model. This represents the
     * occurrence count for this combination of attribute name and value across
     * the time series features and notifications in the activity.
     * @param count the occurrence count.
     */
    public void setCount(int count)
    {
    	set(COUNT, count);
    }
    
    
    /**
     * Returns the count for this activity tree model. This represents the
     * occurrence count for this combination of attribute name and value across
     * the time series features and notifications in the activity.
     * @return the occurrence count, or 0 if the property has not been set.
     */
    public int getCount()
	{
		return get(COUNT, new Integer(0));
	}
    
    
    /**
     * Sets the 'top' item of causality data for this activity tree model,
     * generally only set for leaf nodes.
     * @param causalityData the 'top' item of causality data.
     */
    public void setTopCausalityData(CausalityDataModel causalityData)
    {
    	set("topCausalityData", causalityData);
    }
    
    
    /**
     * Returns the 'top' item of causality data for this activity tree model,
     * generally only set for leaf nodes.
     * @return the 'top' item of causality data, or <code>null</code> if not set.
     */
    public CausalityDataModel getTopCausalityData()
    {
    	return get("topCausalityData");
    }
    
    
    /**
     * Sets a property, which will override the default approach based on child count,
     * to flag whether this model represents a leaf node in the tree.
     * @param isLeaf
     */
    public void setLeaf(boolean isLeaf)
    {
    	set("isLeaf", isLeaf);
    }
    

    @Override
    public boolean isLeaf()
    {
    	Boolean isLeaf = get("isLeaf");
    	
    	if (isLeaf != null)
    	{
    		return isLeaf.booleanValue();
    	}
    	else
    	{
    		return super.isLeaf();
    	}
    }
	
	
	/**
	 * Sets the list of metric path attribute values represented by this tree node,
	 * starting from the root node.
	 * @param values list of attribute values, which may include <code>null</code> values,
	 * 	where the attribute for the root node is the first entry in the returned list.
	 */
	public void setPathAttributeValues(List<String> values)
	{
		set("pathAttributeValues", values);
	}
	
	
	/**
	 * Returns the list of metric path attribute values represented by this tree node,
	 * starting from the root node.
	 * @return list of attribute values, which may include <code>null</code> values,
	 * 	where the attribute for the root node is the first entry in the returned list,
	 * 	or <code>null</code> if this property has not been set.
	 */
	public List<String> getPathAttributeValues()
	{
		return get("pathAttributeValues", Collections.singletonList(getAttributeValue()));
	}
    
    
    /**
	 * Returns a String representation of this activity tree model.
	 * @return String representation of the tree node.
	 */
	public String toString()
	{
		return getProperties().toString();
	}
}
