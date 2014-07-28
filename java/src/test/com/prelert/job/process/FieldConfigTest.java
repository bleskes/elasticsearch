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

package com.prelert.job.process;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.ini4j.Config;
import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.junit.Test;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.Detector;
import com.prelert.job.JobConfigurationException;

/**
 * Test the functions in {@linkplain com.prelert.job.process.ProcessCtl} for
 * writing the analysis configurations as either config files or comamnd line
 * arguments to autodetect 
 */
public class FieldConfigTest 
{
	/**
	 * Test writing a single detector as command line options.
	 * {@linkplain Detector#verify()} is called on each config as a  
	 * sanity check
	 * @throws JobConfigurationException 
	 */
	@Test	
	public void testSingleDetectorToCommandLineArgs() throws JobConfigurationException
	{
		// simple case of a by b
		Detector d = new Detector();
		d.setFieldName("Integer_Value");
		d.setByFieldName("ts_hash");
		d.verify();
		
		List<String> args = ProcessCtrl.detectorConfigToCommandLinArgs(d);
		assertEquals(args.size(), 3);
		assertEquals(args.get(0), d.getFieldName());
		assertEquals(args.get(1), "by");
		assertEquals(args.get(2), d.getByFieldName());
		
		// no function for by field should default to the count function
		d = new Detector();
		d.setFunction("rare");
		d.setByFieldName("ts_hash");
		d.verify();
		
		args = ProcessCtrl.detectorConfigToCommandLinArgs(d);
		assertEquals(args.size(), 3);
		assertEquals(args.get(0), "rare");
		assertEquals(args.get(1), "by");
		assertEquals(args.get(2), d.getByFieldName());
				
		// function and field
		d = new Detector();
		d.setFunction("max");
		d.setFieldName("responseTime");
		d.verify();
		
		args = ProcessCtrl.detectorConfigToCommandLinArgs(d);
		assertEquals(args.size(), 1);
		assertEquals(args.get(0), "max(responseTime)");
		
		
		// function and field and by field
		d = new Detector();
		d.setFunction("max");
		d.setFieldName("responseTime");
		d.setByFieldName("airline");
		d.verify();
		
		args = ProcessCtrl.detectorConfigToCommandLinArgs(d);
		assertEquals(args.size(), 3);
		assertEquals(args.get(0), "max(responseTime)");		
		assertEquals(args.get(1), "by");
		assertEquals(args.get(2), d.getByFieldName());
		
		
		// function and field over field
		d = new Detector();
		d.setFunction("min");
		d.setFieldName("responseTime");
		d.setOverFieldName("region");
		d.verify();
		
		args = ProcessCtrl.detectorConfigToCommandLinArgs(d);
		assertEquals(args.size(), 3);
		assertEquals(args.get(0), "min(responseTime)");		
		assertEquals(args.get(1), "over");
		assertEquals(args.get(2), d.getOverFieldName());
		
		// function and field, by field and over field
		d = new Detector();
		d.setFunction("max");
		d.setFieldName("responseTime");
		d.setByFieldName("airline");
		d.setOverFieldName("region");
		d.verify();
		
		args = ProcessCtrl.detectorConfigToCommandLinArgs(d);
		assertEquals(args.size(), 5);
		assertEquals(args.get(0), "max(responseTime)");		
		assertEquals(args.get(1), "by");
		assertEquals(args.get(2), d.getByFieldName());		
		assertEquals(args.get(3), "over");
		assertEquals(args.get(4), d.getOverFieldName());		
		
		// function and field, by field and over field and partition
		d = new Detector();
		d.setFunction("max");
		d.setFieldName("responseTime");
		d.setByFieldName("airline");
		d.setOverFieldName("region");
		d.setPartitionFieldName("airport");
		d.verify();
		
		// function and field, by field and over field		
		args = ProcessCtrl.detectorConfigToCommandLinArgs(d);
		assertEquals(args.size(), 6);
		assertEquals(args.get(0), "max(responseTime)");		
		assertEquals(args.get(1), ProcessCtrl.BY_ARG);
		assertEquals(args.get(2), d.getByFieldName());		
		assertEquals(args.get(3), ProcessCtrl.OVER_ARG);
		assertEquals(args.get(4), d.getOverFieldName());
		assertEquals(args.get(5), ProcessCtrl.PARTITION_FIELD_ARG + d.getPartitionFieldName());
		
		
		// function and field, by field and  partition
		d = new Detector();
		d.setFunction("min");
		d.setFieldName("responseTime");
		d.setByFieldName("airline");
		d.setPartitionFieldName("airport");
		d.verify();
		
		// function and field, by field and over field		
		args = ProcessCtrl.detectorConfigToCommandLinArgs(d);
		assertEquals(args.size(), 4);
		assertEquals(args.get(0), "min(responseTime)");		
		assertEquals(args.get(1), ProcessCtrl.BY_ARG);
		assertEquals(args.get(2), d.getByFieldName());		
		assertEquals(args.get(3), ProcessCtrl.PARTITION_FIELD_ARG + d.getPartitionFieldName());
	}
	
	
	@Test
	public void testSingleDetectorToConfFile()
	throws IOException
	{
		List<Detector> detectors = new ArrayList<>();
		
		Detector d = new Detector();
		d.setFieldName("Integer_Value");
		d.setByFieldName("ts_hash");
		detectors.add(d);
		Detector d2 = new Detector();
		d2.setFunction("count");
		d2.setByFieldName("ipaddress");
		detectors.add(d2);
		Detector d3 = new Detector();
		d3.setFunction("max");
		d3.setFieldName("Integer_Value");
		d3.setOverFieldName("ts_hash");
		detectors.add(d3);
		Detector d4 = new Detector();
		d4.setFunction("rare");
		d4.setFieldName("ipaddress");
		d4.setPartitionFieldName("host");
		detectors.add(d4);
		
		AnalysisConfig config = new AnalysisConfig();
		config.setDetectors(detectors);
		
		ByteArrayOutputStream ba = new ByteArrayOutputStream();
		try (OutputStreamWriter osw = new OutputStreamWriter(ba, "UTF-8"))
		{
			BasicConfigurator.configure();
			Logger logger = Logger.getLogger(FieldConfigTest.class);
			
			ProcessCtrl.writeFieldConfig(config, osw, logger);
		}
		
		// read the ini file - all the settings are in the global section
		StringReader reader = new StringReader(ba.toString("UTF-8"));
		
		Config iniConfig = new Config();
		iniConfig.setLineSeparator(new String(new char [] {ProcessCtrl.NEW_LINE}));
		iniConfig.setGlobalSection(true);
		
		Ini fieldConfig = new Ini();
		fieldConfig.setConfig(iniConfig);
		fieldConfig.load(reader);
		
		Section section = fieldConfig.get(iniConfig.getGlobalSectionName());
		
		Assert.assertEquals(detectors.size(), section.size());
		
		String value = fieldConfig.get(iniConfig.getGlobalSectionName(), "Integer_Value-ts_hash.by");
		Assert.assertEquals("ts_hash", value);
		value = fieldConfig.get(iniConfig.getGlobalSectionName(), "count-ipaddress.by");
		Assert.assertEquals("ipaddress", value);
		value = fieldConfig.get(iniConfig.getGlobalSectionName(), "max(Integer_Value)-ts_hash.over");
		Assert.assertEquals("ts_hash", value);
		value = fieldConfig.get(iniConfig.getGlobalSectionName(), "rare(ipaddress)-host.partition");
		Assert.assertEquals("host", value);
	}
}
