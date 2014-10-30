/************************************************************
 *                                                          *
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
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

package com.prelert.proxy.plugin.jdbc.scom;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import com.prelert.data.Attribute;

/**
 * Class to handle the conversion of Scom Attributes/External Key strings
 * into SQL query parameters.
 */
public class ScomQueryParams 
{
	public final String EXTERNAL_KEY_SPLIT = "&~%&";
	
	private final Integer m_EntityId;
	private final Integer m_RuleInstanceId;
	
	private final String m_FullName;
	private final String m_ObjectName;
	private final String m_InstanceName;
	private final String m_CounterInstance;
	
	
	
	/**
	 * Create a <code>ScomQueryParams</code>.
	 * 
	 * @param fullName
	 * @param objectName
	 * @param instanceName
	 * @param counterInstance
	 */
	public ScomQueryParams(String fullName, String objectName,
							String instanceName, String counterInstance,
							int entityId, int ruleInstanceId)
	{
		m_FullName = fullName;
		m_ObjectName = objectName;
		m_InstanceName = instanceName;
		m_CounterInstance = counterInstance;
		
		m_EntityId = entityId;
		m_RuleInstanceId = ruleInstanceId;
	}

	/**
	 * Parses the Scom query parameters from <code>externalkey</code>.
	 * If <code>externalkey</code> cannot be parsed a ParseException is thrown.
	 * 
	 * @param externalKey
	 * @throws ParseException
	 */
	public ScomQueryParams(String externalKey) throws ParseException
	{
		String [] split = externalKey.split(EXTERNAL_KEY_SPLIT);
		if (split.length != 6)		
		{
			throw new ParseException("ScomQueryAttributes Could not parse external key: " 
										+ externalKey, 0);
		}
		
		m_FullName = split[0];
		m_ObjectName = split[1];
		m_InstanceName = split[2];
		m_CounterInstance = split[3];
		
		m_EntityId = Integer.parseInt(split[4]);
		m_RuleInstanceId = Integer.parseInt(split[5]);
	}
	
	/**
	 * Create a new <code>ScomQueryParams</code>
	 * 
	 * Throws a <code>IllegalArgumentException</code> if the 
	 * ScomPlugin.OBJECTNAME_ATTRIBUTE, ScomPlugin.INSTANCENAME_ATTRIBUTE
	 * and ScomPlugin.FULLNAME_ATTRIBUTE attributes are not present.
	 * 
	 * @param metric - Equivalent to the Scom counter instance
	 * @param attributes
	 */
	public ScomQueryParams(String metric, List<Attribute> attributes)
	{
		String objectName = null;
		String instanceName = null;
		String fullName = null;
		Integer entityId = null;
		Integer ruleInstanceId = null;
		

		for (Attribute attr : attributes)
		{
			if (attr.getAttributeName().equals(ScomPlugin.OBJECTNAME_ATTRIBUTE))
			{
				objectName = attr.getAttributeValue();
			}
			else if (attr.getAttributeName().equals(ScomPlugin.INSTANCENAME_ATTRIBUTE))
			{
				instanceName = attr.getAttributeValue();
			}
			else if (attr.getAttributeName().equals(ScomPlugin.FULLNAME_ATTRIBUTE))
			{
				fullName = attr.getAttributeValue();
			}
			else if (attr.getAttributeName().equals(ScomPlugin.ENTITY_ID_ATTRIBUTE))
			{
				entityId = Integer.parseInt(attr.getAttributeValue());
			}
			else if (attr.getAttributeName().equals(ScomPlugin.RULE_INSTANCE_ID_ATTRIBUTE))
			{
				ruleInstanceId = Integer.parseInt(attr.getAttributeValue());
			}
		}
		
		
		m_CounterInstance = metric;
		
		m_ObjectName = objectName;
		m_InstanceName = instanceName;
		m_FullName = fullName;
		
		m_EntityId = entityId;
		m_RuleInstanceId = ruleInstanceId;
		
		if (m_CounterInstance == null || m_ObjectName == null || m_FullName == null ||
				m_EntityId == null || m_RuleInstanceId == null)
		{
			throw new IllegalArgumentException("The SCOM attributes: " + 
									ScomPlugin.OBJECTNAME_ATTRIBUTE + ", " + 
									ScomPlugin.INSTANCENAME_ATTRIBUTE + " and " +
									ScomPlugin.FULLNAME_ATTRIBUTE + " and " +
									ScomPlugin.ENTITY_ID_ATTRIBUTE + " and " +
									ScomPlugin.RULE_INSTANCE_ID_ATTRIBUTE +
									" must all be defined.");
		}
		
	}
	
	/**
	 * Returns the external key string representation of this object.
	 * 
	 * @return
	 */
	public String toExternalKey()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append(m_FullName);
		builder.append(EXTERNAL_KEY_SPLIT);
		builder.append(m_ObjectName);
		builder.append(EXTERNAL_KEY_SPLIT);
		builder.append(m_InstanceName);
		builder.append(EXTERNAL_KEY_SPLIT);
		builder.append(m_CounterInstance);
		builder.append(EXTERNAL_KEY_SPLIT);
		builder.append(m_EntityId.toString());
		builder.append(EXTERNAL_KEY_SPLIT);
		builder.append(m_RuleInstanceId.toString());
		return builder.toString();
	}
	
	
	/**
	 * Get the query parameters as <code>Attribute</code>s.
	 * The result is the as if this object was constructed with
	 * <code>Attribute</code>s.
	 * 
	 * @return
	 */
	public List<Attribute> getAttributes()
	{
		List<Attribute> result = new ArrayList<Attribute>();
		result.add(new Attribute(ScomPlugin.OBJECTNAME_ATTRIBUTE, m_ObjectName));
		result.add(new Attribute(ScomPlugin.INSTANCENAME_ATTRIBUTE, m_InstanceName));
		result.add(new Attribute(ScomPlugin.FULLNAME_ATTRIBUTE, m_FullName));
		result.add(new Attribute(ScomPlugin.ENTITY_ID_ATTRIBUTE, m_EntityId.toString()));
		result.add(new Attribute(ScomPlugin.RULE_INSTANCE_ID_ATTRIBUTE, m_RuleInstanceId.toString()));
		
		return result;
	}

	public String getFullName()
	{
		return m_FullName;
	}
	
	public String getObjectName()
	{
		return m_ObjectName;
	}

	public String getInstanceName()
	{
		return m_InstanceName;
	}
	
	public String getCounterInstance()
	{
		return m_CounterInstance;
	}
	
	public Integer getEntityId()
	{
		return m_EntityId;
	}
	
	public Integer getRuleInstanceId()
	{
		return m_RuleInstanceId;
	}
}
