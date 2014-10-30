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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import junit.framework.Assert;

/**
 * Tests the configured fields in the analysis are correct 
 * {@linkplain AnalysisConfig#analysisFields()}
 * {@linkplain AnalysisConfig#fields()}
 * {@linkplain AnalysisConfig#byFields()}
 * {@linkplain AnalysisConfig#overFields()}
 * {@linkplain AnalysisConfig#partitionFields()}
 */
public class AnalysisConfigTest 
{
	@Test
	public void testFieldConfiguration()
	{
		AnalysisConfig ac = new AnalysisConfig();
		Detector det = new Detector();
		det.setFieldName("responsetime");
		det.setByFieldName("airline");
		det.setPartitionFieldName("sourcetype");
		ac.setDetectors(Arrays.asList(det));

		Set<String> analysisFields = new TreeSet<String>(Arrays.asList(new String [] {
				"responsetime", "airline", "sourcetype"}));

		for (String s : ac.analysisFields())
		{
			Assert.assertTrue(analysisFields.contains(s));
		}
		
		
		Assert.assertEquals(1, ac.fields().size());
		Assert.assertTrue(ac.fields().contains("responsetime"));
		
		Assert.assertEquals(1, ac.byFields().size());
		Assert.assertTrue(ac.byFields().contains("airline"));
		
		Assert.assertEquals(1, ac.partitionFields().size());
		Assert.assertTrue(ac.partitionFields().contains("sourcetype"));
		
		
		List<Detector> detectors = new ArrayList<>();
		
		ac = new AnalysisConfig();
		det = new Detector();
		det.setFieldName("metric1");
		det.setByFieldName("by_one");
		det.setPartitionFieldName("partition_one");
		detectors.add(det);
		
		det = new Detector();
		det.setFieldName("metric2");
		det.setByFieldName("by_two");
		det.setOverFieldName("over_field");
		detectors.add(det);
		
		det = new Detector();
		det.setFieldName("metric2");
		det.setByFieldName("by_two");
		det.setPartitionFieldName("partition_two");
		detectors.add(det);
		
		ac.setDetectors(detectors);

		analysisFields = new TreeSet<String>(Arrays.asList(new String [] {
				"metric1", "metric2", "by_one", "by_two", "over_field", 
				"partition_one", "partition_two"}));

		for (String s : ac.analysisFields())
		{
			Assert.assertTrue(analysisFields.contains(s));
		}
		
		
		Assert.assertEquals(2, ac.fields().size());
		Assert.assertTrue(ac.fields().contains("metric1"));
		Assert.assertTrue(ac.fields().contains("metric2"));
		
		Assert.assertEquals(2, ac.byFields().size());
		Assert.assertTrue(ac.byFields().contains("by_one"));
		Assert.assertTrue(ac.byFields().contains("by_two"));
		
		Assert.assertEquals(1, ac.overFields().size());
		Assert.assertTrue(ac.overFields().contains("over_field"));
		
		Assert.assertEquals(2, ac.partitionFields().size());
		Assert.assertTrue(ac.partitionFields().contains("partition_one"));
		Assert.assertTrue(ac.partitionFields().contains("partition_two"));
		
	}
}
