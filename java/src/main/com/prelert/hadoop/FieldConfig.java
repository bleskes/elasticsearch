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

package com.prelert.hadoop;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.log4j.Logger;


/**
 * This is the equivalent of the C++ CFieldConfig class used to hold
 * field configuration options. 
 *
 */
public class FieldConfig 
{
	public static final Logger s_Logger = Logger.getLogger(FieldConfig.class);
	
	/**
	 * These constants should match the values in CFieldConfig.
	 */
	public static final String ERROR_FIELDNAME = "ERROR";
	public static final String DEFAULT_STANZA = "default";
	public static final char   SUFFIX_SEPARATOR = '.';
	public static final char   FIELDNAME_SEPARATOR = '-';
	public static final String IS_ENABLED_SUFFIX = ".isEnabled";
	public static final String BY_SUFFIX = ".by";
	public static final String PARTITION_SUFFIX = ".partition";
	public static final String USE_NULL_SUFFIX = ".useNull";
	public static final String BY_TOKEN = "by";
	public static final String COUNT_NAME = "count";

	
	private List<FieldOptions> m_DefaultFields;
	private Map<String, List<FieldOptions>> m_FieldsBySourceType;
	
	
	public FieldConfig()
	{
		m_DefaultFields = new ArrayList<FieldOptions>();
		m_FieldsBySourceType = new HashMap<String, List<FieldOptions>>();
	}
	
	
	public FieldConfig(String fieldname, String byFieldName, 
						boolean useNull)
	{
		this();
		
		FieldOptions fo = new FieldOptions(fieldname, "single",
										byFieldName, useNull);
		
		m_DefaultFields.add(fo);

		// TODO Seen fields?
	}


	/**
	 * Factory method, construct a <code>FieldConfig</code> from a field
	 * config file. Field configs are in a Windows 'ini' style format.
	 * 
	 * @param file The ini config file
	 * @return
	 */
	static public FieldConfig initFromFile(File file)
	{
		FieldConfig config = new FieldConfig();
		try 
		{
			HierarchicalINIConfiguration iniConfig = new HierarchicalINIConfiguration(file);
			
			Set<String> sections = iniConfig.getSections();
			
			for (String sourcetype : sections)
			{
				SubnodeConfiguration section = iniConfig.getSection(sourcetype);
				config.addSection(section);
			}
			
		}
		catch (ConfigurationException e) 
		{
			s_Logger.error("Invalid file cannot parse field config file.", e);
		} 

		return config;
	}
	
	
	/**
	 * Get all the FieldOptions for the given sourcetype.
	 * 
	 * @param sourcetype
	 * @return
	 */
	public List<FieldOptions> getFieldsForSourceType(String sourcetype)
	{
		List<FieldOptions> result = m_FieldsBySourceType.get(sourcetype);
		if (result == null)
		{
			result = Collections.emptyList();
		}
		
		return result;
	}
	
	
	private boolean addSection(SubnodeConfiguration section)
	{
		Set<String> configKeys = new HashSet<String>();
		
		Iterator<String> keyIter = section.getKeys(); 
		// each section can have multiple configs/functions
		while (keyIter.hasNext())
		{
			String key = keyIter.next();
			
			int index = key.indexOf('.');
			
			String configKey = key.substring(0, index);
			configKeys.add(configKey);
		}
		
		for (String key : configKeys)
		{
			// is enabled
			if (section.containsKey(key + IS_ENABLED_SUFFIX))
			{
				boolean isEnabled = section.getBoolean(key + IS_ENABLED_SUFFIX);
				if (isEnabled == false)
				{
					continue;
				}
			}
			
			// use null
			boolean useNull = false;
			if (section.containsKey(key + USE_NULL_SUFFIX))
			{
				useNull = section.getBoolean(key + USE_NULL_SUFFIX);
			}
			
			// by field 
			String byFieldName = "";
			if (section.containsKey(key + BY_SUFFIX))
			{
				byFieldName = section.getString(key + BY_SUFFIX);
			}

			// partition field 
			String partitionFieldName = "";
			if (section.containsKey(key + PARTITION_SUFFIX))
			{
				partitionFieldName = section.getString(key + PARTITION_SUFFIX);
			}
			
			// function name
			String funField = key;
			int index = key.indexOf(FIELDNAME_SEPARATOR);
			if (index > -1)
			{
				funField = key.substring(0, index);
			}
			FieldOptions fo = parseFunctionString(funField);
			
			fo.m_ByFieldName = byFieldName;
			fo.m_PartitionFieldName = partitionFieldName;
			fo.m_ConfigKey = key;
			fo.m_UseNull = useNull;
			
			List<FieldOptions> options = m_FieldsBySourceType.get(section.getSubnodeKey());
			if (options == null)
			{
				options = new ArrayList<FieldOptions>();
				m_FieldsBySourceType.put(section.getSubnodeKey(), options);
			}
			options.add(fo);
			
		}
		
		return true;
	}
	
	
	/**
	 * Extract the function type and optional field name from the field 
	 * string. This function will match:
     * count
     * count()
     * pc(category)
     * dc(category)
     * etc.
	 * Returns a partially populated FieldOption or <code>null</code> if the field
	 * cannot be parsed. Only the function and fieldname will be set in the returned
	 * FieldOptions.
	 * 
	 * @param field the field string (right hand side of the '=')
	 * @param A partially populated FieldOption or <code>null</code> if the field
	 * cannot be parsed.
	 */
	private FieldOptions parseFunctionString(String field)
	{
		Pattern regex = Pattern.compile("([^()]+)(?:\\(([^()]*)\\))?");
		Matcher matcher = regex.matcher(field);
		matcher.matches();
		
	    // Overall string "x(y)" => outerToken is "x" and innerToken is "y"
	    // Overall string "x"    => outerToken is "x" and innerToken is empty
	    // Overall string "x()"  => outerToken is "x" and innerToken is empty
		String outerToken = matcher.group(1);
		if (outerToken == null || outerToken.isEmpty())
		{
			s_Logger.error("Cannot parse field = " + field);			
		}
		
		String innerToken = matcher.group(2);
		if (innerToken == null)
		{
			innerToken = "";
		}
		
		
		FunctionType function = FunctionType.fromString(outerToken);
		
		if (function == FunctionType.IndividualRareCount)
		{
			if (!innerToken.isEmpty())
			{
				s_Logger.error("Individual count may not specify a category = " + innerToken);
				return null;
		    }

			FieldOptions fo = new FieldOptions(COUNT_NAME, ""); // empty configKey
			fo.m_Function = function;
			return fo;
		}
		else if (function == FunctionType.None)
		{
			// We expect an individual metric here, but if the original string
			// contained brackets then there's probably been a typo because a metric
			// name should not be followed by brackets
			if (field.contains("("))
			{
				s_Logger.error(outerToken + "() is not a known function");
				return null;
			}

			FieldOptions fo = new FieldOptions(outerToken, ""); // empty configKey 
			fo.m_Function = FunctionType.IndividualMetric;
			return fo;
		}

		// All metric and population functions must have an argument except
		// population count
		if (function != FunctionType.PopulationCount &&
				innerToken.isEmpty())
		{
			s_Logger.error("Function " + outerToken + "() requires an argument");
			return null;
		}

		FieldOptions fo = new FieldOptions(innerToken, "");
		fo.m_Function = function;
		return fo;
	}
	
	
	/**
	 * FieldOptions class contains the config for a single 
	 * model. 
	 */
	public class FieldOptions implements Comparable<FieldOptions>
	{
        private FunctionType m_Function;
        private String m_FieldName;
        private String m_ConfigKey;
        private String m_ByFieldName;
        private String m_PartitionFieldName;
        private boolean m_UseNull;
        
