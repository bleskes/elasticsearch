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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.prelert.rs.data.ErrorCode;


/**
 * Defines the fields to be used in the analysis. 
 * <code>fieldname</code> must be set and only one of <code>byFieldName</code> 
 * and <code>overFieldName</code> should be set.
 */
@JsonInclude(Include.NON_NULL)
public class Detector
{
	static final public String FUNCTION = "function";
	static final public String FIELD_NAME = "fieldName";
	static final public String BY_FIELD_NAME = "byFieldName";
	static final public String OVER_FIELD_NAME = "overFieldName";
	static final public String PARTITION_FIELD_NAME = "partitionFieldName";
	static final public String USE_NULL = "useNull";
	
	
	static final public String COUNT = "count";
	static final public String HIGH_COUNT = "high_count";
	static final public String LOW_COUNT = "low_count";
	static final public String NON_ZERO_COUNT = "non_zero_count";
	static final public String NZC = "nzc";
	static final public String DISTINCT_COUNT = "distinct_count";
	static final public String DC = "dc";
	static final public String RARE = "rare";
	static final public String FREQ_RARE = "freq_rare";
	static final public String METRIC = "metric";
	static final public String MEAN = "mean";
	static final public String AVG = "avg";
	static final public String MIN = "min";
	static final public String MAX = "max";
	static final public String SUM = "sum";
	
	/**
	 * The set of valid function names.
	 */
	static public final Set<String> ANALYSIS_FUNCTIONS = 
			new HashSet<String>(Arrays.<String>asList(new String [] {
				COUNT, 
				HIGH_COUNT, 
				LOW_COUNT,
				NON_ZERO_COUNT, NZC,
				DISTINCT_COUNT, DC,
				RARE,
				FREQ_RARE,
				METRIC,
				MEAN, AVG,
				MIN, 
				MAX,
				SUM
			}));
	
	/**
	 * The set of functions that do not require a field, by field or over field
	 */
	static public final Set<String> COUNT_WITHOUT_FIELD_FUNCTIONS = 
			new HashSet<String>(Arrays.<String>asList(new String [] {
				COUNT,
				HIGH_COUNT, 
				LOW_COUNT,
				NON_ZERO_COUNT, NZC
			}));
	
	/**
	 * The set of functions that require a fieldname
	 */
	static public final Set<String> FIELD_NAME_FUNCTIONS = 
			new HashSet<String>(Arrays.<String>asList(new String [] {
				DISTINCT_COUNT, DC,
				METRIC,
				MEAN, AVG,
				MIN, 
				MAX,
				SUM
			}));
	
	/**
	 * The set of functions that require a by fieldname
	 */
	static public final Set<String> BY_FIELD_NAME_FUNCTIONS = 
			new HashSet<String>(Arrays.<String>asList(new String [] {
				RARE,
				FREQ_RARE
			}));
	
	/**
	 * The set of functions that require a over fieldname
	 */
	static public final Set<String> OVER_FIELD_NAME_FUNCTIONS = 
			new HashSet<String>(Arrays.<String>asList(new String [] {
				DISTINCT_COUNT, DC,
				FREQ_RARE
			}));
	
	
	/**
	 * field names cannot contain any of these characters
	 * 	[, ], (, ), =, ", \, - 
	 */
	static private String PROHIBITED = "[, ], (, ), =, \", \\, -";
	static final private Character [] PROHIBITED_FIELDNAME_CHARACTERS = 
		{'[', ']', '(', ')', '=', '"', '\\', '-'};	
			
	
	private String m_Function;
	private String m_FieldName;
	private String m_ByFieldName;
	private String m_OverFieldName;		
	private String m_PartitionFieldName;
	private Boolean m_UseNull;		
		
	public Detector()
	{
		
	}
	
	/**
	 * Populate the detector from the String -> object map.
	 * 
	 * @param detectorMap
	 */
	public Detector(Map<String, Object> detectorMap)
	{
		if (detectorMap.containsKey(FUNCTION))
		{
			Object field = detectorMap.get(FUNCTION);
			if (field != null)
			{
				this.setFunction(field.toString());
			}
		}
		if (detectorMap.containsKey(FIELD_NAME))
		{
			Object field = detectorMap.get(FIELD_NAME);
			if (field != null)
			{
				this.setFieldName(field.toString());
			}
		}
		if (detectorMap.containsKey(BY_FIELD_NAME))
		{
			Object field = detectorMap.get(BY_FIELD_NAME);
			if (field != null)
			{
				this.setByFieldName(field.toString());
			}
		}
		if (detectorMap.containsKey(OVER_FIELD_NAME))
		{
			Object field = detectorMap.get(OVER_FIELD_NAME);
			if (field != null)
			{
				this.setOverFieldName(field.toString());
			}
		}				
		if (detectorMap.containsKey(PARTITION_FIELD_NAME))
		{
			Object obj = detectorMap.get(PARTITION_FIELD_NAME);
			if (obj != null)
			{
				m_PartitionFieldName = obj.toString();
			}
		}
		if (detectorMap.containsKey(USE_NULL))
		{
			Object field = detectorMap.get(USE_NULL);
			if (field != null && field instanceof Boolean)
			{
				this.setUseNull((Boolean)field);
			}
		}						
	}
	
	
	
