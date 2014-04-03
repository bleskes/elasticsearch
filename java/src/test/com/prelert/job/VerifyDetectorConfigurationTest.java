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
package com.prelert.job;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

public class VerifyDetectorConfigurationTest
{
	/**
	 * Test the good/bad detector configurations
	 * @throws JobConfigurationException 
	 */
	@Test
	public void detectorsTest() throws JobConfigurationException
	{
		// if nothing else is set count is the only allowable function
		Detector d = new Detector();
		d.setFunction(Detector.COUNT);
		d.verify();
		
		Set<String> difference = new HashSet<String>(Detector.ANALYSIS_FUNCTIONS);
		difference.remove(Detector.COUNT);
		for (String f : difference)
		{
			try
			{
				d.setFunction(f);
				d.verify();
				Assert.assertTrue(false); // should throw
			}
			catch (JobConfigurationException e)
			{
			}
		}
		
		// certain fields aren't allowed with certain functions
		// first do the over field
		d.setOverFieldName("over");
		d.setFunction(Detector.NON_ZERO_COUNT);
		try
		{
			d.verify();
			Assert.assertTrue(false); // should throw
		}
		catch (JobConfigurationException e)
		{
		}
		
		d.setFunction(Detector.NZC);
		try
		{
			d.verify();
			Assert.assertTrue(false); // should throw
		}
		catch (JobConfigurationException e)
		{
		}
				
		for (String f : new String [] {Detector.COUNT, Detector.HIGH_COUNT, 
				Detector.LOW_COUNT})
		{
			d.setFunction(f);
			d.verify();
		}
		
		d.setByFieldName("by");
		for (String f : new String [] {Detector.RARE, Detector.FREQ_RARE})
		{
			d.setFunction(f);
			d.verify();
		}
		d.setByFieldName(null);
		
		
		// some functions require a fieldname
		d.setFieldName("f");
		for (String f : new String [] {Detector.DISTINCT_COUNT, Detector.DC, 
				Detector.METRIC, Detector.MEAN, Detector.AVG, Detector.MAX, 
				Detector.MIN, Detector.SUM})
		{
			d.setFunction(f);
			d.verify();
		}
		
		

		// do the by field
		d = new Detector();
		d.setByFieldName("by");
		d.setFunction(Detector.DISTINCT_COUNT);
		try
		{
			d.verify();
			Assert.assertTrue(false); // should throw
		}
		catch (JobConfigurationException e)
		{
		}
		
		d.setFunction(Detector.DC);
		try
		{
			d.verify();
			Assert.assertTrue(false); // should throw
		}
		catch (JobConfigurationException e)
		{
		}
				
		for (String f : new String [] {Detector.COUNT, Detector.HIGH_COUNT, 
				Detector.LOW_COUNT, Detector.RARE, 
				Detector.NON_ZERO_COUNT, Detector.NZC})
		{
			d.setFunction(f);
			d.verify();
		}
		
		d.setOverFieldName("over");
		d.setFunction(Detector.FREQ_RARE);
		d.verify();
		d.setOverFieldName(null);
		
		// some functions require a fieldname
		d.setFieldName("f");
		for (String f : new String [] {Detector.METRIC, Detector.MEAN, 
				Detector.AVG, Detector.MAX, 
				Detector.MIN, Detector.SUM})
		{
			d.setFunction(f);
			d.verify();
		}
		
		
		// test field name
		d = new Detector();
		d.setFieldName("field");
		for (String f : new String [] {Detector.COUNT, Detector.HIGH_COUNT, 
				Detector.LOW_COUNT, Detector.NON_ZERO_COUNT, Detector.NZC,
				Detector.RARE, Detector.FREQ_RARE})
		{
			try
			{
				d.setFunction(f);
				d.verify();
				Assert.assertTrue(false); // should throw
			}
			catch (JobConfigurationException e)
			{
			}
		}
		
		d.setOverFieldName("over");
		d.setFunction(Detector.FREQ_RARE);
		try
		{
			d.verify();
			Assert.assertTrue(false); // should throw
		}
		catch (JobConfigurationException e)
		{
		}
		
		for (String f : new String [] {Detector.DISTINCT_COUNT, Detector.METRIC, 
				Detector.MEAN, Detector.AVG, Detector.MIN,
				Detector.MAX, Detector.SUM})
		{
			d.setFunction(f);
			d.verify();
		}
		
		
		// count functions should have a by or over field 
		d = new Detector();
		for (String f : new String [] {Detector.HIGH_COUNT, 
				Detector.LOW_COUNT, Detector.NON_ZERO_COUNT, Detector.NZC})
		{
			try
			{
				d.setFunction(f);
				d.verify();
				Assert.assertTrue(false); // should throw
			}
			catch (JobConfigurationException e)
			{
			}
		}
		
		d = new Detector();
		d.setByFieldName("by");
		for (String f : new String [] {Detector.HIGH_COUNT, 
				Detector.LOW_COUNT, Detector.NON_ZERO_COUNT, Detector.NZC})
		{
			d.setFunction(f);
			d.verify();
		}
		
		d = new Detector();
		d.setOverFieldName("over");
		for (String f : new String [] {Detector.HIGH_COUNT, 
				Detector.LOW_COUNT})
		{
			d.setFunction(f);
			d.verify();
		}
		
		d = new Detector();
		d.setByFieldName("by");
		d.setOverFieldName("over");
		for (String f : new String [] {Detector.HIGH_COUNT, 
				Detector.LOW_COUNT})
		{
			d.setFunction(f);
			d.verify();
		}
		
		d = new Detector();
		d.setByFieldName("by");
		d.setOverFieldName("over");
		for (String f : new String [] {Detector.NON_ZERO_COUNT, Detector.NZC})
		{
			try
			{
				d.setFunction(f);
				d.verify();
				Assert.assertTrue(false); // should throw
			}
			catch (JobConfigurationException e)
			{
			}
		}
		
		
	}
}
