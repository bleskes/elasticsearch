/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2013     *
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

package com.prelert.hadoop.test;


import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Set;


import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.junit.Test;

import com.prelert.hadoop.FieldConfig;
import com.prelert.hadoop.FunctionType;

import static org.junit.Assert.*;

/**
 * Unit test for loading/parsing field config files. 
 */
public class FieldConfigTest 
{
	
	@Test
	public void testLoadIni() throws FileNotFoundException, ConfigurationException
	{
		final String PRELERT_HOME = System.getProperty("PRELERT_SRC_HOME");
		File file = new File(PRELERT_HOME + "/gui/apps/hadoop/test/populationprelertfields.conf");
		
		HierarchicalINIConfiguration ini = new HierarchicalINIConfiguration(file);
		
		Set<String> sections = ini.getSections();
		
		assertTrue(sections.contains("default"));
		assertTrue(sections.contains("netflow"));
		
		SubnodeConfiguration defaultSection = ini.configurationAt("default");
		/*** 
		String s = defaultSection.getRootNode().getName();
		//String s = (String)defaultSection.getProperty("count-_raw.isEnabled");
		
		assertTrue(defaultSection.getString("count-_raw.isEnabled").equals("True"));
		assertTrue(defaultSection.getString("count-_raw.useNull").equals("False"));
		assertTrue(defaultSection.getString("count-_raw.by").equals("prelertcategory"));
		***/
	}
	
	
	@Test
	public void testFieldConfig() throws FileNotFoundException, ConfigurationException
	{
		final String PRELERT_HOME = System.getProperty("PRELERT_SRC_HOME");
		File file = new File(PRELERT_HOME + "/gui/apps/hadoop/test/populationprelertfields.conf");
		/***
		FieldConfig config = FieldConfig.initFromFile(file);		
		List<FieldConfig.FieldOptions> fOptions = config.getFieldsForSourceType("netflow");
		
		/***
		Collections.sort(fOptions);
		
		FieldConfig.FieldOptions fo = fOptions.get(0);
		assertTrue(fo.getConfigKey().equals("count-DPT"));
		assertTrue(fo.getByFieldName().equals("SRC"));
		assertTrue(fo.getFieldName().equals("count"));
		assertTrue(fo.getFunctionType() == FunctionType.IndividualRareCount);
		assertTrue(fo.isUseNull() == false);
		
		fo = fOptions.get(1);
		assertTrue(fo.getConfigKey().equals("population_count(DPT)"));
		assertTrue(fo.getByFieldName().equals("SRC"));
		assertTrue(fo.getFieldName().equals("DPT"));
		assertTrue(fo.getFunctionType() == FunctionType.PopulationCount);
		assertTrue(fo.isUseNull() == true);
		***/
	}
}
