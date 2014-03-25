/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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

package com.prelert.job;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;


/**
 * Defines the fields to be used in the analysis. 
 * <code>Fieldname</code> must be set and only one of <code>ByFieldName</code> 
 * and <code>OverFieldName</code> should be set.
 */
@JsonInclude(Include.NON_NULL)
public class Detector
{
	private String m_Function;
	private String m_FieldName;
	private String m_ByFieldName;
	private String m_OverFieldName;		
	private Boolean m_UseNull;		
	
	/**
	 * The analysis function used e.g. count, rare, min etc. There is no 
	 * validation to check this value is one a predefined set 
	 * @return The function or <code>null</code> if not set
	 */
	public String getFunction() 
	{
		return m_Function;
	}
	
	public void setFunction(String m_Function) 
	{
		this.m_Function = m_Function;
	}
	
	/**
	 * The Analysis field
	 * @return
	 */
	public String getFieldName() 
	{
		return m_FieldName;
	}
	
	public void setFieldName(String m_FieldName) 
	{
		this.m_FieldName = m_FieldName;
	}
	
	/**
	 * The 'by' field or <code>null</code> if not set. 
	 * @return
	 */
	public String getByFieldName() 
	{
		return m_ByFieldName;
	}
	
	public void setByFieldName(String m_ByFieldName) 
	{
		this.m_ByFieldName = m_ByFieldName;
	}
	
	/**
	 * The 'over' field or <code>null</code> if not set. 
	 * @return
	 */
	public String getOverFieldName() 
	{
		return m_OverFieldName;
	}
	
	public void setOverFieldName(String m_OverFieldName) 
	{
		this.m_OverFieldName = m_OverFieldName;
	}	
	
	/**
	 * Where there isn't a value for the 'by' or 'over' field should a new
	 * series be used as the 'null' series. 
	 * @return
	 */
	public Boolean isUseNull() 
	{
		return m_UseNull;
	}
	
	public void setUseNull(Boolean useNull) 
	{
		this.m_UseNull = useNull;
	}
			
	@Override 
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		
		if (other instanceof Detector == false)
		{
			return false;
		}
		
		Detector that = (Detector)other;
				
		return JobDetails.bothNullOrEqual(this.m_Function, that.m_Function) &&
				JobDetails.bothNullOrEqual(this.m_FieldName, that.m_FieldName) &&
				JobDetails.bothNullOrEqual(this.m_ByFieldName, that.m_ByFieldName) &&
				JobDetails.bothNullOrEqual(this.m_OverFieldName, that.m_OverFieldName) &&
				JobDetails.bothNullOrEqual(this.m_UseNull, that.m_UseNull);					
	}
}
