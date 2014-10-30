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

package com.prelert.job.input;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.prelert.job.status.DummyStatusReporter;
import com.prelert.job.usage.DummyUsageReporter;


public class CountingInputStreamTest 
{
	static private Logger s_Logger = Logger.getLogger(CountingInputStreamTest.class);

	@Test	
	public void testLengthEncodedWriter() throws IOException
	{
		DummyUsageReporter usageReporter = new DummyUsageReporter("", s_Logger);
		DummyStatusReporter statusReporter = new DummyStatusReporter(usageReporter);
		
		InputStream source = new ByteArrayInputStream("123".getBytes(StandardCharsets.UTF_8));
		
		try (CountingInputStream counting = new CountingInputStream(source, 
				statusReporter))
		{
			while (counting.read() >= 0)
			{
				;
			}
			// an extra byte is read because we don't check the return 
			// value of the read() method
			Assert.assertTrue(usageReporter.getBytesReadSinceLastReport() == 4);
			
			Assert.assertEquals(usageReporter.getBytesReadSinceLastReport(),
					statusReporter.getBytesRead());
		}
		
		source = new ByteArrayInputStream(("To the man who only has a hammer,"
				+ " everything he encounters begins to look like a nail.").getBytes(StandardCharsets.UTF_8));
		
		usageReporter = new DummyUsageReporter("", s_Logger);
		statusReporter = new DummyStatusReporter(usageReporter);
		
		try (CountingInputStream counting = new CountingInputStream(source, 
				statusReporter))
		{
			byte buf [] = new byte[6];
			while (counting.read(buf) >= 0)
			{
				;
			}
			// an extra byte is read because we don't check the return 
			// value of the read() method
			Assert.assertTrue(usageReporter.getBytesReadSinceLastReport() == 85);
			
			Assert.assertEquals(usageReporter.getBytesReadSinceLastReport(),
					statusReporter.getBytesRead());
		}
		
		source = new ByteArrayInputStream(("To the man who only has a hammer,"
				+ " everything he encounters begins to look like a nail.").getBytes(StandardCharsets.UTF_8));
		
		usageReporter = new DummyUsageReporter("", s_Logger);
		statusReporter = new DummyStatusReporter(usageReporter);
		
		try (CountingInputStream counting = new CountingInputStream(source, 
				statusReporter))
		{
			byte buf [] = new byte[8];
			while (counting.read(buf, 0, 8) >= 0)
			{
				;
			}
			// an extra byte is read because we don't check the return 
			// value of the read() method
			Assert.assertTrue(usageReporter.getBytesReadSinceLastReport() == 85);
			
			Assert.assertEquals(usageReporter.getBytesReadSinceLastReport(),
					statusReporter.getBytesRead());
		}
		
		
		
	}


}