	/**
	 * The analysis function used e.g. count, rare, min etc. There is no 
	 * validation to check this value is one a predefined set 
	 * @return The function or <code>null</code> if not set
	 */
	public String getFunction() 
	{
		return m_Function;
	}
	
	public void setFunction(String m_Function) 
	{
		this.m_Function = m_Function;
	}
	
	/**
	 * The Analysis field
	 * @return The field to analyse
	 */
	public String getFieldName() 
	{
		return m_FieldName;
	}
	
	public void setFieldName(String m_FieldName) 
	{
		this.m_FieldName = m_FieldName;
	}
	
	/**
	 * The 'by' field or <code>null</code> if not set. 
	 * @return The 'by' field
	 */
	public String getByFieldName() 
	{
		return m_ByFieldName;
	}
	
	public void setByFieldName(String m_ByFieldName) 
	{
		this.m_ByFieldName = m_ByFieldName;
	}
	
	/**
	 * The 'over' field or <code>null</code> if not set. 
	 * @return The 'over' field
	 */
	public String getOverFieldName() 
	{
		return m_OverFieldName;
	}
	
	public void setOverFieldName(String m_OverFieldName) 
	{
		this.m_OverFieldName = m_OverFieldName;
	}	
	
	/**
	 * Segments the analysis along another field to have completely 
	 * independent baselines for each instance of partitionfield
	 *
	 * @return The Partition Field
	 */
	public String getPartitionFieldName() 
	{
		return m_PartitionFieldName;
	}
	
	public void setPartitionFieldName(String partitionFieldName) 
	{
		this.m_PartitionFieldName = partitionFieldName;
	}
	
	
	/**
	 * Where there isn't a value for the 'by' or 'over' field should a new
	 * series be used as the 'null' series. 
	 * @return true if the 'null' series should be created
	 */
	public Boolean isUseNull() 
	{
		return m_UseNull;
	}
	
	public void setUseNull(Boolean useNull) 
	{
		this.m_UseNull = useNull;
	}
			
	@Override 
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		
		if (other instanceof Detector == false)
		{
			return false;
		}
		
		Detector that = (Detector)other;
				
