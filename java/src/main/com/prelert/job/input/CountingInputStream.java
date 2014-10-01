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
package com.prelert.job.input;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.prelert.job.status.StatusReporter;

/**
 * Simple wrapper around an inputstream instance that counts
 * all the bytes read from it. See {@linkplain countBytesRead()}.
 *
 * Overrides the read methods counting the number of bytes read.
 */
public class CountingInputStream extends FilterInputStream 
{
	private StatusReporter m_StatusReporter;
	
	/**
	 * 
	 * @param in
	 * @param usageReporter Writes the number of raw bytes processed over time
	 * @param statusReporter Write number of records, bytes etc.
	 */
	public CountingInputStream(InputStream in, StatusReporter statusReporter) 
	{
		super(in);
		m_StatusReporter = statusReporter;
	}

	/**
	 * We don't care if the count is one byte out
	 * because we don't check for the case where read 
	 * returns -1.
	 * 
	 * One of the buffered read(..) methods is more likely to 
	 * be called anyway.
	 */
	@Override
	public int read() throws IOException 
	{
		m_StatusReporter.reportBytesRead(1);
		
		return in.read();
	}
	
	/**
	 * Don't bother checking for the special case where
	 * the stream is closed/finished and read returns -1.
	 * Our count will be 1 byte out.
	 */
	@Override
	public int read(byte[] b) throws IOException
	{		
		int read = in.read(b);
		
		m_StatusReporter.reportBytesRead(read);
		
		return read;
	}
	
	/**
	 * Don't bother checking for the special case where
	 * the stream is closed/finished and read returns -1.
	 * Our count will be 1 byte out.
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException
	{
		int read = in.read(b, off, len);
		
		m_StatusReporter.reportBytesRead(read);
		return read;
	}
	
}
