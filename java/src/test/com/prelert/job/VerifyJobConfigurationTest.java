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

package com.prelert.job;

import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.prelert.rs.data.ErrorCode;

/**
 * Test the {@link JobConfiguration#verify()} function for 
 * basic errors in the configuration   
 */
public class VerifyJobConfigurationTest 
{
	@Test
	public void dataDescriptionTest() 
	throws JobConfigurationException
	{
		String badFormat = "YYY-mm-UU hh:mm:ssY";
		DataDescription dd = new DataDescription();
		
		dd.setTimeFormat(badFormat);
		try
		{
			dd.verify();
			// shouldn't get here
			Assert.assertTrue("Invalid format should throw", false);
		}
		catch (JobConfigurationException e)
		{
		}
		
		String goodFormat = "yyyy.MM.dd G 'at' HH:mm:ss z";
		dd.setTimeFormat(goodFormat);
		Assert.assertTrue("Good time format", dd.verify());
	}
	
	
	@Test 
	public void analysisConfigTest()
	throws JobConfigurationException
	{
		AnalysisConfig ac = new AnalysisConfig();
		
		// no detector config
		Detector d = new Detector();
		ac.setDetectors(Arrays.asList(new Detector[] {d}));
		try
		{
			ac.verify();
			Assert.assertTrue(false); // shouldn't get here
		}
		catch (JobConfigurationException e)
		{
		}
		
		// count works with no fields
		d.setFunction("count");
		ac.verify();
		
		d.setFunction("distinct_count");
		try
		{
			ac.verify();
			Assert.assertTrue(false); // shouldn't get here
		}
		catch (JobConfigurationException e)
		{
		}
		
		// should work now
		d.setFieldName("somefield");
		d.setOverFieldName("over");
		ac.verify();
		
		d.setFunction("made_up_function");
		try
		{
			ac.verify();
			Assert.assertTrue(false); // shouldn't get here
		}
		catch (JobConfigurationException e)
		{
		}
		
		ac.setBatchSpan(-1L);
		try
		{
			ac.verify();
			Assert.assertTrue(false); // shouldn't get here
		}
		catch (JobConfigurationException e)
		{
		}
		
		ac = new AnalysisConfig();
		ac.setBucketSpan(-1L);
		try
		{
			ac.verify();			
			Assert.assertTrue(false); // shouldn't get here
		}
		catch (JobConfigurationException e)
		{
		}
		
		ac = new AnalysisConfig();
		ac.setPeriod(-1L);
		try
		{
			ac.verify();
			Assert.assertTrue(false); // shouldn't get here
		}
		catch (JobConfigurationException e)
		{
		}		
	}
	
	@Test 
	public void analysisLimitsTest()
	throws JobConfigurationException
	{
		AnalysisLimits ao = new AnalysisLimits(-1, 10000);
		try
		{
			ao.verify();
			Assert.assertTrue(false); // shouldn't get here
		}
		catch (JobConfigurationException e)
		{
		}
		
		ao = new AnalysisLimits(300, -5000);
		try
		{
			ao.verify();
			Assert.assertTrue(false); // shouldn't get here
		}
		catch (JobConfigurationException e)
		{
		}
	}
	
