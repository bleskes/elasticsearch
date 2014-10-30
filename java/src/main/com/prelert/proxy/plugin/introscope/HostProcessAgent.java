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

package com.prelert.proxy.plugin.introscope;

import java.text.ParseException;

/**
 * Utility class representing the 3 parts of a Host|Process|Agent
 * path in Introscope. 
 */
public class HostProcessAgent implements Comparable<HostProcessAgent>
{
	private final String m_Host;
	private final String m_Process;
	private final String m_Agent;
	
	/**
	 * Constructor parses the <code>agentPath</code> into its
	 * host/process/agent components.
	 * @param agentPath
	 * @throws ParseException 
	 */
	public HostProcessAgent(String agentPath) throws ParseException
	{
		String [] agentSplit = agentPath.split("\\|");
		if (agentSplit.length != 3)
		{
			throw new ParseException(
					"Agent path should be of the form (.*)\\|(.*)\\|(.*). Arg=" 
											+ agentPath, 0);
		}
		
		m_Host = agentSplit[0];
		m_Process = agentSplit[1];
		m_Agent = agentSplit[2];
	}
	
	public String getHost()
	{
		return m_Host;
	}
	
	public String getProcess()
	{
		return m_Process;
	}
	
	public String getAgent()
	{
		return m_Agent;
	}

	@Override
	public int compareTo(HostProcessAgent other) 
	{
		String thisHPA = m_Host + m_Process + m_Agent;
		String otherHPA = other.m_Host + other.m_Process + other.m_Agent;
		
		return thisHPA.compareTo(otherHPA);
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other == null)
		{
			return false;
		}
		
		if (other == this)
		{
			return true;
		}
		
		if (!(other instanceof HostProcessAgent))
		{
			return false;
		}
					
		HostProcessAgent otherInst = (HostProcessAgent)other;
		
		String thisHPA = m_Host + m_Process + m_Agent;
		String otherHPA = otherInst.m_Host + otherInst.m_Process + otherInst.m_Agent;
		
		return thisHPA.equals(otherHPA);
	}
	
	@Override
	public int hashCode()
	{
		String thisHPA = m_Host + m_Process + m_Agent;
		return thisHPA.hashCode();
	}
	
	
}
