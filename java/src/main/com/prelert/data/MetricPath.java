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

package com.prelert.data;

import java.io.Serializable;

/**
 * Class represents a MetricPath 
 */
public class MetricPath implements Serializable
{
	private static final long serialVersionUID = -4212146561237228420L;
	
	private String m_Datatype;
	private String m_LastLevelName;
	private String m_LastLevelValue;
	private String m_LastLevelPrefix;
	private String m_PartialPath;
	private int m_OpaqueNum;
	private String m_OpaqueStr;
	private String m_ExternalKey;
	
	
	/**
	 * Creates a new, empty <code>MetricPath</code> with empty properties.
	 */
	public MetricPath()
	{
		m_OpaqueNum = 0;
	}
	
	
	/**
	 * Returns the data type of this <code>MetricPath</code>.
	 * @return the data type.
	 */
	public String getDatatype() 
	{
		return m_Datatype;
	}

	
	/**
	 * Sets the data type of this <code>MetricPath</code>.
	 * @param datatype the data type.
	 */
	public void setDatatype(String datatype) 
	{
		m_Datatype = datatype;
	}
	

	/**
	 * Returns the name of the field that is the last level in this metric path.
	 * @return the name of the last level.
	 */
	public String getLastLevelName() 
	{
		return m_LastLevelName;
	}

	
	/**
	 * Sets the name of the field that is the last level in this metric path.
	 * @param metricName the name of the metric field.
	 */
	public void setLastLevelName(String lastLevelName) 
	{
		m_LastLevelName = lastLevelName;
	}

	
	/**
	 * Returns the prefix string that separates the last level from the rest of the
	 * metric path.
	 * @return the prefix used before the last level in this metric path.
	 */
	public String getLastLevelPrefix() 
	{
		return m_LastLevelPrefix;
	}

	
	/**
	 * Sets the prefix string that separates the last level from the rest of the
	 * metric path.
	 * @param metricPrefix the prefix used before the last level in this metric path.
	 */
	public void setLastLevelPrefix(String lastLevelPrefix) 
	{
		m_LastLevelPrefix = lastLevelPrefix;
	}

	
	/**
	 * Returns the value of the last level in this metric path.
	 * @return the value of the last level in the path.
	 */
	public String getLastLevelValue() 
	{
		return m_LastLevelValue;
	}

	
	/**
	 * Sets the value of the last level in this metric path.
	 * @param metricValue the value of the last level in the path.
	 */
	public void setLastLevelValue(String lastLevelValue) 
	{
		m_LastLevelValue = lastLevelValue;
	}
	
	
	/**
	 * Return the partial metric path up to the final metric elements.
	 * @return the partial metric path up to the metric prefix and value.
	 */
	public String getPartialPath()
	{
		return m_PartialPath; 
	}
	
	
	/**
	 * Sets the partial metric path up to the final metric elements.
	 * @param partialPath the partial metric path up to the metric prefix and value.
	 */
	public void setPartialPath(String partialPath)
	{
		m_PartialPath = partialPath;
	}
	
	
	/**
	 * Returns a client defined number that aids the processing of the metric path.
	 * @return opaque numeric ID.
	 */
	public int getOpaqueNum()
	{
		return m_OpaqueNum;
	}
	
	
	/**
	 * Sets a client defined number that aids the processing of the metric path.
	 * @param num opaque numeric ID.
	 */
	public void setOpaqueNum(int num)
	{
		m_OpaqueNum = num;
	}
	
	
	/**
	 * Returns a client defined string that aids the processing of the metric path.
	 * @return opaque textual GUID used by some external plugins to 
	 * 	obtain metric path data.
	 */
	public String getOpaqueStr()
	{
		return m_OpaqueStr;
	}
	
	
	/**
	 * Sets a client defined string that aids the processing of the metric path.
	 * @param opaqueStr opaque textual GUID used by some external plugins to 
	 * 	obtain metric path data.
	 */
	public void setOpaqueStr(String opaqueStr)
	{
		m_OpaqueStr = opaqueStr;
	}
	
	
	/**
	 * Returns the external key used by external time series type.
	 * Will be <code>null</code> or empty if unset.
	 * @return
	 */
	public String getExternalKey()
	{
		return m_ExternalKey;
	}
	
	public void setExternalKey(String externalKey)
	{
		m_ExternalKey = externalKey;
	}


	/**
	 * Returns a summary of the properties of this Metric Path object.
	 * @return String representation of this MetricPath object.
	 */
    @Override
    public String toString()
    {
    	StringBuilder strRep = new StringBuilder();
    	
    	strRep.append("{type=").append(m_Datatype);
    	strRep.append(", partialPath=").append(m_PartialPath);
    	strRep.append(", lastLevelName=").append(m_LastLevelName);
    	strRep.append(", lastLevelValue=").append(m_LastLevelValue);
    	strRep.append(", lastLevelPrefix=").append(m_LastLevelPrefix);
    	strRep.append(", opaqueNum=").append(m_OpaqueNum);
    	strRep.append(", opaqueStr=").append(m_OpaqueStr);
    	strRep.append(", externalKey=").append(m_ExternalKey);
    	
    	strRep.append('}');
		
		return strRep.toString();
    }
	
	
	
	
}
