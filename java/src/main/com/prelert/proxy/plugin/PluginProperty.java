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

package com.prelert.proxy.plugin;

import java.util.Collections;
import java.util.List;

/**
 * Defines a property that can set on a plugin.
 * </br>If the plugin must have the property set then {@link #isRequired()}
 * will return true.
 * </br>If the property value must be one of a small set of values
 * e.g. Database types will be one of 'MySQL, Postgres, SQL Server'
 * then {@link #getPreDefinedValues()} will return those values or an
 * empty list if any value can be set.
 */
public class PluginProperty 
{
	private String m_Key;
	private List<String> m_AcceptableValues;
	
	private boolean m_Required;
	private boolean m_IsRegularExpression;

	
	public PluginProperty(String key, boolean required)
	{
		m_Key = key;
		m_Required = required;
		m_IsRegularExpression = false;
		m_AcceptableValues = Collections.emptyList();
	}

	public PluginProperty(String key, boolean required, boolean isRegEx)
	{
		m_Key = key;
		m_Required = required;
		m_IsRegularExpression = isRegEx;
		m_AcceptableValues = Collections.emptyList();
	}
	
	
	public PluginProperty(String key, boolean required, 
							List<String> acceptableValues)
	{
		m_Key = key;
		m_Required = required;
		m_IsRegularExpression = false;
		m_AcceptableValues = acceptableValues;
	}
	
	
	/**
	 * The property's key name.
	 * @return
	 */
	public String getKey()
	{
		return m_Key;
	}
	
	
	/**
	 * Returns true if this Property must be defined for its
	 * plugin.
	 * 
	 * @return
	 */
	public boolean isRequired()
	{
		return m_Required;
	}

	
	/**
	 * Returns true if this properties key is a regular expression
	 * and so could match multiple properties.
	 * 
	 * @return
	 */
	public boolean isRegularExpression()
	{
		return m_IsRegularExpression;
	}
	
	/**
	 * If the property value has to be one of a pre-defined set
	 * of values this function will return the list of acceptable
	 * values or an empty list if there is not a defined set of
	 * values. 
	 * @return 
	 */
	public List<String> getPreDefinedValues()
	{
		return m_AcceptableValues;
	}
	
}
