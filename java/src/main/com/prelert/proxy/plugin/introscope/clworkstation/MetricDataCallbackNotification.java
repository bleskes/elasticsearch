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


import java.io.CharArrayReader;
import java.nio.BufferOverflowException;
import java.nio.CharBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.apache.log4j.Logger;

import com.prelert.data.TimeSeriesData;
import com.wily.introscope.spec.server.beans.clw.ICommandNotification;
import com.wily.isengard.messageprimitives.ConnectionException;

public class MetricDataCallbackNotification implements ICommandNotification
{
	private static Logger s_Logger = Logger.getLogger(MetricDataCallbackNotification.class);
	
	volatile private int m_ReturnStatus;	
	private String m_Datatype;
	
	// Char buffer
	volatile private CharBuffer m_CharBuffer;
	
	private CyclicBarrier m_CyclicBarrier;
	
	
	public MetricDataCallbackNotification(String dataTypeName)
	{
		m_Datatype = dataTypeName;
		
		m_CyclicBarrier = new CyclicBarrier(2);

		// Start with a 1MB buffer
		m_CharBuffer = CharBuffer.allocate(1024 * 1024);
		
		m_ReturnStatus = 0;
	}


	@Override
	public void noticeCommandOutput(char[] arg0) throws ConnectionException
	{
		if (m_CharBuffer.position() + arg0.length > m_CharBuffer.capacity())
		{
			// Take the maximum of twice the previous buffer size and the
			// required space for this addition
			long newCapacity = Math.max(m_CharBuffer.capacity() * 2L,
										m_CharBuffer.position() + (long)arg0.length);
			if (newCapacity > Integer.MAX_VALUE)
			{
				s_Logger.warn("Wanted to increase char buffer capacity to "
								+ newCapacity + " but the limit is "
								+ Integer.MAX_VALUE);
				newCapacity = Integer.MAX_VALUE;
			}

			s_Logger.debug("Char buffer capacity about to increase from "
							+ m_CharBuffer.capacity() + " to " + newCapacity);
			CharBuffer temp = CharBuffer.allocate((int)newCapacity);
			temp.put(m_CharBuffer.array(), 0, m_CharBuffer.position());

			m_CharBuffer = temp;
		}

		try
		{
			m_CharBuffer.put(arg0);
		}
		catch (BufferOverflowException e)
		{
			s_Logger.error("Buffer overflow adding: " + new String(arg0), e);
		}
	}


	@Override
	public void noticeErrorOutput(char[] arg0) throws ConnectionException 
	{
		s_Logger.error("Callback notification error- " + new String(arg0));
		
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
	
	
	public Collection<TimeSeriesData> getQueryResults()
	{
		IntroscopeCsvProcessor csvProcessor = new IntroscopeCsvProcessor(m_Datatype);
		
		Collection<TimeSeriesData> result;
		if (m_CharBuffer.hasArray())
		{
			result = csvProcessor.processCsv(
					new CharArrayReader(m_CharBuffer.array(), 0, m_CharBuffer.position()));
		}
		else
		{
			s_Logger.error("Char buffer has no array");
			result = Collections.emptyList();
		}

		csvProcessor = null;

		m_CharBuffer.clear();  // resets the buffer but does not free memory.

		return result;
	}

}
