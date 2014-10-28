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

package com.prelert.proxy.data;

import java.io.Serializable;

/**
 * Class representing the Plugin DataType information as stored in the
 * Prelert database.
 */
public class ExternalDataTypeConfig implements Serializable
{
	private static final long serialVersionUID = 8184173248786214326L;
	
	private String m_Type;
	private String m_Category;
	private boolean m_IsExternal;
	private String m_ExternalPlugin;

	
	public String getType() 
	{
		return m_Type;
	}
	
	public void setType(String type) 
	{
		this.m_Type = type;
	}
	
	public String getCategory() 
	{
		return m_Category;
	}
	
	public void setCategory(String category) 
	{
		this.m_Category = category;
	}
		
	public boolean getIsExternal() 
	{
		return m_IsExternal;
	}
	
	public void setIsExternal(boolean isExternal) 
	{
		this.m_IsExternal = isExternal;
	}
	
	/**
	 * The key string for the external plugin, may be null.
	 * @return Value could be null.
	 */
	public String getExternalPlugin() 
	{
		return m_ExternalPlugin;
	}
	
	public void setExternalPlugin(String externalPlugin) 
	{
		this.m_ExternalPlugin = externalPlugin;
	}
	

	@Override
    public String toString()
    {
		StringBuilder strRep = new StringBuilder();
		strRep.append('{');
		strRep.append("Type = " + m_Type + "; ");
		strRep.append("Category = " + m_Category + "; ");
		strRep.append("IsExternal = " + m_IsExternal + "; ");
		strRep.append("ExternalPlugin = " + m_ExternalPlugin + "; ");
		strRep.append('}');
		
		return strRep.toString();
    }

}
