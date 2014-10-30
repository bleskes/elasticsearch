/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2013     *
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
 ************************************************************/

package com.prelert.api.test.clw;

import java.io.CharArrayReader;
import java.nio.BufferOverflowException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.apache.log4j.Logger;

import com.wily.introscope.spec.server.beans.clw.ICommandNotification;
import com.wily.isengard.messageprimitives.ConnectionException;

/**
 * Command line workstation callback class.
 * Processes the raw CLW csv data and returns {@link MetricData}
 */
public class MetricDataCallback implements ICommandNotification
{
	private static Logger s_Logger = Logger.getLogger(MetricDataCallback.class);
	
	volatile private int m_ReturnStatus;	
	
	// Char buffer
	volatile private CharBuffer m_CharBuffer;
	
	private CyclicBarrier m_CyclicBarrier;
	
	
	public MetricDataCallback()
	{
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


	/**
	 * A blocking call which will not return until the query has
	 * finished.
	 * @return 0 if the data was successfully loaded or -1 on an error.
	 */
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
	
	
	/**
	 * Converts the csv data returned by the CLWorkstation query and 
	 * converts it to {@link MetricData}
	 * <p/>
	 * This function should only be called after {@link #waitForFinish()} 
	 * has been returned
	 * .
	 * @return List of MetricData
	 */
	public List<MetricData> getQueryResults()
	{
		List<MetricData> result = new ArrayList<MetricData>();
		if (m_CharBuffer.hasArray())
		{
			ClwCsvProcessor csvProcessor = new ClwCsvProcessor();
			result = csvProcessor.processCsv(
					new CharArrayReader(m_CharBuffer.array(), 0, m_CharBuffer.position()));
		}
		else
		{
			s_Logger.error("Char buffer has no array");
			result = Collections.emptyList();
		}

		m_CharBuffer.clear();  // resets the buffer but does not free memory.

		return result;
	}


}
