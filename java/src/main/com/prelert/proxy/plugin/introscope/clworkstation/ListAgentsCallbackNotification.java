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

package com.prelert.proxy.plugin.introscope.clworkstation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.apache.log4j.Logger;

import com.wily.introscope.spec.server.beans.clw.ICommandNotification;
import com.wily.isengard.messageprimitives.ConnectionException;

public class ListAgentsCallbackNotification implements ICommandNotification 
{
	private static Logger s_Logger = Logger.getLogger(ListAgentsCallbackNotification.class);
	
	private int m_ReturnStatus;
	
	private List<String> m_QueryResult;
	
	private CyclicBarrier m_CyclicBarrier;
	
	public ListAgentsCallbackNotification()
	{
		m_CyclicBarrier = new CyclicBarrier(2);
		
		m_QueryResult = new ArrayList<String>();
	}
	
	@Override
	public void noticeCommandOutput(char[] arg0) throws ConnectionException
	{
		String all = new String(arg0);
		String[] split = all.split("\r?\n");
		
		for (int i=0; i<split.length; ++i)
		{
			// Strip windows carriage return.
			if (split[i].endsWith("\r"))
			{
				split[i] = split[i].substring(0, split[i].length() - "\r".length());
			}
			m_QueryResult.add(split[i]);
		}
		
		m_ReturnStatus = 0;
	}
	
	@Override
	public void noticeErrorOutput(char[] arg0) throws ConnectionException 
	{
		s_Logger.error("Callback notification error-.");
		s_Logger.error(arg0);
		
		m_ReturnStatus = -1;
	}

	@Override
	public void closing() throws ConnectionException 
	{

	}

	@Override
	public void finished() throws ConnectionException 
	{
		try
		{
			m_CyclicBarrier.await();
		}
		catch (InterruptedException ie)
		{
			s_Logger.debug("Command Notification Barrier interrupted. " + ie);
		}
		catch (BrokenBarrierException bbe)
		{
			s_Logger.debug("Command Notification Barrier broken. " + bbe);
		}
	}
	
	public int waitForFinish()
	{
		try
		{
			m_CyclicBarrier.await();
		}
		catch (InterruptedException ie)
		{
			s_Logger.debug("Command Notification Barrier interrupted. " + ie);
		}
		catch (BrokenBarrierException bbe)
		{
			s_Logger.debug("Command Notification Barrier broken. " + bbe);
		}
		
		return m_ReturnStatus;
	}
	
	public List<String> getQueryResults()
	{
		List<String> result = new ArrayList<String>(m_QueryResult);
		
		m_QueryResult.clear();
		
		return result;
	}

}
