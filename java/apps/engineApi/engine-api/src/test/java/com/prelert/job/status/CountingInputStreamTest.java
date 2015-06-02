/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
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

package com.prelert.job.status;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.prelert.job.status.CountingInputStream;
import com.prelert.job.usage.DummyUsageReporter;


public class CountingInputStreamTest
{
	private static Logger LOGGER = Logger.getLogger(CountingInputStreamTest.class);

	@Test
	public void testRead_OneByteAtATime() throws IOException
	{
		DummyUsageReporter usageReporter = new DummyUsageReporter("", LOGGER);
		DummyStatusReporter statusReporter = new DummyStatusReporter(usageReporter);

		final String TEXT = "123";
		InputStream source = new ByteArrayInputStream(TEXT.getBytes(StandardCharsets.UTF_8));

		try (CountingInputStream counting = new CountingInputStream(source,
				statusReporter))
		{
			while (counting.read() >= 0)
			{
				;
			}
			// an extra byte is read because we don't check the return
			// value of the read() method
			Assert.assertEquals(TEXT.length()+1, usageReporter.getBytesReadSinceLastReport());

			Assert.assertEquals(usageReporter.getBytesReadSinceLastReport(),
					statusReporter.getBytesRead());
		}
	}

	@Test
	public void testRead_WithBuffer() throws IOException
	{
	    final String TEXT = "To the man who only has a hammer,"
	              + " everything he encounters begins to look like a nail.";

        DummyUsageReporter usageReporter = new DummyUsageReporter("", LOGGER);
        DummyStatusReporter statusReporter = new DummyStatusReporter(usageReporter);

        InputStream source = new ByteArrayInputStream(TEXT.getBytes(StandardCharsets.UTF_8));

		usageReporter = new DummyUsageReporter("", LOGGER);
		statusReporter = new DummyStatusReporter(usageReporter);

		try (CountingInputStream counting = new CountingInputStream(source,
				statusReporter))
		{
			byte buf [] = new byte[256];
			while (counting.read(buf) >= 0)
			{
				;
			}
			// one less byte is reported because we don't check
			// the return value of the read() method
			Assert.assertEquals(TEXT.length() -1, usageReporter.getBytesReadSinceLastReport());

			Assert.assertEquals(usageReporter.getBytesReadSinceLastReport(),
					statusReporter.getBytesRead());
		}
	}

	@Test
    public void testRead_WithTinyBuffer() throws IOException
    {
        final String TEXT = "To the man who only has a hammer,"
                  + " everything he encounters begins to look like a nail.";

        DummyUsageReporter usageReporter = new DummyUsageReporter("", LOGGER);
        DummyStatusReporter statusReporter = new DummyStatusReporter(usageReporter);

        InputStream source = new ByteArrayInputStream(TEXT.getBytes(StandardCharsets.UTF_8));

		usageReporter = new DummyUsageReporter("", LOGGER);
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
			Assert.assertEquals(TEXT.length() -1, usageReporter.getBytesReadSinceLastReport());

			Assert.assertEquals(usageReporter.getBytesReadSinceLastReport(),
					statusReporter.getBytesRead());
		}

	}

}