		return JobDetails.bothNullOrEqual(this.m_Function, that.m_Function) &&
				JobDetails.bothNullOrEqual(this.m_FieldName, that.m_FieldName) &&
				JobDetails.bothNullOrEqual(this.m_ByFieldName, that.m_ByFieldName) &&
				JobDetails.bothNullOrEqual(this.m_OverFieldName, that.m_OverFieldName) &&
				JobDetails.bothNullOrEqual(this.m_PartitionFieldName, that.m_PartitionFieldName) &&
				JobDetails.bothNullOrEqual(this.m_UseNull, that.m_UseNull);					
	}
	
	/**
	 * Checks the configuration is valid
	 * <ol>
	 * <li>One of fieldName, byFieldName, overFieldName or function must be set</li>
	 * <li>Unless the function is 'count' one of fieldName, byFieldName 
	 * or overFieldName must be set</li>
	 * <li>If byFieldName is set function or fieldName must bet set</li>
	 * <li>If overFieldName is set function or fieldName must bet set</li>
	 * <li>function is one of the strings in the set {@link #ANALYSIS_FUNCTIONS}</li>
	 * <li>If function is not set but the fieldname happens to be the same 
	 * as one of the function names (e.g.a field called 'count')
	 * set function to 'metric'</li>
	 * <li>Check the metric/by/over fields are set as required by the different
	 * functions</li> 
	 * <li>Check the metric/by/over fields that cannot be set with certain 
	 * functions are not set</li> 
	 * <li>If the function is NON_ZERO_COUNT or NZC
	 * then overFieldName must not be set</li>
	 * </ol>
	 * 
	 * @return true
	 * @throws JobConfigurationException
	 */
	public boolean verify()
	throws JobConfigurationException
	{	
		boolean emptyField = m_FieldName == null || m_FieldName.isEmpty();
		boolean emptyByField = m_ByFieldName == null || m_ByFieldName.isEmpty();
		boolean emptyOverField = m_OverFieldName == null || m_OverFieldName.isEmpty();
		boolean emptyFunction = m_Function == null || m_Function.isEmpty();
		
		
		if (emptyField && emptyByField && emptyOverField)
		{
			if (emptyFunction)
			{
				throw new JobConfigurationException("One of fieldName, "
						+ "byFieldName, overFieldName or function must be set",
						ErrorCode.INVALID_FIELD_SELECTION);
			}
			
			if (!COUNT_WITHOUT_FIELD_FUNCTIONS.contains(m_Function))
			{
				throw new JobConfigurationException("Unless the function is 'count'"
						+ " one of fieldName, byFieldName or overFieldName must be set",
						ErrorCode.INVALID_FIELD_SELECTION);
			}
		}
		
		if (!emptyFunction && ANALYSIS_FUNCTIONS.contains(m_Function) == false)
		{
			throw new JobConfigurationException("Unknown function '" + m_Function + "'",
					ErrorCode.UNKNOWN_FUNCTION);
		}
		
		// If function is not set but the fieldname happens 
		// to be the same as one of the function names (e.g. 
		// a field called 'count' set function to 'metric'
		if (emptyFunction)
		{
			if (ANALYSIS_FUNCTIONS.contains(m_FieldName))
			{
				m_Function = METRIC;
			}
		}
		
		
		if (!emptyByField)
		{
			if (emptyField && emptyFunction)
			{
				throw new JobConfigurationException(
						"byFieldName must be used in "
						+ "conjunction with fieldName or function",
						ErrorCode.INVALID_FIELD_SELECTION);
			}
		}
		
		if (!emptyOverField)
		{
			if (emptyField && emptyFunction)
			{
				throw new JobConfigurationException(
						"overFieldName must be used in "
						+ "conjunction with fieldName or function",
						ErrorCode.INVALID_FIELD_SELECTION);
			}
		}
		
		// check functions have required fields
		if (!emptyFunction)
		{
			if (FIELD_NAME_FUNCTIONS.contains(m_Function))
			{
				if (emptyField)
				{
					throw new JobConfigurationException(
							String.format("The fieldName must be set when the "
									+ " '%s' function is used", m_Function),
							ErrorCode.INVALID_FIELD_SELECTION);
				}
			}
			
			if (!emptyField && (FIELD_NAME_FUNCTIONS.contains(m_Function) == false))
			{
				throw new JobConfigurationException(
						String.format("fieldName cannot be used with function '%s'",
								m_Function),
						ErrorCode.INVALID_FIELD_SELECTION);
			}
			
			if (BY_FIELD_NAME_FUNCTIONS.contains(m_Function))
			{
				if (emptyByField)
				{
					throw new JobConfigurationException(
							String.format("The byFieldName must be set when the "
									+ " '%s' function is used", m_Function),
							ErrorCode.INVALID_FIELD_SELECTION);
				}
			}
			
			if (!emptyByField && (DISTINCT_COUNT.equals(m_Function) || DC.equals(m_Function)))
			{
				throw new JobConfigurationException(
						String.format("byFieldName cannot be used with function '%s'",
								m_Function),
						ErrorCode.INVALID_FIELD_SELECTION);
			}
			
			if (OVER_FIELD_NAME_FUNCTIONS.contains(m_Function))
			{
				if (emptyOverField)
				{
					throw new JobConfigurationException(
							String.format("The overFieldName must be set when the "
									+ " '%s' function is used", m_Function),
							ErrorCode.INVALID_FIELD_SELECTION);
				}
			}		
			
			if (!emptyOverField && (NON_ZERO_COUNT.equals(m_Function) || NZC.equals(m_Function)))
			{
				throw new JobConfigurationException(
						String.format("overFieldName cannot be used with function '%s'",
								m_Function),
						ErrorCode.INVALID_FIELD_SELECTION);
			}
			
		}
		
		
		// field names cannot contain certain characters
		String [] fields = {m_FieldName, m_ByFieldName, m_OverFieldName, m_PartitionFieldName};
		for (String field : fields)
		{
			verifyFieldName(field);
		}

		return true;
	}


	/**
	 * Check that the characters used in a field name will not cause problems.
	 * @param field The field name to be validated
	 * @return true
	 * @throws JobConfigurationException
	 */
	public static boolean verifyFieldName(String field)
	throws JobConfigurationException
	{
		if (field != null)
		{
			for (Character ch : PROHIBITED_FIELDNAME_CHARACTERS)
			{
				if (field.indexOf(ch) >= 0)
				{
					throw new JobConfigurationException(
							"Invalid fieldname '" + field + "'. " +
							"Fieldnames including over, by and partition fields cannot " +
							"contain any of these characters: " + PROHIBITED,
							ErrorCode.PROHIBITIED_CHARACTER_IN_FIELD_NAME);
				}
			}
		}
		return true;
	}

}
