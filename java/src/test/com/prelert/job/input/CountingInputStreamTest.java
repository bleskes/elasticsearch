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
 ***********************************************************/

package com.prelert.job.input;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import com.prelert.job.DummyUsageReporter;


public class CountingInputStreamTest 
{

	@Test	
	public void testLengthEncodedWriter() throws IOException
	{
		DummyUsageReporter usageRepoter = new DummyUsageReporter("", null);
		
		InputStream source = new ByteArrayInputStream("123".getBytes(StandardCharsets.UTF_8));
		
		try (CountingInputStream counting = new CountingInputStream(source, usageRepoter))
		{
			while (counting.read() >= 0)
			{
				;
			}
			// an extra byte is read because we don't check the return 
			// value of the read() method
			Assert.assertTrue(counting.getTotalBytesRead() == 4);
			Assert.assertTrue(usageRepoter.getBytesRead() == 4);
		}
		
		source = new ByteArrayInputStream(("To the man who only has a hammer,"
				+ " everything he encounters begins to look like a nail.").getBytes(StandardCharsets.UTF_8));
		usageRepoter = new DummyUsageReporter("", null);
		try (CountingInputStream counting = new CountingInputStream(source, usageRepoter))
		{
			byte buf [] = new byte[6];
			while (counting.read(buf) >= 0)
			{
				;
			}
			// an extra byte is read because we don't check the return 
			// value of the read() method
			Assert.assertTrue(counting.getTotalBytesRead() == 85);
			Assert.assertTrue(usageRepoter.getBytesRead() == 85);
		}
		
		source = new ByteArrayInputStream(("To the man who only has a hammer,"
				+ " everything he encounters begins to look like a nail.").getBytes(StandardCharsets.UTF_8));
		usageRepoter = new DummyUsageReporter("", null);
		try (CountingInputStream counting = new CountingInputStream(source, usageRepoter))
		{
			byte buf [] = new byte[8];
			while (counting.read(buf, 0, 8) >= 0)
			{
				;
			}
			// an extra byte is read because we don't check the return 
			// value of the read() method
			Assert.assertTrue(counting.getTotalBytesRead() == 85);
			Assert.assertTrue(usageRepoter.getBytesRead() == 85);
		}
		
		
		
	}


}