	@Test 
	public void jobConfigurationTest() 
	throws JobConfigurationException
	{
		JobConfiguration jc = new JobConfiguration();
		try
		{
			jc.verify();
			Assert.assertTrue(false); // shouldn't get here
		}
		catch (JobConfigurationException e)
		{
			Assert.assertTrue(e.getErrorCode() == ErrorCode.INCOMPLETE_CONFIGURATION);
		}
		
		jc.setReferenceJobId("ref_id");
		jc.verify();
			
		jc.setId("bad id with spaces");
		try
		{
			jc.verify();
			Assert.assertTrue(false); // shouldn't get here
		}
		catch (JobConfigurationException e)
		{
			Assert.assertTrue(e.getErrorCode() == ErrorCode.PROHIBITIED_CHARACTER_IN_JOB_ID);
		}
		
		
		jc.setId("bad id with UPPERCASE chars");
		try
		{
			jc.verify();
			Assert.assertTrue(false); // shouldn't get here
		}
		catch (JobConfigurationException e)
		{
			Assert.assertTrue(e.getErrorCode() == ErrorCode.PROHIBITIED_CHARACTER_IN_JOB_ID);
		}
		
		jc.setId("a very  very very very very very very very very very very very very very very very very very very very long id");
		try
		{
			jc.verify();
			Assert.assertTrue(false); // shouldn't get here
		}
		catch (JobConfigurationException e)
		{
			Assert.assertTrue(e.getErrorCode() == ErrorCode.JOB_ID_TOO_LONG);
		}
		
		
		jc.setId(null);
		jc.setReferenceJobId(null);
		jc.setAnalysisConfig(new AnalysisConfig());
		try
		{
			jc.verify();
			Assert.assertTrue(false); // shouldn't get here
		}
		catch (JobConfigurationException e)
		{
			Assert.assertTrue(e.getErrorCode() == ErrorCode.INCOMPLETE_CONFIGURATION);
		}
		
		
		
		AnalysisConfig ac = new AnalysisConfig();
		Detector d = new Detector();
		d.setFieldName("a");
		d.setByFieldName("b");
		d.setFunction("max");
		ac.setDetectors(Arrays.asList(new Detector[] {d}));
		
		jc.setAnalysisConfig(ac);
		jc.verify(); // ok
		
		jc.setAnalysisLimits(new AnalysisLimits(-1, 0));
		try
		{
			jc.verify();
			Assert.assertTrue(false); // shouldn't get here
		}
		catch (JobConfigurationException e)
		{
		}
		
		jc.setAnalysisLimits(new AnalysisLimits(1000, 50000));
		jc.verify();
		
		DataDescription dc = new DataDescription();
		dc.setTimeFormat("YYY_KKKKajsatp*");
		
		jc.setDataDescription(dc);
		try
		{
			jc.verify();
			Assert.assertTrue(false); // shouldn't get here
		}
		catch (JobConfigurationException e)
		{
		}
		
		
		dc = new DataDescription();
		jc.setDataDescription(dc);
		
		
		jc.setTimeout(-1L);
		try
		{
			jc.verify();
			Assert.assertTrue(false); // shouldn't get here
		}
		catch (JobConfigurationException e)
		{
		}
		
		jc.setTimeout(300L);
		jc.verify(); 

	}
	
	
	/**
	 * Test the {@link AnalysisConfig#analysisFields()} method which produces
	 * a list of analysis fields from the detectors
	 */
	@Test
	public void testAnalysisConfigRequiredFields()
	{
		Detector d1 = new Detector();
		d1.setFieldName("field");
		d1.setByFieldName("by");
		d1.setFunction("max");
		
		Detector d2 = new Detector();
		d2.setFieldName("field2");
		d2.setOverFieldName("over");
		
		AnalysisConfig ac = new AnalysisConfig();
		ac.setDetectors(Arrays.asList(new Detector[] {d1, d2}));
		
		List<String> analysisFields = ac.analysisFields();
		Assert.assertTrue(analysisFields.size() == 4);
		
		Assert.assertTrue(analysisFields.contains("field"));
		Assert.assertTrue(analysisFields.contains("by"));
		Assert.assertTrue(analysisFields.contains("field2"));
		Assert.assertTrue(analysisFields.contains("over"));
		
		Assert.assertFalse(analysisFields.contains("max"));
		Assert.assertFalse(analysisFields.contains(""));
		Assert.assertFalse(analysisFields.contains(null));
		
		Detector d3 = new Detector();
		d3.setFunction("count");
		d3.setByFieldName("by2");
		d3.setPartitionFieldName("partition");
		
		ac = new AnalysisConfig();
		ac.setDetectors(Arrays.asList(new Detector[] {d1, d2, d3}));
		
		analysisFields = ac.analysisFields();
		Assert.assertTrue(analysisFields.size() == 6);
		
		Assert.assertTrue(analysisFields.contains("partition"));
		Assert.assertTrue(analysisFields.contains("field"));
		Assert.assertTrue(analysisFields.contains("by"));
		Assert.assertTrue(analysisFields.contains("by2"));
		Assert.assertTrue(analysisFields.contains("field2"));
		Assert.assertTrue(analysisFields.contains("over"));
		
		Assert.assertFalse(analysisFields.contains("count"));
		Assert.assertFalse(analysisFields.contains("max"));
		Assert.assertFalse(analysisFields.contains(""));
		Assert.assertFalse(analysisFields.contains(null));
	}
	
}
