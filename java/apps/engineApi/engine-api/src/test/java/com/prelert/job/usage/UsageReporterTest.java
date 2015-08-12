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
 ************************************************************/

package com.prelert.job.usage;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.mockito.Mockito;

import com.prelert.job.persistence.UsagePersister;

public class UsageReporterTest
{
	@Test
	public void testUpdatePeriod()
	{
		// set the update interval to 1 secs
		System.setProperty(UsageReporter.UPDATE_INTERVAL_PROP, "1");

		Logger logger = Logger.getLogger(UsageReporterTest.class);
		UsagePersister persister = Mockito.mock(UsagePersister.class);
		UsageReporter usage = new UsageReporter("job1", persister, logger);

		usage.addBytesRead(10);
		usage.addFieldsRecordsRead(5);

		Assert.assertEquals(10, usage.getBytesReadSinceLastReport());
		Assert.assertEquals(5, usage.getFieldsReadSinceLastReport());
		Assert.assertEquals(1, usage.getRecordsReadSinceLastReport());

		try
		{
			Thread.sleep(1500);
		}
		catch (InterruptedException e)
		{
			Assert.assertTrue(false);
		}

		usage.addBytesRead(50);
		Mockito.verify(persister, Mockito.times(1)).persistUsage("job1", 60l, 5l, 1l);

		Assert.assertEquals(0, usage.getBytesReadSinceLastReport());
		Assert.assertEquals(0, usage.getFieldsReadSinceLastReport());
		Assert.assertEquals(0, usage.getRecordsReadSinceLastReport());


		// Write another
		usage.addBytesRead(20);
		usage.addFieldsRecordsRead(10);

		Assert.assertEquals(20, usage.getBytesReadSinceLastReport());
		Assert.assertEquals(10, usage.getFieldsReadSinceLastReport());
		Assert.assertEquals(1, usage.getRecordsReadSinceLastReport());


		try
		{
			Thread.sleep(1500);
		}
		catch (InterruptedException e)
		{
			Assert.assertTrue(false);
		}

		usage.addBytesRead(10);
		Mockito.verify(persister, Mockito.times(1)).persistUsage("job1", 30l, 10l, 1l);

		Assert.assertEquals(0, usage.getBytesReadSinceLastReport());
		Assert.assertEquals(0, usage.getFieldsReadSinceLastReport());
		Assert.assertEquals(0, usage.getRecordsReadSinceLastReport());

	}
}