		/**
		 * Construct with no "by" field, deducing the function from the fieldname.
		 * UseNull is set to true.
		 * 
		 * @param fieldName
		 * @param configKey
		 */
        public FieldOptions(String fieldName, String configKey)
		{
			m_Function = (fieldName.equals(COUNT_NAME)) ? FunctionType.IndividualRareCount : FunctionType.IndividualMetric;
			m_FieldName = fieldName;
			m_ConfigKey = configKey;
			m_UseNull = true;
		}


		/**
		 * Deduce the function from the fieldName
		 * 
		 * @param fieldName
		 * @param configKey
		 * @param byFieldName
		 * @param useNull
		 */
		public FieldOptions(String fieldName, String configKey,
					String byFieldName, boolean useNull)
		{
			this(fieldName, configKey);

			m_ByFieldName = byFieldName;
			m_UseNull = useNull;
		}


		/**
		 * Specify everything
		 * 
		 * @param functionType
		 * @param fieldName
		 * @param configKey
		 * @param byFieldName
		 * @param partitionFieldName
		 * @param useNull
		 */
		public FieldOptions(FunctionType functionType,
				String fieldName, String configKey,
				String byFieldName, String partitionFieldName,
				boolean useNull)
	    {
			m_Function = functionType;
			m_FieldName = fieldName;
			m_ConfigKey = configKey;
			m_ByFieldName = byFieldName;
			m_PartitionFieldName = partitionFieldName;
			m_UseNull = useNull;
	    }
		
		
		public boolean isMetric()
		{
			return m_Function.isMetric();
		}
		
		
		public FunctionType getFunctionType()
		{
			return m_Function;
		}


		public String getFieldName()
		{
			return m_FieldName;
		}


		public String getConfigKey()
		{
			return m_ConfigKey;
		}


		public String getByFieldName()
		{
			return m_ByFieldName;
		}


		public String getPartitionFieldName()
		{
			return m_PartitionFieldName;
		}


		public boolean isUseNull()
		{
			return m_UseNull;
		}


		/**
		 * Order field options by the config key.
		 */
		@Override
		public int compareTo(FieldOptions other) 
		{
			if (this == other)
			{
				return 0;
			}
			
			return this.m_ConfigKey.compareTo(other.m_ConfigKey);
		}

	}
}
