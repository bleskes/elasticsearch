/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Inc 2006-2014     *
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
import java.util.Set;

import org.junit.Test;

import com.prelert.job.AnalysisConfig;

/**
 * Test the functions in {@linkplain com.prelert.job.process.ProcessCtl} for
 * writing the analysis configurations as either config files or comamnd line
 * arguments to autodetect 
 */
public class FieldConfigTest 
{
	/**
	 * Test writing a single detector as command line options
	 */
	@Test	
	public void testSingleDetectorToCommandLineArgs()
	{
		ProcessCtrl processCtl = new ProcessCtrl();
			
		// simple case of a by b
		AnalysisConfig.Detector d = new AnalysisConfig.Detector();
		d.setFieldName("Integer_Value");
		d.setByFieldName("ts_hash");
		
		List<String> args = processCtl.detectorConfigToCommandLinArgs(d);
		assertEquals(args.size(), 3);
		assertEquals(args.get(0), d.getFieldName());
		assertEquals(args.get(1), "by");
		assertEquals(args.get(2), d.getByFieldName());
		
		// no field name should default to the count funtion
		d = new AnalysisConfig.Detector();
		d.setByFieldName("ts_hash");
		
		// TODO is 'rare by field' a valid config? 
		
		args = processCtl.detectorConfigToCommandLinArgs(d);
		assertEquals(args.size(), 3);
		assertEquals(args.get(0), "count");
		assertEquals(args.get(1), "by");
		assertEquals(args.get(2), d.getByFieldName());
				
		// function and field
		d = new AnalysisConfig.Detector();
		d.setFunction("max");
		d.setFieldName("responseTime");
		
		args = processCtl.detectorConfigToCommandLinArgs(d);
		assertEquals(args.size(), 1);
		assertEquals(args.get(0), "max(responseTime)");
		
		// function and field and by field
		d = new AnalysisConfig.Detector();
		d.setFunction("max");
		d.setFieldName("responseTime");
		d.setByFieldName("airline");
		
		args = processCtl.detectorConfigToCommandLinArgs(d);
		assertEquals(args.size(), 3);
		assertEquals(args.get(0), "max(responseTime)");		
		assertEquals(args.get(1), "by");
		assertEquals(args.get(2), d.getByFieldName());
		
		// function and field over field
		d = new AnalysisConfig.Detector();
		d.setFunction("min");
		d.setFieldName("responseTime");
		d.setOverFieldName("region");
		
		args = processCtl.detectorConfigToCommandLinArgs(d);
		assertEquals(args.size(), 3);
		assertEquals(args.get(0), "min(responseTime)");		
		assertEquals(args.get(1), "over");
		assertEquals(args.get(2), d.getOverFieldName());
		
		// function and field, by field and over field
		d = new AnalysisConfig.Detector();
		d.setFunction("max");
		d.setFieldName("responseTime");
		d.setByFieldName("airline");
		d.setOverFieldName("region");
		
		args = processCtl.detectorConfigToCommandLinArgs(d);
		assertEquals(args.size(), 5);
		assertEquals(args.get(0), "max(responseTime)");		
		assertEquals(args.get(1), "by");
		assertEquals(args.get(2), d.getByFieldName());		
		assertEquals(args.get(3), "over");
		assertEquals(args.get(4), d.getOverFieldName());				
	}
	
	
	@Test
	public void testSingleDetectorToConfFile()
	throws IOException
	{
		ProcessCtrl processCtl = new ProcessCtrl();
		
		List<AnalysisConfig.Detector> detectors = new ArrayList<>();
		
		
		
		AnalysisConfig.Detector d = new AnalysisConfig.Detector();
		d.setFieldName("Integer_Value");
		d.setByFieldName("ts_hash");
		detectors.add(d);
		AnalysisConfig.Detector d2 = new AnalysisConfig.Detector();
		d2.setFunction("count");
		d2.setByFieldName("ts_hash");
		detectors.add(d2);
		
		

		
		AnalysisConfig config = new AnalysisConfig();
		config.setDetectors(detectors);
		
		ByteArrayOutputStream ba = new ByteArrayOutputStream();
		try (OutputStreamWriter osw = new OutputStreamWriter(ba, "UTF-8"))
		{
			processCtl.writeFieldConfig(config, osw);
		}
		//System.out.print(ba.toString("UTF-8"));

		// TODO Apache commons config separates the ini file section headers 
		// from the key with '.' so you use getProperty(section.key) which
		// doesnt work for us as the keys contain '.'.
		// Try ini4j
		/***
		HierarchicalINIConfiguration iniConfig = new HierarchicalINIConfiguration();
		
		StringReader reader = new StringReader(ba.toString("UTF-8"));
		iniConfig.load(reader);
		
		Set<String> sections = iniConfig.getSections();
		for (String s : sections)
		{
			System.out.println(s);
		}
		
		String by = iniConfig.getProperty("count-ts_hash.by").toString();
		System.out.println(by);
		by = iniConfig.getProperty("Integer_Value-ts_hash.by").toString();
		System.out.println(by);		
		****/
	}
}
